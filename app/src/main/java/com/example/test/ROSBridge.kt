package com.example.test

import android.util.Log
import com.google.gson.Gson
import com.neovisionaries.ws.client.*
import java.util.*

class ROSBridge(uri: String) {
    // Class for conversion between Data classes and JSON formatted strings.
    private val mGson : Gson = Gson()

    private val mWebSocket : WebSocket = WebSocketFactory().createSocket(uri)

    var mErrorHandler  : ( (String) -> Unit )?      = null
    var mStatusHandler : ( (String) -> Unit )?      = null
    var mDataHandler   : ( (ROSMessage) -> Unit)?   = null

    private val mWebSocketListener : WebSocketAdapter = object : WebSocketAdapter() {
        override fun onTextFrame(websocket: WebSocket?, frame: WebSocketFrame?) {
            val data = frame.toString()
            /**
             * TODO : Parse string and create struct.
             */
            mDataHandler?.invoke(ROSMessage(type = "Empty"))
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

    fun send(data : ROSMessage)
    {
        mWebSocket.sendFrame( WebSocketFrame.createTextFrame( mGson.toJson(data) ) )
    }

    fun advertise( typeName : String)
    {
        send( ROSMessage(op = "advertise", type = typeName))
    }

    fun unadvertise( typeName : String)
    {
        send( ROSMessage(op = "unadvertise", type = typeName))
    }

    fun disconnect()
    {
        mWebSocket.disconnect()
    }


}