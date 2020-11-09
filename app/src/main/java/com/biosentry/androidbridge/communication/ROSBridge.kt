package com.biosentry.androidbridge.communication

import android.util.Log
import com.google.gson.Gson
import com.neovisionaries.ws.client.*

class ROSBridge(uri: String) {
    // Class for conversion between Data classes and JSON formatted strings.
    private val mGson : Gson = Gson()

    private val mWebSocket : WebSocket = WebSocketFactory().createSocket(uri)

    var mErrorHandler  : ( (String) -> Unit )?      = null
    var mStatusHandler : ( (String) -> Unit )?      = null
    var mDataHandler   : ( (PublishMessage<Any>) -> Unit)?   = null

    private val mWebSocketListener : WebSocketAdapter = object : WebSocketAdapter() {
        override fun onFrame(websocket: WebSocket?, frame: WebSocketFrame?) {
            super.onFrame(websocket, frame)

            Log.println(Log.WARN, "ROSBridge", frame.toString())
            /**
             * TODO : Parse string and create struct.
             */
            mDataHandler?.invoke(PublishMessage(type = "Empty", msg = "", topic = ""))

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

    fun send(data : PublishMessage<*>)
    {
        val json = mGson.toJson(data)
        mWebSocket.sendFrame( WebSocketFrame.createTextFrame( json ) )
    }

    fun advertise( typeName: String, topicName : String)
    {
        send( PublishMessage(op = "advertise", type = typeName, topic = topicName, msg = Unit) )
    }

    fun unadvertise( typeName : String, topicName: String)
    {
        send( PublishMessage(op = "unadvertise", topic = topicName, type = typeName, msg = Unit))
    }

    fun disconnect()
    {
        mWebSocket.disconnect()
    }

    fun subscribe(type: String, topic: String) {
        send(PublishMessage(op="advertise", type = type, topic = topic, msg = Unit))
    }


}