package com.biosentry.androidbridge.serialization

import com.biosentry.androidbridge.communication.BridgeMessage

/**
 * Interface for (de)serializing between ROSBridge messages and JSON strings.
 */
interface IBridgeMessageSerializer {

    fun toJson(msg : BridgeMessage) : String
    fun fromJson(msg : String) : BridgeMessage
}