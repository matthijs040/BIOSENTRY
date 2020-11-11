package com.biosentry.androidbridge.serialization

import com.biosentry.androidbridge.communication.*
import com.google.gson.*
import java.lang.reflect.Type

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


class GsonSerializer : IBridgeMessageSerializer
{
    private var mGson : Gson

    override fun toJson(msg: BridgeMessage): String {
        return mGson.toJson(msg)
    }

    override fun fromJson(msg: String): BridgeMessage {
        return mGson.fromJson(msg, BridgeMessage::class.java)
    }

    init {
        val builder = GsonBuilder()
        builder.registerTypeAdapter(BridgeMessage::class.java, BridgeMessageDeserializer())
        mGson = builder.create()
    }
}