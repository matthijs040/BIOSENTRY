package com.example.biosentry

/**
 * Abstract interface for a ROS-bridge compliant sensor.
 * MessageType must be specialized into a message type defined in ROSMessages.kt.
 */
interface IROSSensor< MessageType > {

    // Push interface. Allows user to set function here to receive data as it comes in.
    var mDataHandler :  ( (ROSMessage<MessageType>) -> Unit)?

    // Name of the message type and topic contained in the ROS Message.
    val mMessageTypeName : String
    val mMessageTopicName : String

    // Pull interface. Allows user to request latest data when needed.
    fun read() : ROSMessage<MessageType>
}