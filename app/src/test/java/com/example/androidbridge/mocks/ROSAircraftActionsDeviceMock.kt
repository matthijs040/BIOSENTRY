package com.example.androidbridge.mocks

import com.biosentry.androidbridge.communication.*

class ROSAircraftActionsDeviceMock : IROSDevice {
    var latestData : AircraftFlightActions? = null

    fun doStoreData(msg : ROSMessage)
    {
        if(msg is AircraftFlightActions)
            latestData = msg
    }

    override val mControls: List<ROSControl> =
        listOf(
            ROSControl(SubscribeMessage(type = "/biosentry/AircraftFlightActions", topic = "/biosentry/AircraftFlightActions" ), ::doStoreData),
        )
}