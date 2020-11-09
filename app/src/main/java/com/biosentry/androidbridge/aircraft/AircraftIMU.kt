package com.biosentry.androidbridge.aircraft

import com.biosentry.androidbridge.communication.IROSSensor
import com.biosentry.androidbridge.communication.ROSMessage
import com.biosentry.androidbridge.communication.Vector3
import dji.common.flightcontroller.FlightControllerState
import dji.sdk.products.Aircraft
import dji.sdk.sdkmanager.DJISDKManager
import java.lang.Exception

class AircraftIMU() : IROSSensor<Vector3>
{
    override var mMessageTypeName: String = "geometry_msgs/Vector3"

    // Can be renamed in case of different drone connected to phone at run time.
    override var mMessageTopicName: String = ""
        set(value)
        {
            mReading.topic = value
            field = value
        }

    override var mDataHandler: ((ROSMessage<Vector3>) -> Unit)? = null

    private var mReading : ROSMessage<Vector3> =
        ROSMessage(
            type = mMessageTypeName,
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
            if(mDataHandler != null)
                mDataHandler!!.invoke(read())
        }

    }

    override fun read(): ROSMessage<Vector3> {
            return mReading
    }

    init {
        val product = DJISDKManager.getInstance().product
        if(product !is Aircraft || product.model == null)
            throw Exception("Valid Aircraft required for initialization")
        else
        {
            mMessageTopicName = String.format("android/%s/IMU", product.model.name)
            product.flightController?.setStateCallback(mIMUCallback)
        }

    }

}

