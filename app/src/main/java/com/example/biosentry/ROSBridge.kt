package com.example.biosentry

import android.hardware.Sensor
import android.util.Log
import com.google.gson.Gson
import com.neovisionaries.ws.client.*

class ROSBridge(uri: String) {
    // Class for conversion between Data classes and JSON formatted strings.
    private val mGson : Gson = Gson()

    private val mWebSocket : WebSocket = WebSocketFactory().createSocket(uri)

    var mErrorHandler  : ( (String) -> Unit )?      = null
    var mStatusHandler : ( (String) -> Unit )?      = null
    var mDataHandler   : ( (ROSMessage<Any>) -> Unit)?   = null

    private val mWebSocketListener : WebSocketAdapter = object : WebSocketAdapter() {
        override fun onTextFrame(websocket: WebSocket?, frame: WebSocketFrame?) {
            //val data = frame.toString()
            /**
             * TODO : Parse string and create struct.
             */
            mDataHandler?.invoke(ROSMessage(type = "Empty", msg = ""))
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

    private fun setAccelTransmission(rate : Int)
    {

    }

    fun setTransmission(sensorType : Int, rate : Int)
    {
        when(sensorType)
        {
            Sensor.TYPE_ACCELEROMETER -> setAccelTransmission(rate)
            else -> return

        }
    }

    fun<T> send(data : ROSMessage<T>)
    {
        val json = mGson.toJson(data)

        mWebSocket.sendFrame( WebSocketFrame.createTextFrame( json ) )
    }

    fun advertise( typeName : String)
    {
        send( ROSMessage<Unit>(op = "advertise", type = typeName, msg = Unit))
    }

    fun unadvertise( typeName : String)
    {
        send( ROSMessage<Unit>(op = "unadvertise", type = typeName, msg = Unit))
    }

    fun disconnect()
    {
        mWebSocket.disconnect()
    }


}