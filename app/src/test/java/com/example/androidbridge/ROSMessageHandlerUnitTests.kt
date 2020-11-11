package com.example.androidbridge

import com.biosentry.androidbridge.communication.Point
import com.biosentry.androidbridge.communication.ROSMessageHandler
import com.biosentry.androidbridge.communication.Twist
import com.biosentry.androidbridge.serialization.GsonSerializer
import com.example.androidbridge.mocks.JsonTranceiverMock
import com.example.androidbridge.mocks.ROSPointDeviceMock
import com.example.androidbridge.mocks.ROSPointSensorMock
import com.example.androidbridge.mocks.ROSTwistSensorMock
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
        mROSMessageHandler.attachSensor(mockSensor, 10)
        mROSMessageHandler.attachDevice(mockDevice)

        while ( mockDevice.latestData == null )
        {
            Thread.sleep(100)
        }

        println("Expected: $expected")
        println("Actual: " + mockDevice.latestData)

        assert(expected == mockDevice.latestData )
    }

    @Test
    fun messageHandler_test_subscribe_twist_device()
    {
        val mockSensor = ROSTwistSensorMock()
        val expected = mockSensor.mReading.msg as Twist

        mROSMessageHandler.attachSensor(mockSensor, 10)
    }






}