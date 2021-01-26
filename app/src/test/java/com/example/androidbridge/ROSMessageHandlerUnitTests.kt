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
    fun publishes_when_attached_with_specified_rate()
    {
        val mTransceiver = JsonTranceiverMock()
        val mSerializer = GsonSerializer()
        val mROSMessageHandler = ROSMessageHandler(mTransceiver, mSerializer )

        val mockSensor = ROSPointSensorMock()
        val expectedMessage = mSerializer.toJson(mockSensor.mReading)
        val rate : Long = 10
        val expectedCount = 10
        val actual = mutableListOf<String>()
        mTransceiver.attachReceiver {
            actual.add(it)
        }
        // Rate 100. messageHandler takes mock message every 100ms.
        mROSMessageHandler.attachSensor(mockSensor, rate)

        Thread.sleep(transmissionDelay * 3)

        println("Expected: $expectedMessage")
        print("Actual: ")

        actual.forEach {
            println(it)
        }

        assert(actual.subList(3, actual.size ).all{ it == expectedMessage } )
        assert(expectedCount == actual.size)

    }

    @Test
    fun messageHandler_test_device_attach()
    {
        val mTransceiver = JsonTranceiverMock()
        val mSerializer = GsonSerializer()
        val mROSMessageHandler = ROSMessageHandler(mTransceiver, mSerializer )

        val mockDevice = ROSPointDeviceMock()

        val expected = 1
        mROSMessageHandler.attachDevice(mockDevice)
        // assert(expected == mROSMessageHandler.mHandledControls.size)
        assert(false)
    }

    @Test
    fun messageHandler_test_subscribe_point_device()
    {
        val mTransceiver = JsonTranceiverMock()
        val mSerializer = GsonSerializer()
        val mROSMessageHandler = ROSMessageHandler(mTransceiver, mSerializer )

        val mockSensor = ROSPointSensorMock()
        val mockDevice = ROSPointDeviceMock()

        val expected : Point = mockSensor.mReading.msg as Point

        mROSMessageHandler.attachDevice(mockDevice)
        mTransceiver.send( mSerializer.toJson(mockSensor.mReading ))

        println("Expected: $expected")
        val actual =  mockDevice.latestData

        println("Actual: $actual" )

        assert(expected == actual)
    }

    @Test
    fun messageHandler_test_subscribe_twist_device()
    {
        val mTransceiver = JsonTranceiverMock()
        val mSerializer = GsonSerializer()
        val mROSMessageHandler = ROSMessageHandler(mTransceiver, mSerializer )

        val mockSensor = ROSTwistSensorMock()
        val mockDevice = ROSTwistDeviceMock()
        val expected = mockSensor.mReading.msg as Twist

        mROSMessageHandler.attachDevice(mockDevice)
        mTransceiver.send( mSerializer.toJson(mockSensor.mReading ))

        println("Expected: $expected")
        println("Actual: " + mockDevice.latestData)

        assert(expected == mockDevice.latestData )
    }

    @Test
    fun messageHandler_test_subscribe_aircraftActions_device()
    {
        val mTransceiver = JsonTranceiverMock()
        val mSerializer = GsonSerializer()
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
    fun messageHandler_test_attach_recv_multi_control_device()
    {
        val mTransceiver = JsonTranceiverMock()
        val mSerializer = GsonSerializer()
        val mROSMessageHandler = ROSMessageHandler(mTransceiver, mSerializer )

        val mockDevice = MultiControlDeviceMock()

        val msg1 = PublishMessage(
            topic = "/biosentry/AircraftFlightActions",
            msg = AircraftFlightActionsInt(1)
        )

        val msg2 = PublishMessage(
            topic = "/geometry_msgs/Twist",
            msg = Twist(
                Vector3( 1.0,2.0,3.0),
                Vector3(1.0,2.0,3.0)
            )
        )

        mROSMessageHandler.attachDevice(mockDevice)
        mTransceiver.send( mSerializer.toJson(msg1))
        mTransceiver.send( mSerializer.toJson(msg2))

        println("Actual1: " + mockDevice.mFActions)
        println("Actual2: " + mockDevice.mTwist)

        // assert(mROSMessageHandler.mControls.size == 2)
        assert(false)
        assert( mockDevice.mFActions != null ) //msg cannot be compared as conversion between types is done.
        assert( mockDevice.mTwist == msg2.msg)
    }

    @Test
    fun messageHandler_test_attach_subscribe_multi_control_device()
    {
        val mTransceiver = JsonTranceiverMock()
        val mSerializer = GsonSerializer()
        val mROSMessageHandler = ROSMessageHandler(mTransceiver, mSerializer )

        val mockDevice = MultiControlDeviceMock()
        val actual = mutableListOf<BridgeMessage>()
        mTransceiver.attachReceiver {
            actual.add(mSerializer.fromJson(it))
        }
        mROSMessageHandler.attachDevice(mockDevice)

        Thread.sleep(500)

        mockDevice.mControls.forEach{
            assert( actual.contains(it.message))
        }
    }

    companion object {
        const val transmissionDelay = ROSMessageHandler.retransmissionRate + 20
    }


}