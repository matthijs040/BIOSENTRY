package com.biosentry.androidbridge.communication

import com.biosentry.androidbridge.BuildConfig
import com.biosentry.androidbridge.communication.*
import dji.common.flightcontroller.FlightControllerState
import dji.common.flightcontroller.GPSSignalLevel
import dji.sdk.products.Aircraft
import dji.sdk.sdkmanager.DJISDKManager
import java.lang.Exception

open class ROSGPS(
    final override val mMessageTopicName : String,
    override val mMessageTypeName : String = "/sensor_msgs/NavSatFix",
    override val mAdvertiseMessage : AdvertiseMessage = AdvertiseMessage(
                      type = mMessageTypeName,
                      topic = mMessageTopicName
                  ) ) : IROSSensor
{
    override var mDataHandler: ((PublishMessage) -> Unit)? = null

    var mStatus = NavSatStatus(
        NavSatStatus.STATUS_NO_FIX,
        NavSatStatus.SERVICE_GLONASS + NavSatStatus.SERVICE_GPS
    )

    override fun updateData(data: ROSMessage) {

        if (BuildConfig.DEBUG && data !is NavSatFix) {
            error("Assertion failed")
        }
        mReading.msg = data
        mDataHandler?.invoke(mReading)
        mSeqNumber++

    }

    // Frame ID: I.E. Euclidean distance from vehicle centre to GPS antenna is currently unknown.
    var mSeqNumber : Long = 0
    var mHeader = Header(mSeqNumber, time(0,0), "N.A.")
    var  mReading = PublishMessage (
        //type = mMessageTypeName,
        topic = mMessageTopicName,
        msg = NavSatFix(
        mHeader,
        mStatus,
        0.0,
        0.0,
        0.0,
        position_covariance = DoubleArray(9),
        position_covariance_type = 0 ) )

    override fun read(): PublishMessage {
        return mReading
    }

}