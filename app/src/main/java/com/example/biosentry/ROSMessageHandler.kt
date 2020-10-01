package com.example.biosentry

import android.app.Activity
import android.content.Context
import java.util.*
import kotlin.concurrent.timerTask

class ROSMessageHandler(uri : String, context: Context, activity: Activity) {

    private val mROSBridge : ROSBridge = ROSBridge(uri)
    private val mTimer : Timer = Timer()

    fun attachSensor( sensor : IROSSensor<Any>, rateInMs : Long )
    {
        mROSBridge.advertise( sensor.mMessageTypeName, sensor.mMessageTopicName)

        if(rateInMs == 0L)
        {
            sensor.mDataHandler = mROSBridge::send
        }
        else
        {
            mTimer.schedule(
                timerTask {
                        mROSBridge.send( sensor.read() )
                },0, rateInMs )
        }
    }
}