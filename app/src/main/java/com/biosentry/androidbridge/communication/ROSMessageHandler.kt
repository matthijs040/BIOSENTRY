package com.biosentry.androidbridge.communication

import com.biosentry.androidbridge.serialization.IBridgeMessageSerializer
import com.google.gson.*
import java.lang.reflect.Type
import java.util.*
import kotlin.concurrent.timerTask




class ROSMessageHandler(private val bridge : IJSONTranceiver,
                        private val mSerializer : IBridgeMessageSerializer)
{

    private val mTimer : Timer = Timer()
    private val mControls = mutableListOf<ROSControl<*>>()


    fun send(data : BridgeMessage)
    {
        bridge.send(mSerializer.toJson(data))
    }

    private fun recv(jsonData : String)
    {
        println(jsonData)
        val msg = mSerializer.fromJson(jsonData)

        // If it is a message for one of the devices to receive.
        if(msg is PublishMessage<*>)
        {
            // Iterate over all receivers.
            mControls.forEach{

                // If the receiver's msg-data type matches the message
                if(msg.msg!!::class.java == it.type)
                {

                    // Try to send the data to it.
                    it.tryCall( PublishMessage(
                        type = msg.type,
                        topic = msg.topic,
                        msg = msg.msg!!)
                    )
                }
            }
        }
    }

    fun attachSensor(sensor: IROSSensor<*>, rateInMs: Long ) : Boolean
    {
        send(sensor.mAdvertiseMessage)
        mTimer.schedule(
            timerTask {
                send(sensor.mAdvertiseMessage) // send second time in case of bad reception.
            }, 500
        )

        if(rateInMs <= 0L)
        {
            sensor.mDataHandler = ::send
        }
        else
        {
            mTimer.schedule(
                timerTask {
                    send( sensor.read() )
                },1000, rateInMs )
        }

        return true
    }

    fun attachDevice(device : IROSDevice)
    {
        device.mControls.forEach{
            send(it.message)
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