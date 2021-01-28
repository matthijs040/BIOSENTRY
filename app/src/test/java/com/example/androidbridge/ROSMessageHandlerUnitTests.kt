package com.example.androidbridge

import com.biosentry.androidbridge.communication.*
import com.biosentry.androidbridge.serialization.GsonSerializer
import com.example.androidbridge.mocks.*
import org.junit.Test

/**
 * Some simple unit tests to assert that the WebSocket client is functioning.
 */
class ROSMessageHandlerUnitTests {

    fun hasKeyValuePair(data : String, key : String, value : String) : Boolean
    {
        return data.contains("\"$key\":\"$value\"")
    }

    @Test
    fun advertises_when_sensor_is_attached()
    {
        val mTransceiver = JsonTranceiverMock()
        val mSerializer = GsonSerializer()
        val mROSMessageHandler = ROSMessageHandler(mTransceiver, mSerializer )

        // Create a copy of the expected serialized message.
        val mockSensor = ROSPointSensorMock()
        val expected = mockSensor.mAdvertiseMessage

        //Rate 0 mock does not publish
        mROSMessageHandler.attachSensor(mockSensor, 0)

        //Sleep to let the message handler run the advertise
        Thread.sleep(transmissionDelay)

        // Get the result from the transceiver mock
        val actual = mTransceiver.recv()
        println("Actual: $actual")

        // Expect that at least the expected data is contained
        assert( hasKeyValuePair(actual, "op",   expected.op ) )
        assert( hasKeyValuePair(actual, "topic", expected.topic ) )
        assert( hasKeyValuePair(actual, "type", expected.type ) )

        // Note that id is not compared since the message-handler changes it to use for routing.
    }

    @Test
    fun keeps_advertising_if_not_acknowledged()
    {
        val mTransceiver = JsonTranceiverMock()
        val mSerializer = GsonSerializer()
        val mROSMessageHandler = ROSMessageHandler(mTransceiver, mSerializer )

        val mockSensor = ROSPointSensorMock()
        mROSMessageHandler.attachSensor(mockSensor, 0)


        val expectedMessage = mSerializer.toJson(mockSensor.mAdvertiseMessage)
        val expectedCount = 3
        val actual = mutableListOf<String>()

        // Make a lambda function receive the transmissions from the message-handler.
        // Which then pushes it back into a list.
        mTransceiver.attachReceiver {
            actual.add(it)
        }

        // Wait while messages are sent through a different thread.
        Thread.sleep(transmissionDelay * expectedCount)

        actual.forEach {
            println(it)
        }

        // Assert that the messages were sent and repeated.
        assert(actual.all{ it == expectedMessage } )
        assert(expectedCount == actual.size)
    }

    @Test
    fun stops_advertising_when_acknowledged()
    {
        val mTransceiver = JsonTranceiverMock()
        val mSerializer = GsonSerializer()
        val mROSMessageHandler = ROSMessageHandler(mTransceiver, mSerializer )

        val mockSensor = ROSPointSensorMock()
        mROSMessageHandler.attachSensor(mockSensor, 0)

        val expectedMessage = mSerializer.toJson(mockSensor.mAdvertiseMessage)
        val expectedResponse = mSerializer.toJson(StatusMessage(level="info", id = mockSensor.mAdvertiseMessage.id, msg="advertise_ack"))
        val expectedCount = 1
        val actual = mutableListOf<String>()

        mTransceiver.attachReceiver {
            actual.add(it)

            val recv = mSerializer.fromJson(it)
            if(recv is AdvertiseMessage)
                mTransceiver.send( expectedResponse )
        }

        // Give additional time in which it can be tested if additional messaging stops.
        Thread.sleep(transmissionDelay * expectedCount * 3)

        actual.forEach {
            println(it)
        }

        // Assert that only the advertisement and response are in the list.
        assert(actual.elementAt(0) == expectedMessage )
        assert(actual.elementAt(1) == expectedResponse )
        assert(actual.size == 2)
    }


