package com.biosentry.androidbridge.communication

import android.app.Application
import android.util.Log
import com.biosentry.androidbridge.serialization.IBridgeMessageSerializer
import com.google.gson.*
import java.lang.reflect.Type
import java.util.*
import kotlin.concurrent.timerTask




class ROSMessageHandler(private val bridge : IJSONTranceiver,
                        private val mSerializer : IBridgeMessageSerializer)
{

    private val messagingTimer = Timer()
    private val devicePollingTimer = Timer()
    val mControls = mutableListOf<ROSControl>()
    private val retransmitDelay : Long = 10

    fun send(data : BridgeMessage)
    {
        bridge.send(mSerializer.toJson(data))
    }

    /**
     * Function that sends out a subscribe message while no publisher is yet registered.
     */
    private fun resubscribe(msg : SubscribeMessage)
    {
        messagingTimer.schedule(
            timerTask {
                while(true)
                {
                    send(msg)
                    Thread.sleep(retransmitDelay * 200) // Waiting for some arbitrary process might take long. This should be a non-performance affecting routine.
                }
            }, retransmitDelay
        )
    }

    private fun retransmit(msg : BridgeMessage, times : Int)
    {
        messagingTimer.schedule(
            timerTask {
                for( x in 0 .. times)
                {
                    send(msg) // send second time in case of bad reception.
                    Thread.sleep(retransmitDelay)
                }
            }, retransmitDelay
        )
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
        retransmit(sensor.mAdvertiseMessage, 3 )
        if(rateInMs <= 0L)
        {
            sensor.mDataHandler = ::send
        }
        else
        {
           devicePollingTimer.schedule(
               timerTask {
                   send( sensor.read() )
               },600, rateInMs )
        }

        Log.d(this.javaClass.simpleName, "attached sensor: " + sensor.javaClass.simpleName)

        return true
    }

    fun attachDevice(device : IROSDevice)
    {
        device.mControls.forEach{
            resubscribe(it.message)

            mControls.add(it)
            Log.d(this.javaClass.simpleName, "attached control: " + it.javaClass.simpleName)
        }
    }

    fun removeSensors()
    {
        devicePollingTimer.cancel()
        devicePollingTimer.purge()
    }

    init
    {
        bridge.mReceiver = ::recv
    }
}