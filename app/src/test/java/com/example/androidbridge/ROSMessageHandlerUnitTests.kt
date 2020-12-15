package com.example.androidbridge

import com.biosentry.androidbridge.communication.*
import com.biosentry.androidbridge.serialization.GsonSerializer
import com.example.androidbridge.mocks.*
import kotlinx.coroutines.runBlocking
import org.junit.Test

/**
 * Some simple unit tests to assert that the WebSocket client is functioning.
 */
class ROSMessageHandlerUnitTests {

    // https://www.websocket.org/echo.html
    // private val mURI : String = "ws://demos.kaazing.com/echo"
    private val mTranceiver = JsonTranceiverMock()
    private val mSerializer = GsonSerializer()
    private val mROSMessageHandler = ROSMessageHandler(mTranceiver, mSerializer )

    @Test
    fun advertises_when_sensor_is_attached()
    {
        // Create a copy of the expected serialized message.
        val mockSensor = ROSPointSensorMock()
        val expected = mSerializer.toJson(mockSensor.mAdvertiseMessage)

        //Rate 0 mock does not publish
        mROSMessageHandler.attachSensor(mockSensor, 0)

        //Sleep to let the message handler run the advertise
        Thread.sleep(10)

        // Print the assertion that the advertise that should be sent is sent.
        println("Expected: $expected")
        println("Actual: " + mTranceiver.recv())

        //
        assert(expected == mTranceiver.recv() )
    }

    @Test
    fun publishes_when_attached_with_specified_rate()
    {
        val mockSensor = ROSPointSensorMock()
        val expectedMessage = mSerializer.toJson(mockSensor.mReading)
        val rate : Long = 10
        val expectedCount = 10
        val actual = mutableListOf<String>()
        mTranceiver.attachReceiver {
            actual.add(it)
        }
        // Rate 100. messageHandler takes mock message every 100ms.
        mROSMessageHandler.attachSensor(mockSensor, rate)

        Thread.sleep(rate * expectedCount + 1000)

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
        val mockDevice = ROSPointDeviceMock()

        val expected = 1
        mROSMessageHandler.attachDevice(mockDevice)
        assert(expected == mROSMessageHandler.mControls.size)
    }

    @Test
    fun messageHandler_test_subscribe_point_device()
    {
        val mockSensor = ROSPointSensorMock()
        val mockDevice = ROSPointDeviceMock()

        val expected : Point = mockSensor.mReading.msg as Point

        mROSMessageHandler.attachDevice(mockDevice)
        mTranceiver.send( mSerializer.toJson(mockSensor.mReading ))

        println("Expected: $expected")
        println("Actual: " + mockDevice.latestData)

        assert(expected == mockDevice.latestData )
    }

    @Test
    fun messageHandler_test_subscribe_twist_device()
    {
        val mockSensor = ROSTwistSensorMock()
        val mockDevice = ROSTwistDeviceMock()
        val expected = mockSensor.mReading.msg as Twist

        mROSMessageHandler.attachDevice(mockDevice)
        mTranceiver.send( mSerializer.toJson(mockSensor.mReading ))

        println("Expected: $expected")
        println("Actual: " + mockDevice.latestData)

        assert(expected == mockDevice.latestData )
    }

    @Test
    fun messageHandler_test_subscribe_aircraftActions_device()
    {
        val mockDevice = ROSAircraftActionsDeviceMock()
        val msg = PublishMessage(
            topic = "/biosentry/AircraftFlightActions",
            msg = AircraftFlightActionsInt(1)
        )
        val expected = FlightActions.TurnMotorsOff

        mROSMessageHandler.attachDevice(mockDevice)
        mTranceiver.send( mSerializer.toJson(msg))

        println("Expected: $expected")
        println("Actual: " + mockDevice.latestData?.flightActions)

        assert( expected == mockDevice.latestData?.flightActions)

    }

    @Test
    fun messageHandler_test_attach_recv_multi_control_device()
    {
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
        mTranceiver.send( mSerializer.toJson(msg1))
        mTranceiver.send( mSerializer.toJson(msg2))

        println("Actual1: " + mockDevice.mFActions)
        println("Actual2: " + mockDevice.mTwist)

        assert(mROSMessageHandler.mControls.size == 2)

        assert( mockDevice.mFActions != null ) //msg cannot be compared as conversion between types is done.
        assert( mockDevice.mTwist == msg2.msg)
    }

    @Test
    fun messageHandler_test_attach_subscribe_multi_control_device()
    {
        val mockDevice = MultiControlDeviceMock()
        val actual = mutableListOf<BridgeMessage>()
        mTranceiver.attachReceiver {
            actual.add(mSerializer.fromJson(it))
        }
        mROSMessageHandler.attachDevice(mockDevice)

        Thread.sleep(500)

        mockDevice.mControls.forEach{
            assert( actual.contains(it.message))
        }
    }






}