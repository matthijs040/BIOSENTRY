package com.biosentry.androidbridge.aircraft

import com.biosentry.androidbridge.communication.AdvertiseMessage
import com.biosentry.androidbridge.communication.IROSSensor
import com.biosentry.androidbridge.communication.Point
import com.biosentry.androidbridge.communication.PublishMessage
import dji.common.flightcontroller.FlightControllerState
import dji.sdk.products.Aircraft
import dji.sdk.sdkmanager.DJISDKManager
import java.lang.Exception

class AircraftGyroscope : IROSSensor {

    override val mMessageTypeName: String = "/geometry_msgs/Point"
    override val mMessageTopicName = "/android/drone/Gyro"
    override val mAdvertiseMessage = AdvertiseMessage(
        type = mMessageTypeName,
        topic = mMessageTopicName
    )

    override var mDataHandler: ((PublishMessage) -> Unit)? = null

    private var mReading : PublishMessage =
        PublishMessage(
            //type = mMessageTypeName,
            topic = mMessageTopicName,
            msg = Point(
                Double.NaN,
                Double.NaN,
                Double.NaN
            )
        )

    private val mGyroCallback = FlightControllerState.Callback { p0 ->
        if(p0 != null)
        {
            mReading.msg = Point(
                p0.attitude.roll,
                p0.attitude.pitch,
                p0.attitude.yaw
            )
            if(mDataHandler != null)
                mDataHandler!!.invoke(read())
        }

    }

    override fun read(): PublishMessage {
        return mReading
    }



    init {
        val product = DJISDKManager.getInstance().product
        if(product !is Aircraft || !product.isConnected)
            throw Exception("Valid Aircraft required for initialization")
        else
        {
            product.flightController?.setStateCallback(mGyroCallback)
        }
    }
}