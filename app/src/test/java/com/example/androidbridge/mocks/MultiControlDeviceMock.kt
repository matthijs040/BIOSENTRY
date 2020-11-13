package com.example.androidbridge.mocks

import com.biosentry.androidbridge.communication.*

class MultiControlDeviceMock : IROSDevice
{
    var mTwist : Twist? = null
    var mFActions : AircraftFlightActions? = null

    private fun handleTwist(msg : ROSMessage)
    {
        if(msg is Twist)
            mTwist = msg
    }

    private fun handleAircraftActions(msg : ROSMessage)
    {
        if(msg is AircraftFlightActions)
            mFActions = msg

    }
    override val mControls: List<ROSControl> = listOf(
        ROSControl(SubscribeMessage(topic = "/geometry_msgs/Twist"), ::handleTwist ),
        ROSControl(SubscribeMessage(type = "/biosentry/AircraftFlightActions", topic = "/biosentry/AircraftFlightActions"), ::handleAircraftActions),
    )

}