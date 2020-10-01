package com.example.biosentry

import kotlinx.coroutines.delay
import java.util.*
import kotlin.concurrent.timerTask

class ROSMessageHandler(private val bridge : ROSBridge) {

    private val mTimer : Timer = Timer()

    fun attachSensor(sensor: IROSSensor<*>, rateInMs: Long )
    {
        bridge.advertise( sensor.mMessageTypeName, sensor.mMessageTopicName)


        if(rateInMs == 0L)
        {
            sensor.mDataHandler = bridge::send
        }
        else
        {
            mTimer.schedule(
                timerTask {
                        bridge.send( sensor.read() )
                },0, rateInMs )
        }
    }

    fun removeSensors()
    {
        mTimer.cancel()
        mTimer.purge()
    }
}