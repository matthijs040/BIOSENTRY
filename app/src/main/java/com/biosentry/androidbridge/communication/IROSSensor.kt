package com.biosentry.androidbridge.communication

/**
 * Abstract interface for a ROS-bridge compliant sensor.
 * MessageType must be specialized into a message type defined in ROSMessages.kt.
 */
interface IROSSensor< MessageType > {

    // Name of the message type and topic contained in the ROS Message.
    val mMessageTypeName : String
    val mMessageTopicName : String

    // Push interface. Allows user to set function here to receive data as it comes in.
    var mDataHandler :  ( (ROSMessage<MessageType>) -> Unit)?

    // Pull interface. Allows user to request latest data when needed.
    fun read() : ROSMessage<MessageType>
}