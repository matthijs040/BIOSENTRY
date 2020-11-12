package com.biosentry.androidbridge.serialization

import com.biosentry.androidbridge.communication.*
import com.google.gson.*
import java.lang.reflect.Type
import java.util.*


/**
 * Deserializer for the various specializations the "msg" field in PublishMessages.
 * Based on the MessageTypeName field in the PublishMessage.
 * Falls back on the generic Any if no match is found.
 */
class PublishMessageDeserializer : JsonDeserializer<PublishMessage?>
{
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): PublishMessage? {
        var ret : PublishMessage? = null

        if(json != null && context != null)
        {

            val obj = json.asJsonObject

            val topicString = obj.get("topic").asString

            val msg = when (topicString)
            {
                //Accessing nested JSON data. data is:
                // {"op":"publish","type":"/geometry_msgs/Point","topic":"/mock/Point","msg":{"x":1.0,"y":2.0,"z":3.0}}
                // from: https://stackoverflow.com/questions/8233542/parse-a-nested-json-using-gson
                "/geometry_msgs/Point" -> Point(
                    x = obj.get("msg").asJsonObject.get("x").asDouble,
                    y = obj.get("msg").asJsonObject.get("y").asDouble,
                    z = obj.get("msg").asJsonObject.get("z").asDouble )

                // Takes enum int value to be compatible with C,C++ / Python(?) enums.
                "/biosentry/AircraftFlightActions" -> ( FlightActions.getByValue(obj.get("msg").asJsonObject.get("flightActions").asInt) )?.let {
                    AircraftFlightActions( flightActions = it) }

                "/geometry_msgs/Twist" -> Twist(
                    Vector3(
                        obj.get("msg").asJsonObject.get("linear").asJsonObject.get("x").asDouble,
                        obj.get("msg").asJsonObject.get("linear").asJsonObject.get("y").asDouble,
                        obj.get("msg").asJsonObject.get("linear").asJsonObject.get("z").asDouble
                    ),
                    Vector3(
                        obj.get("msg").asJsonObject.get("angular").asJsonObject.get("x").asDouble,
                        obj.get("msg").asJsonObject.get("angular").asJsonObject.get("y").asDouble,
                        obj.get("msg").asJsonObject.get("angular").asJsonObject.get("z").asDouble
                    )
                )
                else -> null
            }

            if(msg != null)
                ret = PublishMessage(
                    topic = topicString,
                    msg = msg
                )
        }

        return ret
    }



}

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
                "advertise" ->      context.deserialize(json, AdvertiseMessage::class.java)
                "unadvertise" ->    context.deserialize(json, UnadvertiseMessage::class.java)
                "publish" ->        context.deserialize(json, PublishMessage::class.java)
                "subscribe" ->      context.deserialize(json, SubscribeMessage::class.java)
                "unsubscribe" ->    context.deserialize(json, UnsubscribeMessage::class.java)
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
        builder.registerTypeAdapter(PublishMessage::class.java, PublishMessageDeserializer())
        mGson = builder.create()
    }
}