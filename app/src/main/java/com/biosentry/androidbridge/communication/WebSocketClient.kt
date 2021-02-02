package com.biosentry.androidbridge.communication

import android.util.Log
import com.neovisionaries.ws.client.*

class WebSocketClient(uri: String) : IJSONTranceiver {
    // Class for conversion between Data classes and JSON formatted strings.

    private val mWebSocket : WebSocket = WebSocketFactory().createSocket(uri)

    var mErrorHandler  : ( (String) -> Unit )?      = null
    override val mStateHandlers: MutableList<(STATE) -> Unit> = mutableListOf()
    override val mReceivers: MutableList<(String) -> Unit> = mutableListOf()


    private var mLastMessage : String = ""

    private val mWebSocketListener : WebSocketAdapter = object : WebSocketAdapter() {
        override fun onFrame(websocket: WebSocket?, frame: WebSocketFrame?) {
            super.onFrame(websocket, frame)

            frame?.let {
                Log.i("WebSocket", "textFrame: " + it.payloadText)
                invokeReceivers(it.payloadText)
            }
        }

        override fun onFrameUnsent(websocket: WebSocket?, frame: WebSocketFrame?) {
            Log.e("WebSocket", "Unsent: " + frame?.payloadText)
            super.onFrameUnsent(websocket, frame)
        }

        override fun onBinaryMessage(websocket: WebSocket?, binary: ByteArray?) {
            Log.i("WebSocket", "BinFrame: " + binary.toString())
            super.onBinaryMessage(websocket, binary)
        }

        override fun onError(websocket: WebSocket?, cause: WebSocketException?) {
            mErrorHandler?.invoke(cause.toString())
            Log.println(Log.ERROR, "WebSocket" , cause.toString() )
        }

        override fun onStateChanged(websocket: WebSocket?, newState: WebSocketState?) {

            Log.println(Log.INFO, "WebSocket",  newState.toString() )
            if(newState == WebSocketState.OPEN)
            {
                invokeHandlers(STATE.CONNECTED)
            }
            else
                invokeHandlers(STATE.NOT_CONNECTED)
        }
    }

    init {
        mWebSocket.addListener(mWebSocketListener)
        mWebSocket.connectAsynchronously()
    }

    override fun send(data : String) 
    {
        //println(data)
        mWebSocket.sendFrame( WebSocketFrame.createTextFrame( data ) )
    }

    override fun recv(): String {
        return mLastMessage
    }

    fun disconnect()
    {
        mWebSocket.sendClose()
        mWebSocket.disconnect()
    }
}
