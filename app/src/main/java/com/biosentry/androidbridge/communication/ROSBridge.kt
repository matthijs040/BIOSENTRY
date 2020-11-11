package com.biosentry.androidbridge.communication

import android.util.Log
import com.google.gson.Gson
import com.neovisionaries.ws.client.*
import kotlin.reflect.typeOf

class ROSBridge(uri: String) : IJSONTranceiver {
    // Class for conversion between Data classes and JSON formatted strings.

    private val mWebSocket : WebSocket = WebSocketFactory().createSocket(uri)

    var mErrorHandler  : ( (String) -> Unit )?      = null
    override var mReceiver: ((String) -> Unit)? = null
    override var mStateHandler: ((STATE) -> Unit)? = null

    private var mLastMessage : String = ""

    private val mWebSocketListener : WebSocketAdapter = object : WebSocketAdapter() {
        override fun onFrame(websocket: WebSocket?, frame: WebSocketFrame?) {
            super.onFrame(websocket, frame)
            frame?.let {  mReceiver?.invoke(it.payloadText) }
        }

        override fun onError(websocket: WebSocket?, cause: WebSocketException?) {
            mErrorHandler?.invoke(cause.toString())
            Log.println(Log.ERROR, "WebSocket" , cause.toString() )
        }

        override fun onStateChanged(websocket: WebSocket?, newState: WebSocketState?) {
            if(newState == WebSocketState.OPEN)
                mStateHandler?.invoke(STATE.CONNECTED)
            else
                mStateHandler?.invoke(STATE.NOT_CONNECTED)

            Log.println(Log.INFO, "WebSocket",  newState.toString() )
        }
    }

    init {
        mWebSocket.addListener(mWebSocketListener)
        mWebSocket.connectAsynchronously()
    }


    override fun send(data : String)
    {
        mWebSocket.sendFrame( WebSocketFrame.createTextFrame( data ) )
    }

    override fun recv(): String {
        return mLastMessage
    }

    fun disconnect()
    {
        mWebSocket.disconnect()
    }
}