package com.biosentry.androidbridge.communication

import android.util.Log
import com.google.gson.Gson
import com.neovisionaries.ws.client.*
import kotlin.reflect.typeOf

class ROSBridge(uri: String) {
    // Class for conversion between Data classes and JSON formatted strings.
    private val mGson : Gson = Gson()

    private val mWebSocket : WebSocket = WebSocketFactory().createSocket(uri)

    var mErrorHandler  : ( (String) -> Unit )?      = null
    var mStatusHandler : ( (String) -> Unit )?      = null
    var mDataHandler   : ( (Any) -> Unit)?   = null

    private val mWebSocketListener : WebSocketAdapter = object : WebSocketAdapter() {
        override fun onFrame(websocket: WebSocket?, frame: WebSocketFrame?) {
            super.onFrame(websocket, frame)
            mDataHandler?.invoke(mGson.fromJson(frame?.payloadText, Any::class.java))
        }

        override fun onError(websocket: WebSocket?, cause: WebSocketException?) {
            mErrorHandler?.invoke(cause.toString())
            Log.println(Log.ERROR, "WebSocket" , cause.toString() )
        }

        override fun onStateChanged(websocket: WebSocket?, newState: WebSocketState?) {
            mStatusHandler?.invoke( newState.toString() )


            Log.println(Log.INFO, "WebSocket",  newState.toString() )
        }
    }

    init {
        mWebSocket.addListener(mWebSocketListener)
        mWebSocket.connectAsynchronously()
    }

    fun<T> send(data : T)
    {
        val json = mGson.toJson(data)
        mWebSocket.sendFrame( WebSocketFrame.createTextFrame( json ) )
    }

    fun disconnect()
    {
        mWebSocket.disconnect()
    }
}