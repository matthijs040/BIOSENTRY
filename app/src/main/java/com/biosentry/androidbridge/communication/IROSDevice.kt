package com.biosentry.androidbridge.communication


/**
 * A structure representing a piece of a ROS device whose behavior can be controlled through messages.
 * The specification of the message is given here, the consequent behavior is implemented in the IROSDevice compliant class.
 * This can be direct behavior (e.g. setting a motor's RPM with an int message)
 * or meta behavior (e.g. changing configuration of a sensor)
 */
data class ROSControl<MessageType>(val message : PublishMessage<MessageType>,
                                   val behavior : (PublishMessage<MessageType>) -> Unit)

/**
 * Generic interface for a ROS controllable device.
 * Accepting a commands that changes the device's behavior.
 */
interface IROSDevice {
    val mControls : List<ROSControl<*>>
}