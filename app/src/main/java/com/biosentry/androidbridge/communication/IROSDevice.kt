package com.biosentry.androidbridge.communication

import java.lang.reflect.Type
import kotlin.reflect.KClass
import kotlin.reflect.KTypeParameter


/**
 * A structure representing a piece of a ROS device whose behavior can be controlled through messages.
 * The specification of the message is given here, the consequent behavior is implemented in the IROSDevice compliant class.
 * This can be direct behavior (e.g. setting a motor's RPM with an int message)
 * or meta behavior (e.g. changing configuration of a sensor)
 * Requires the type to know with what data to call behavior at runtime.
 */
data class ROSControl<T : Any>( val type : KClass<T>,
                                val message : SubscribeMessage,
                                val behavior : (PublishMessage<T>) -> Unit)
{
    @Suppress("UNCHECKED_CAST")
    inline fun<reified T> tryCall(msg : PublishMessage<T>)
    {
        if(type is T)
            ( behavior as (PublishMessage<T>) -> Unit ).invoke(msg)
    }

}





/**
 * Generic interface for a ROS controllable device.
 * Accepting a commands that changes the device's behavior.
 */
interface IROSDevice {
    val mControls : List<ROSControl<*>>
}