package com.biosentry.androidbridge.communication

import java.lang.reflect.Type
import kotlin.reflect.KClass
import kotlin.reflect.KTypeParameter


/**
 * A structure representing a piece of a ROS device whose behavior can be controlled through messages.
 * The specification of that message is given here, the consequent behavior is implemented in the IROSDevice compliant class.
 * This can be direct behavior (e.g. setting a motor's RPM with an int message)
 * or meta behavior (e.g. changing configuration of a sensor)
 */
data class ROSControl( val message : SubscribeMessage,
                       val behavior : (ROSMessage) -> Unit)

/**
 * Generic interface for a ROS controllable device.
 * Accepting a commands that changes the device's behavior.
 */
interface IROSDevice {
    val mControls : List<ROSControl>
}