package com.biosentry.androidbridge.aircraft

import com.biosentry.androidbridge.*
import dji.common.flightcontroller.FlightControllerState
import dji.sdk.products.Aircraft
import dji.sdk.sdkmanager.DJISDKManager
import java.lang.Exception
import kotlin.time.ExperimentalTime
import kotlin.time.nanoseconds

class AircraftGPS : IROSSensor<NavSatFix> {
    override var mMessageTypeName: String = "sensor_msgs/NavSatFix"

    // Can be renamed in case of different drone connected to phone at run time.
    override var mMessageTopicName: String = ""
        set(value)
        {
            mReading.topic = value
            field = value
        }

    override var mDataHandler: ((ROSMessage<NavSatFix>) -> Unit)? = null

    private var mStatus = NavSatStatus(
        NavSatStatus.STATUS_NO_FIX,
        NavSatStatus.SERVICE_GLONASS + NavSatStatus.SERVICE_GPS
    )

    private var mSeqNumber : Long = 0

    // Frame ID: I.E. Euclidean distance from vehicle centre to GPS antenna is currently unknown.
    private var mHeader = Header(mSeqNumber, time(0,0), "N.A.")

    private var  mReading = ROSMessage<NavSatFix> (
        type = mMessageTypeName,
        topic = mMessageTopicName,
        msg = NavSatFix(
        mHeader,
        mStatus,
        0.0,
        0.0,
        0.0,
        position_covariance = DoubleArray(9),
        position_covariance_type = 0 ) )

    private val mGPSCallback = FlightControllerState.Callback { p0 ->
        p0?.aircraftLocation?.let {

            mStatus = NavSatStatus(status = NavSatStatus.STATUS_FIX, service = NavSatStatus.SERVICE_GLONASS + NavSatStatus.SERVICE_GPS)
            mHeader = Header(seq = mSeqNumber, stamp = time(p0.flightTimeInSeconds.toLong(), 0), frame_id = "N.A.")

            mReading.msg = NavSatFix(
                mHeader,
                mStatus,
                if(it.latitude.isNaN()) 0.0 else it.latitude,
                if(it.longitude.isNaN()) 0.0 else it.longitude,
                if(it.altitude.isNaN()) 0.0 else it.altitude.toDouble(),
                DoubleArray(9),
                0
            )
            if(mDataHandler != null)
                mDataHandler!!.invoke(read())
            mSeqNumber++
        }

    }

    override fun read(): ROSMessage<NavSatFix> {
        return mReading
    }

    init {
        val product = DJISDKManager.getInstance().product
        if(product !is Aircraft || product.model == null)
            throw Exception("Valid Aircraft required for initialization")
        else
        {
            mMessageTopicName = String.format("android/%s/GPS", product.model.name)
            product.flightController?.setStateCallback(mGPSCallback)
        }

    }
}