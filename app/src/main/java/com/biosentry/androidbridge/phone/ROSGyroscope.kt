package com.biosentry.androidbridge.phone

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import com.biosentry.androidbridge.IROSSensor
import com.biosentry.androidbridge.ROSMessage
import com.biosentry.androidbridge.Vector3

/**
 * Class containing logic to fetch gyroscope data from Android's native HAL.
 */
class ROSGyroscope(context: Context,
                   override val mMessageTypeName: String = "geometry_msgs/Vector3",
                   override val mMessageTopicName: String = "android/phone/gyroscope"
) : IROSSensor<Vector3>
{

    // Android HAL objects exposing the phone's sensors.
    private val mSensorManager: SensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private var mGyroscope: Sensor? = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

    // Data that will be read from the outside.
    override var mDataHandler :  ( (ROSMessage<Vector3>) -> Unit )? = null
    private var mReading = Vector3(Double.NaN,Double.NaN,Double.NaN)

    // Android's listener interface for sensors.
    private val mGyroscopeListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {

            mReading = Vector3( event.values[0].toDouble(),
                event.values[1].toDouble(),
                event.values[2].toDouble() )

            mDataHandler?.invoke( read() )
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    init {
        Log.println(Log.INFO, "ROSGyroscope", mSensorManager.getSensorList(Sensor.TYPE_GYROSCOPE).toString())

        mSensorManager.registerListener(
            mGyroscopeListener,
            mGyroscope,
            SensorManager.SENSOR_DELAY_NORMAL
        )
    }

    override fun read(): ROSMessage<Vector3> {
        return ROSMessage( type = mMessageTypeName,
                           topic = mMessageTopicName,
                           msg = mReading )
    }

}