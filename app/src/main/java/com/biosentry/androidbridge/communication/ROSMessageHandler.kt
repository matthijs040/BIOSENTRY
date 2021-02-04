package com.biosentry.androidbridge.communication

import android.util.Log
import com.biosentry.androidbridge.serialization.IBridgeMessageSerializer
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.timerTask

class ROSMessageHandler(private val bridge : IJSONTranceiver,
                        private val mSerializer : IBridgeMessageSerializer,
                        )
{

    class HandledControl(val control : ROSControl, var canReceive : Boolean)
    class HandledSensor( val sensor : IROSSensor, var canSend : Boolean, val rateInMs: Long)

    private var nonce : AtomicInteger = AtomicInteger(0)
    private val messagingTimer = Timer()
    private val devicePollingTimer = Timer()
    private val mHandledControls = mutableListOf<HandledControl>()
    private val mHandledSensors  = mutableListOf<HandledSensor>()
    private var mCanSend : Boolean = true

    fun send(data : BridgeMessage)
    {
        if(mCanSend)
            bridge.send(mSerializer.toJson(data))
    }

    private fun handleStatus( msg : StatusMessage)
    {
        if(msg.msg == "advertise_ack")
        {
            val foundSensor = mHandledSensors.find { it.sensor.mAdvertiseMessage.id == msg.id }

            if( foundSensor != null)
                foundSensor.canSend = true
            else
                Log.w(this.javaClass.simpleName, "received an ack for an advertise that was not sent. Ignoring.")

        }
        else if(msg.msg.contains("advertise_nack"))
        {
            if(!mHandledSensors.remove( mHandledSensors.find { it.sensor.mAdvertiseMessage.id == msg.id } ) )
                Log.w(this.javaClass.simpleName, "received a nAck for an advertise that was not sent. Ignoring.")
        }
        else if(msg.msg == "subscribe_ack")
        {
           mHandledControls.find { it.control.message.id == msg.id }?.canReceive = true

        }
        else if(msg.msg.contains("subscribe_nack"))
        {
            if(!mHandledControls.remove( mHandledControls.find { it.control.message.id == msg.id } ) )
                Log.w(this.javaClass.simpleName, "received a nack for an unregistered / not subscribe-requested sensor. ignoring.")
        }
    }

    private fun handlePublish( msg : PublishMessage)
    {
        // Iterate over all receivers.
        mHandledControls.forEach{

            // If the receiver is allowed to receive and its type matches the message
            if(it.canReceive && msg.topic == it.control.message.topic )
            {
                it.control.behavior.invoke(msg.msg)
            }
        }
    }

    private fun recv(jsonData : String)
    {
        val msg = mSerializer.fromJson(jsonData)

        // If it is a message for one of the devices to receive.
        if(msg is PublishMessage)
            handlePublish(msg)

        // If it is an advertise or subscribe response
        if(msg is StatusMessage)
            handleStatus(msg)

    }


    fun attachSensor(sensor: IROSSensor, rateInMs: Long ) : Boolean
    {
        if( mHandledSensors.contains(HandledSensor(sensor, true, rateInMs) ) )
        { return false }

        sensor.mAdvertiseMessage.id = nonce.getAndIncrement().toString()
        val handledSens = HandledSensor(sensor, false, rateInMs)
        mHandledSensors.add(handledSens)

        messagingTimer.schedule(
            timerTask {
                val sens = mHandledSensors.find { it.sensor == handledSens.sensor }
                if(sens != null) {
                    if (sens.canSend)     // If ack has been received for this sensor and it can send.
                    {
                        if (sens.rateInMs == 0L)
                        {
                            sens.sensor.mDataHandler = ::send
                        }
                        else
                            devicePollingTimer.schedule(
                                timerTask {
                                    send(sens.sensor.read())
                                }, sens.rateInMs, sens.rateInMs
                            )

                        this.cancel()       // Cancel retransmissions of the advertisements.
                        println(this.javaClass.simpleName + " | attached sensor: " + sensor.javaClass.simpleName)
                    }
                    else
                        send(sens.sensor.mAdvertiseMessage) // Re-advertise.
                }
                else
                {
                    println("Server returned error response for sensor: " + handledSens.sensor.javaClass.simpleName)
                    this.cancel()
                }

            }, retransmissionRate, retransmissionRate
        )

        return true
    }

    fun attachDevice(device : IROSDevice)
    {
        device.mControls.forEach{
            attachControl(it)
        }
    }

    fun attachControl(control : ROSControl) : Boolean
    {
        if(mHandledControls.contains(HandledControl(control, true)) )
        {
            return false
        }

        control.message.id = nonce.getAndIncrement().toString()
        val controlToAdd = HandledControl(control, false)
        mHandledControls.add(controlToAdd)
        send(control.message)
        println(this.javaClass.simpleName + " | attached control: " + control.javaClass.simpleName)

        messagingTimer.schedule(
            timerTask {
                val ctrl = mHandledControls.find { it.control == controlToAdd.control }
                if(ctrl != null) {
                    if (ctrl.canReceive)     // If ack has been received for this ctrl.
                    {
                        this.cancel()       // Cancel retransmissions of the subscribees.
                        println(this.javaClass.simpleName + " | attached control: " + ctrl.control.javaClass.simpleName)
                    }
                    else
                        send(ctrl.control.message) // Re-advertise.
                }
                else
                {
                    println("Server returned error response for control: " + controlToAdd.control.javaClass.simpleName)
                    this.cancel()
                }

            }, retransmissionRate, retransmissionRate
        )


        return true
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
            send( SetStatusLevelMessage( level = "info" ) )
        }
    }

    companion object {
        const val retransmissionRate : Long = 100L
    }
}