    @Test
    fun publishes_when_attached_with_specified_rate()
    {
        val mTransceiver = JsonTranceiverMock()
        val mSerializer = GsonSerializer()
        val mROSMessageHandler = ROSMessageHandler(mTransceiver, mSerializer )

        val mockSensor = ROSPointSensorMock()
        val expectedMessage = mSerializer.toJson(mockSensor.mReading)
        val rate : Long = 50
        val actual = mutableListOf<String>()

        mTransceiver.attachReceiver {
            actual.add(it)

            val recv = mSerializer.fromJson(it)
            if(recv is AdvertiseMessage)
                mTransceiver.send( mSerializer.toJson(StatusMessage(id=recv.id, level = "info",msg = "advertise_ack") ) )
        }
        // Rate 100. messageHandler takes mock message every 100ms.
        mROSMessageHandler.attachSensor(mockSensor, rate)

        Thread.sleep(transmissionDelay * 3)

        println("Expected: $expectedMessage")
        print("Actual: ")

        actual.forEach {
            println(it)
        }


        assert(actual.subList(2, actual.size ).all{ it == expectedMessage } )
        //assert(expectedCount == actual.size)

    }

    @Test
    fun device_subscribes_when_attached()
    {
        val mTransceiver = JsonTranceiverMock()
        val mSerializer = GsonSerializer()
        val mROSMessageHandler = ROSMessageHandler(mTransceiver, mSerializer )

        val mockDevice = ROSPointDeviceMock()

        val expected = mutableListOf<SubscribeMessage>()
        mockDevice.mControls.all { expected.add(it.message) }

        val actual = mutableListOf<BridgeMessage>()
        mTransceiver.attachReceiver {
            actual.add(mSerializer.fromJson(it))
        }


        mROSMessageHandler.attachDevice(mockDevice)

        Thread.sleep(transmissionDelay)

        println("expected: $expected")
        println("actual: $actual")

        expected.forEach {
            assert(actual.contains(it))
        }
    }

    @Test
    fun device_keeps_subscribing_if_not_acknowledged() {
        val mTransceiver = JsonTranceiverMock()
        val mSerializer = GsonSerializer()
        val mROSMessageHandler = ROSMessageHandler(mTransceiver, mSerializer)

        val mockDevice = ROSPointDeviceMock()

        val expected = mutableListOf<SubscribeMessage>()
        mockDevice.mControls.all { expected.add(it.message) }

        val actual = mutableListOf<SubscribeMessage>()
        mTransceiver.attachReceiver {
            val recv = mSerializer.fromJson(it)
            if(recv is SubscribeMessage)
                actual.add( recv)
        }

        mROSMessageHandler.attachDevice(mockDevice)

        val retransmissions = 3
        val initialTransmission = 1
        Thread.sleep(transmissionDelay * retransmissions)

        println("expected: ${expected.count()}")
        println("actual: ${actual.count()}")

        assert(actual.count() == expected.count() * ( retransmissions + initialTransmission ) )
    }

    @Test
    fun device_receives_data_it_subscribed_for()
    {
        val mSerializer = GsonSerializer()
        val mTransceiver = JsonTranceiverMock()
        mTransceiver.attachReceiver {
            val recv = mSerializer.fromJson(it)
            if(recv is SubscribeMessage)
            {
                mTransceiver.send( mSerializer.toJson(StatusMessage(id=recv.id,level = "info",msg = "subscribe_ack") ) )
            }
        }


        val mROSMessageHandler = ROSMessageHandler(mTransceiver, mSerializer )

        val mockDevice = ROSPointDeviceMock()
        val expected = Point(12.0,13.0,14.0)

        mROSMessageHandler.attachDevice(mockDevice)
        assert(mockDevice.mControls.count() == 1)
        mTransceiver.send( mSerializer.toJson(PublishMessage(topic = mockDevice.mControls.first().message.topic ,msg = expected)))

        Thread.sleep(transmissionDelay)

        println("Expected: $expected")
        val actual =  mockDevice.latestData

        println("Actual: $actual" )

        assert(expected == actual)
    }

    @Test
    fun device_receives_data_from_other_sensor()
    {
        val mSerializer = GsonSerializer()
        val mTransceiver = JsonTranceiverMock()

        mTransceiver.attachReceiver { val input = mSerializer.fromJson(it)
            if(input is SubscribeMessage)
                mTransceiver.send( mSerializer.toJson(StatusMessage(id=input.id,level = "info",msg = "subscribe_ack") ) )
            else if(input is AdvertiseMessage)
                mTransceiver.send(mSerializer.toJson(StatusMessage(id=input.id,level = "info",msg = "advertise_ack") ))
        }

        val mROSMessageHandler = ROSMessageHandler(mTransceiver, mSerializer )

        val mockSensor = ROSTwistSensorMock()
        val mockDevice = ROSTwistDeviceMock()
        val expected = mockSensor.mReading.msg as Twist

        mROSMessageHandler.attachDevice(mockDevice)
        mTransceiver.send( mSerializer.toJson(mockSensor.mReading ))

        println("Expected: $expected")
        println("Actual: " + mockDevice.latestData)

        assert(expected == mockDevice.latestData )
        // No thread sleep needed???
        // Should add a sleep if this unit test fails. Sensitive to timing due to Timer in message handler.
    }

