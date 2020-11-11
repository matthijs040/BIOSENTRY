package com.example.androidbridge.mocks

import com.biosentry.androidbridge.communication.*

class ROSTwistDeviceMock : IROSDevice {

    var latestData : Twist? = null

    fun doStoreData(msg : ROSMessage)
    {
        if(msg is Twist)
            latestData = msg
    }

    override val mControls: List<ROSControl> =
        listOf(
            ROSControl(SubscribeMessage(type = "geometry_msgs/Twist", topic = "/mock/Twist"), ::doStoreData),
        )
}