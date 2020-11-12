package com.biosentry.androidbridge.communication

import com.biosentry.androidbridge.BuildConfig
import com.biosentry.androidbridge.communication.*
import dji.common.flightcontroller.FlightControllerState
import dji.sdk.products.Aircraft
import dji.sdk.sdkmanager.DJISDKManager
import java.lang.Exception

open class ROSAccelerometer(
    final override val mMessageTopicName : String,
    override val mMessageTypeName: String = "/geometry_msgs/Vector3",
    override val mAdvertiseMessage : AdvertiseMessage =
                       AdvertiseMessage(
                        type = mMessageTypeName,
                        topic = mMessageTopicName ) ) : IROSSensor
{

    override var mDataHandler: ((PublishMessage) -> Unit)? = null
    override fun updateData(data: ROSMessage) {
        if (BuildConfig.DEBUG && data !is Vector3) {
            error("Assertion failed")
        }
        mReading.msg = data
        mDataHandler?.invoke(mReading)
    }

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


    override fun read(): PublishMessage {
            return mReading
    }
}

