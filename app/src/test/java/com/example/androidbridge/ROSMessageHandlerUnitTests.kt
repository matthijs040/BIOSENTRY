package com.example.androidbridge

import com.biosentry.androidbridge.communication.WebSocketClient
import com.biosentry.androidbridge.communication.ROSMessageHandler
import com.biosentry.androidbridge.serialization.GsonSerializer
import com.google.gson.Gson
import org.junit.Test

/**
 * Some simple unit tests to assert that the WebSocket client is functioning.
 */
class ROSMessageHandlerUnitTests {

    //https://www.websocket.org/echo.html
    private val mURI : String = "ws://demos.kaazing.com/echo"
    private val mROSBridge = WebSocketClient(mURI)
    private val mROSMessageHandler = ROSMessageHandler(mROSBridge, GsonSerializer())
    private val mMockSensor = ROSSensorMock()

    @Test
    fun messageHandler_attachSensor()
    {
        mROSMessageHandler.attachSensor(mMockSensor, 1000)
    }




}