package com.biosentry.androidbridge.aircraft

import com.biosentry.androidbridge.communication.AdvertiseMessage
import com.biosentry.androidbridge.communication.IROSSensor
import com.biosentry.androidbridge.communication.PublishMessage
import com.biosentry.androidbridge.communication.Vector3
import dji.common.flightcontroller.FlightControllerState
import dji.sdk.products.Aircraft
import dji.sdk.sdkmanager.DJISDKManager
import java.lang.Exception

class AircraftIMU() : IROSSensor
{
    override val mMessageTypeName = "geometry_msgs/Vector3"
    override val mMessageTopicName = "/android/drone/IMU"
    override val mAdvertiseMessage = AdvertiseMessage(
        type = mMessageTypeName,
        topic = mMessageTopicName
    )


    override var mDataHandler: ((PublishMessage) -> Unit)? = null

    private var mReading : PublishMessage =
        PublishMessage(
            //type = mMessageTypeName,
            topic = mMessageTopicName,
            msg = Vector3(
                Double.NaN,
                Double.NaN,
                Double.NaN
            )
    )


    private val mIMUCallback = FlightControllerState.Callback { p0 ->
        if(p0 != null)
        {
            mReading.msg = Vector3(
                p0.velocityX.toDouble(),
                p0.velocityY.toDouble(),
                p0.velocityZ.toDouble()
            )
            mDataHandler?.invoke(read())
        }

    }

    override fun read(): PublishMessage {
            return mReading
    }

    init {
        val product = DJISDKManager.getInstance().product
        if(product !is Aircraft || product.model == null)
            throw Exception("Valid Aircraft required for initialization")
        else
        {
            product.flightController!!.setStateCallback(mIMUCallback)
        }

    }

}

