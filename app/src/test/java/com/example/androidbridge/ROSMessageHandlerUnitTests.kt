package com.example.androidbridge

import com.biosentry.androidbridge.communication.*
import com.biosentry.androidbridge.serialization.GsonSerializer
import com.example.androidbridge.mocks.*
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
    fun messageHandler_test_advertise_on_attach()
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
    fun messageHandler_test_publish_after_attach()
    {
        val mockSensor = ROSPointSensorMock()
        val expected = mSerializer.toJson(mockSensor.mReading)

        // Rate 100. messageHandler takes mock message every 100ms.
        mROSMessageHandler.attachSensor(mockSensor, 10)

        Thread.sleep(650) // Sleep until after advertise re-broadcast and publish startup delay.

        println("Expected: $expected")
        println("Actual: " + mTranceiver.recv())

        assert(expected == mTranceiver.recv() )
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






}