package com.example.androidbridge

import com.biosentry.androidbridge.communication.ROSBridge
import com.biosentry.androidbridge.communication.ROSMessageHandler
import com.neovisionaries.ws.client.WebSocket
import com.neovisionaries.ws.client.WebSocketFactory
import org.junit.Test

/**
 * Some simple unit tests to assert that the WebSocket client is functioning.
 */
class ROSMessageHandlerUnitTests {

    //https://www.websocket.org/echo.html
    private val mURI : String = "ws://demos.kaazing.com/echo"
    private val mROSBridge = ROSBridge(mURI)
    private val mROSMessageHandler = ROSMessageHandler(mROSBridge)
    private val mMockSensor = ROSSensorMock()

    @Test
    fun messageHandler_attachSensor()
    {
        mROSMessageHandler.attachSensor(mMockSensor, 1000)
    }




}