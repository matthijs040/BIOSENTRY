package com.biosentry.androidbridge.communication

import android.util.Log
import com.biosentry.androidbridge.serialization.IBridgeMessageSerializer
import com.google.gson.*
import java.lang.reflect.Type
import java.util.*
import kotlin.concurrent.timerTask




class ROSMessageHandler(private val bridge : IJSONTranceiver,
                        private val mSerializer : IBridgeMessageSerializer)
{

    private val mTimer : Timer = Timer()
    val mControls = mutableListOf<ROSControl>()


    fun send(data : BridgeMessage)
    {
        bridge.send(mSerializer.toJson(data))
    }

    private fun recv(jsonData : String)
    {
        val msg = mSerializer.fromJson(jsonData)

        // If it is a message for one of the devices to receive.
        if(msg is PublishMessage)
        {
            // Iterate over all receivers.
            mControls.forEach{

                // If the receiver's msg-data type matches the message
                if(msg.topic == it.message.topic)
                {
                    it.behavior.invoke(msg.msg)
                }
            }
        }
    }

    fun attachSensor(sensor: IROSSensor, rateInMs: Long ) : Boolean
    {
        send(sensor.mAdvertiseMessage)
        //mTimer.schedule(
        //    timerTask {
        //        send(sensor.mAdvertiseMessage) // send second time in case of bad reception.
        //    }, 500
        //)

        if(rateInMs <= 0L)
        {
            sensor.mDataHandler = ::send
        }
        else
        {
           mTimer.schedule(
               timerTask {
                   send( sensor.read() )
               },600, rateInMs )
        }

        Log.d(this.javaClass.simpleName, "attached sensor: $sensor")

        return true
    }

    fun attachDevice(device : IROSDevice)
    {
        device.mControls.forEach{
            send(it.message)

           // mTimer.schedule(
           //     timerTask {
           //         send(it.message) // send second time in case of bad reception.
           //     }, 500
           // )
            Log.d(this.javaClass.simpleName, "attached control: $it")

            mControls.add(it)
        }
    }

    fun removeSensors()
    {
        mTimer.cancel()
        mTimer.purge()
    }

    init
    {
        bridge.mReceiver = ::recv
    }
}