package com.biosentry.androidbridge.communication

import java.util.*
import kotlin.concurrent.timerTask

class ROSMessageHandler(private val bridge : ROSBridge) {

    private val mTimer : Timer = Timer()
    private val mControls = mutableListOf<ROSControl<*>>()

    private fun handleData(msg : Any )
    {
        println(msg.toString())
    }

    fun attachSensor(sensor: IROSSensor<*>, rateInMs: Long ) : Boolean
    {
        bridge.send(sensor.mAdvertiseMessage)
        mTimer.schedule(
            timerTask {
                bridge.send(sensor.mAdvertiseMessage) // send second time in case of bad reception.
            }, 500
        )

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
            bridge.send(it.message)
            mControls.add(it)
        }
    }

    fun removeSensors()
    {
        mTimer.cancel()
        mTimer.purge()
    }

    fun sub(msg : SubscribeMessage)
    {
        bridge.send(msg)
        mTimer.schedule(
            timerTask {
                bridge.send(msg)
            }, 500
        )
    }

    init {
        // Dummy first advertise to make connecting sensors work.
        // First connect always failed.
        //bridge.send(AdvertiseMessage(type = "", topic = ""))
        //Thread.sleep(1000)

        bridge.mDataHandler = ::handleData
    }
}