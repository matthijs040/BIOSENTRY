package com.biosentry.androidbridge.communication

import com.google.gson.*
import java.lang.reflect.Type
import java.util.*
import kotlin.concurrent.timerTask

/**
 * Deserializer for all Bridge messages defined in ROSMessages.kt
 * From: https://stackoverflow.com/questions/21767485/gson-deserialization-to-specific-object-type-based-on-field-value
 * First answer and first comment.
 */
class BridgeMessageDeserializer : JsonDeserializer<BridgeMessage?>
{
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext? ): BridgeMessage? {

        var ret : BridgeMessage? = null

        if (json != null && context != null) {

            val obj = json.asJsonObject
            val bridgeOp = obj.get("op")

            ret = when (bridgeOp.asString) {
                "advertise" ->      context.deserialize<AdvertiseMessage>(json, AdvertiseMessage::class.java)
                "unadvertise" ->    context.deserialize<UnadvertiseMessage>(json, UnadvertiseMessage::class.java)
                "publish" ->        context.deserialize<PublishMessage<*>>(json, PublishMessage::class.java)
                "subscribe" ->      context.deserialize<SubscribeMessage>(json, SubscribeMessage::class.java)
                "unsubscribe" ->    context.deserialize<UnsubscribeMessage>(json, UnsubscribeMessage::class.java)
                else -> null
            }
        }
        return ret
    }
}


class ROSMessageHandler(private val bridge : ROSBridge) {

    private val mTimer : Timer = Timer()
    private val mControls = mutableListOf<ROSControl<*>>()
    private var mGson : Gson

    private fun<T> send(data : T)
    {
        bridge.send(mGson.toJson(data))
    }

    private fun recv(jsonData : String)
    {
        println(jsonData)
        val msg = mGson.fromJson(jsonData, BridgeMessage::class.java)
        if(msg is PublishMessage<*>)
        {
            mControls.forEach{
                if(msg.msg!!::class.java == it.type)
                {
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

    fun sub(msg : SubscribeMessage)
    {
        send(msg)
        mTimer.schedule(
            timerTask {
                send(msg)
            }, 500
        )
    }

    init
    {
        bridge.mReceiver = ::recv
        val builder = GsonBuilder()
        builder.registerTypeAdapter(BridgeMessage::class.java, BridgeMessageDeserializer())
        mGson = builder.create()
    }
}