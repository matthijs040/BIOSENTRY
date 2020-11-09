package com.biosentry.androidbridge.communication

import java.util.*
import kotlin.concurrent.timerTask

class ROSMessageHandler(private val bridge : ROSBridge) {

    private val mTimer : Timer = Timer()

    fun attachSensor(sensor: IROSSensor<*>, rateInMs: Long ) : Boolean
    {
        bridge.advertise( sensor.mMessageTypeName, sensor.mMessageTopicName)
        if(rateInMs <= 0L)
        {
            sensor.mDataHandler = bridge::send
        }
        else
        {
            mTimer.schedule(
                timerTask {
                        bridge.send( sensor.read() )
                },1000, rateInMs )
        }

        return true
    }

    fun attachDevice(device : IROSDevice)
    {
        device.mControls.forEach{
            when(it.message.op)
            {
                "advertise" -> bridge.advertise(it.message.type, it.message.topic)
                "subscribe" -> bridge.subscribe(it.message.type, it.message.topic)
            }
        }
    }

    fun removeSensors()
    {
        mTimer.cancel()
        mTimer.purge()
    }

    init {
        // Dummy first advertise to make connecting sensors work.
        // First connect always failed.
        bridge.advertise("std_msgs/Empty", "/android/phone/empty")
        Thread.sleep(1000)
    }
}