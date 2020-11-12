package com.biosentry.androidbridge.phone

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import com.biosentry.androidbridge.communication.*

/**
 * Class containing logic to fetch accelerometer data from Android's native HAL.
 */
class PhoneAccelerometer(context: Context) : ROSAccelerometer("/android/phone/accelerometer")
{

    // Android HAL objects exposing the phone's sensors.
    private val mSensorManager: SensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private var mAccelerometer: Sensor? = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)


    // Android's listener interface for sensors.
    private val mAccelerometerListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {

            updateData( Vector3( event.values[0].toDouble(),
                                event.values[1].toDouble(),
                                event.values[2].toDouble() ) )
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }



    init {
        Log.println(Log.INFO, "ROSAccelerometer", mSensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER).toString())

        mSensorManager.registerListener(
            mAccelerometerListener,
            mAccelerometer,
            SensorManager.SENSOR_DELAY_NORMAL
        )
    }
}