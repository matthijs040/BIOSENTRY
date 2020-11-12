package com.biosentry.androidbridge.communication

import com.biosentry.androidbridge.BuildConfig
import com.biosentry.androidbridge.communication.*
import dji.common.flightcontroller.FlightControllerState
import dji.sdk.products.Aircraft
import dji.sdk.sdkmanager.DJISDKManager
import java.lang.Exception

open class ROSGyroscope(
    final override val mMessageTopicName : String,
    override val mMessageTypeName: String = "/geometry_msgs/Point",
    override val mAdvertiseMessage : AdvertiseMessage = AdvertiseMessage(
                         type = mMessageTypeName,
                         topic = mMessageTopicName ) ) : IROSSensor
{
    override var mDataHandler: ((PublishMessage) -> Unit)? = null

    var mReading : PublishMessage =
        PublishMessage(
            //type = mMessageTypeName,
            topic = mMessageTopicName,
            msg = Point(
                Double.NaN,
                Double.NaN,
                Double.NaN
            )
        )

    override fun updateData(data: ROSMessage) {
        if (BuildConfig.DEBUG && data !is Point) {
            error("Assertion failed")
        }
        mReading.msg = data
        mDataHandler?.invoke(mReading)
    }

    override fun read(): PublishMessage {
        return mReading
    }

}