package com.biosentry.androidbridge.aircraft

import com.biosentry.androidbridge.IROSSensor
import com.biosentry.androidbridge.Point
import com.biosentry.androidbridge.ROSMessage
import com.biosentry.androidbridge.Vector3
import dji.common.flightcontroller.FlightControllerState
import dji.sdk.products.Aircraft
import dji.sdk.sdkmanager.DJISDKManager
import java.lang.Exception

class AircraftGyroscope : IROSSensor<Point> {
    override var mMessageTypeName: String = "geometry_msgs/Point"

    // Can be renamed in case of different drone connected to phone at run time.
    override var mMessageTopicName: String = ""
        set(value)
        {
            mReading.topic = value
            field = value
        }

    override var mDataHandler: ((ROSMessage<Point>) -> Unit)? = null

    private var mReading : ROSMessage<Point> =
        ROSMessage(
            type = mMessageTypeName,
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

    override fun read(): ROSMessage<Point> {
        return mReading
    }

    init {
        val product = DJISDKManager.getInstance().product
        if(product !is Aircraft || product.model == null)
            throw Exception("Valid Aircraft required for initialization")
        else
        {
            mMessageTopicName = String.format("android/%s/Gyro", product.model.name)
            product.flightController?.setStateCallback(mGyroCallback)
        }
    }
}