    @Test
    fun messageHandler_test_subscribe_aircraftActions_device()
    {
        val mSerializer = GsonSerializer()
        val mTransceiver = JsonTranceiverMock()

        mTransceiver.attachReceiver { val input = mSerializer.fromJson(it)
            if(input is SubscribeMessage)
                mTransceiver.send( mSerializer.toJson(StatusMessage(id=input.id,level = "info",msg = "subscribe_ack") ) )
            else if(input is AdvertiseMessage)
                mTransceiver.send(mSerializer.toJson(StatusMessage(id=input.id,level = "info",msg = "advertise_ack") ))
        }

        val mROSMessageHandler = ROSMessageHandler(mTransceiver, mSerializer )

        val mockDevice = ROSAircraftActionsDeviceMock()
        val msg = PublishMessage(
            topic = "/biosentry/AircraftFlightActions",
            msg = AircraftFlightActionsInt(1)
        )
        val expected = FlightActions.TurnMotorsOff

        mROSMessageHandler.attachDevice(mockDevice)
        mTransceiver.send( mSerializer.toJson(msg))

        println("Expected: $expected")
        println("Actual: " + mockDevice.latestData?.flightActions)

        assert( expected == mockDevice.latestData?.flightActions)

    }

    @Test
    fun a_multi_control_device_advertises_both_controls_when_attached()
    {
        val mSerializer = GsonSerializer()
        val mTransceiver = JsonTranceiverMock()

        mTransceiver.attachReceiver { val input = mSerializer.fromJson(it)
            if(input is SubscribeMessage)
                mTransceiver.send( mSerializer.toJson(StatusMessage(id=input.id,level = "info",msg = "subscribe_ack") ) )
            else if(input is AdvertiseMessage)
                mTransceiver.send(mSerializer.toJson(StatusMessage(id=input.id,level = "info",msg = "advertise_ack") ))
        }

        val mROSMessageHandler = ROSMessageHandler(mTransceiver, mSerializer )

        val mockDevice = MultiControlDeviceMock()
        val actual = mutableListOf<BridgeMessage>()
        mTransceiver.attachReceiver {
            actual.add(mSerializer.fromJson(it))
        }
        mROSMessageHandler.attachDevice(mockDevice)

        mockDevice.mControls.forEach{
            assert( actual.contains(it.message))
        }
    }

    @Test
    fun device_with_multiple_controls_can_receive_data()
    {
        val mSerializer = GsonSerializer()
        val mTransceiver = JsonTranceiverMock()

        mTransceiver.attachReceiver { val input = mSerializer.fromJson(it)
            if(input is SubscribeMessage)
                mTransceiver.send( mSerializer.toJson(StatusMessage(id=input.id,level = "info",msg = "subscribe_ack") ) )
            else if(input is AdvertiseMessage)
                mTransceiver.send(mSerializer.toJson(StatusMessage(id=input.id,level = "info",msg = "advertise_ack") ))
        }

        val mROSMessageHandler = ROSMessageHandler(mTransceiver, mSerializer )


        val mockDevice = MultiControlDeviceMock()
        mROSMessageHandler.attachDevice(mockDevice)

        val msg1 = PublishMessage(
            topic = "/biosentry/AircraftFlightActions",
            msg = AircraftFlightActionsInt(2)   // Is non-default. default is somewhere around 8.
        )

        val msg2 = PublishMessage(
            topic = "/geometry_msgs/Twist",
            msg = Twist(
                Vector3( 1.0,2.0,3.0), // Is non-default. default is all zeros
                Vector3(1.0,2.0,3.0)
            )
        )

        mTransceiver.send( mSerializer.toJson(msg1))
        mTransceiver.send( mSerializer.toJson(msg2))

        println("expected1: ${msg1.msg}")
        println("expected2: ${msg2.msg}")

        println("Actual1: ${mockDevice.mFActions}")
        println("Actual2: ${mockDevice.mTwist}")

        assert( mockDevice.mFActions.flightActions == FlightActions.SetUrgentStopModeEnabled ) // This is int:2 parsed to kotlin enum.
        assert( mockDevice.mTwist == msg2.msg)
    }



    companion object {
        const val transmissionDelay = ROSMessageHandler.retransmissionRate + 20
    }


}