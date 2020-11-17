package com.biosentry.androidbridge.communication

import android.app.ActivityManager
import android.app.Application
import android.util.Log
import com.biosentry.androidbridge.MApplication
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
    private val retransmitDelay : Long = 50
    private var mCanSend : Boolean = true

    fun send(data : BridgeMessage)
    {
        if(mCanSend)
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
            }, 20
        )
    }

    private fun retransmit(msg : BridgeMessage, times : Int)
    {
        for( x in 0 .. times)
        {
            send(msg) // send second time in case of bad reception.
            Thread.sleep(retransmitDelay)
        }
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
            devicePollingTimer.schedule(
                timerTask {
                    sensor.mDataHandler = ::send
                }, 2000
            )

            println(this.javaClass.simpleName + " | attached sensor: " + sensor.javaClass.simpleName)
        }
        else
        {
           devicePollingTimer.schedule(
               timerTask {
                   send( sensor.read() )
               },1000, rateInMs

           )
            println(this.javaClass.simpleName + " | attached sensor: " + sensor.javaClass.simpleName)
        }
        return true
    }

    fun attachDevice(device : IROSDevice)
    {
        device.mControls.forEach{
            attachControl(it)
        }
    }

    fun attachControl(control : ROSControl)
    {
        retransmit(control.message, 3) // Quick bursts of advertisements for initial connection.
        Thread.sleep(50)
        resubscribe(control.message) // Resubscribe for long term connection maintenance.
        mControls.add(control)
        println(this.javaClass.simpleName + " | attached control: " + control.javaClass.simpleName)
    }

    fun removeSensors()
    {
        devicePollingTimer.cancel()
        devicePollingTimer.purge()
    }

    init
    {
        bridge.attachReceiver(::recv)

        bridge.attachHandler {
            mCanSend = it == STATE.CONNECTED
            send( SetStatusLevelMessage( level = "error" ) )
            send(AdvertiseMessage(topic = "/android/dummy", type = "std_msgs/Empty"))
        }
    }
}