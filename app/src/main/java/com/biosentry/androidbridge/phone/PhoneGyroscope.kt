package com.biosentry.androidbridge.phone

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import com.biosentry.androidbridge.communication.*

/**
 * Class containing logic to fetch gyroscope data from Android's native HAL.
 */
class PhoneGyroscope(context: Context ) : ROSGyroscope("/android/phone/gyroscope")
{
    // Android HAL objects exposing the phone's sensors.
    private val mSensorManager: SensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private var mGyroscope: Sensor? = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

    // Android's listener interface for sensors.
    private val mGyroscopeListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {

            this@PhoneGyroscope.updateData( Point( event.values[0].toDouble(),
                                                   event.values[1].toDouble(),
                                                   event.values[2].toDouble() ) )
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


}