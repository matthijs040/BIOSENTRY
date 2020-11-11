package com.example.androidbridge.mocks

import com.biosentry.androidbridge.communication.*

class ROSPointDeviceMock : IROSDevice {

    var latestData : Point? = null

    fun doStoreData(msg : ROSMessage)
    {
        if(msg is Point)
            latestData = msg
    }

    override val mControls: List<ROSControl> =
        listOf(
            ROSControl(SubscribeMessage(type = "/geometry_msgs/Point", topic = "/mock/Point"), ::doStoreData),
        )
}