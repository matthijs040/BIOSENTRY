package com.biosentry.androidbridge.communication

/**
 * Abstract interface for a ROS-bridge compliant sensor.
 * MessageType must be specialized into a message type defined in ROSMessages.kt.
 */
interface IROSSensor {

    // Name of the message type and topic contained in the ROS Message.
    val mMessageTypeName : String
    val mMessageTopicName : String
    val mAdvertiseMessage : AdvertiseMessage

    // Push interface. Allows user to set function here to receive data as it comes in.
    var mDataHandler :  ( (PublishMessage) -> Unit)?

    // Function that allows external introduction of correct data.
    fun updateData( data : ROSMessage)

    // Pull interface. Allows user to request latest data when needed.
    fun read() : PublishMessage
}