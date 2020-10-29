package com.biosentry.androidbridge

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Context.SENSOR_SERVICE
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.lang.Math.pow
import kotlin.math.pow

/**
 * Sensor data that changes at high frequency.
 * To be polled by outside classes.
 */
class SensorReadings {

    var mHaveChanged         : Boolean = false
    var mLocationLatitude    : Double = Double.NaN
    var mLocationLongitude   : Double = Double.NaN

    var mAccelerationX       : Double = Double.NaN
    var mAccelerationY       : Double = Double.NaN
    var mAccelerationZ       : Double = Double.NaN

    var mRotationX           : Double = Double.NaN
    var mRotationY           : Double = Double.NaN
    var mRotationZ           : Double = Double.NaN

    var mAltitude            : Double = Double.NaN
}


/**
 * Class containing all sensors in the phone that can be read during runtime of the application.
 *
 */
class Sensors(context: Context, activity: Activity) {

    // Sensors, listed as private variables.
    var mLocationManager: LocationManager = context.getSystemService(AppCompatActivity.LOCATION_SERVICE) as LocationManager
    var mSensorManager: SensorManager = context.getSystemService(SENSOR_SERVICE) as SensorManager

    var mAccelerometer: Sensor? =    mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    var mGyroscope: Sensor? =        mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    var mBarometer : Sensor? =       mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)
    var mSensorReadings: SensorReadings = SensorReadings()

    // Location data that changes at uncertain and lower frequency times.
    // To be worked with as new data comes in through this function.
    var mLocationChangedCallback : ( (altitude : Double, latitude : Double, longitude : Double) -> Unit )? = null
    var mAccelerometerCallback : ( ( X : Double, Y : Double, Z : Double ) -> Unit)? = null
    var mGyroscopeCallback :    ( ( R : Double, P : Double, Y : Double) -> Unit )?  = null

    private fun askForPermissions(context: Context, activity: Activity) {
        // Ask nicely for permissions to use the GPS.
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                0x0
            )
        }
    }

    private fun initLocationListener(context: Context) {

        // Try to construct a listener.
        try {
            // Request location updates
            mLocationManager.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER,
                0L,
                0.0f,
                mLocationListener
            )
        } catch (ex: SecurityException) {
            Log.d("LocationManager", "Security Exception, no location available")
        }
    }

    private fun initSensorListeners() {
        // Register accelerometer event.
        Log.println(Log.INFO, "Sensors", mSensorManager.getSensorList(Sensor.TYPE_ALL).toString())

        mSensorManager.registerListener(
            accelerometerListener,
            mAccelerometer,
            SensorManager.SENSOR_DELAY_NORMAL
        )

        mSensorManager.registerListener(
            gyroscopeListener,
            mGyroscope,
            SensorManager.SENSOR_DELAY_NORMAL
        )

        mSensorManager.registerListener(
            barometerListener,
            mBarometer,
            SensorManager.SENSOR_DELAY_NORMAL
        )
    }

    fun read(): SensorReadings {
        return mSensorReadings
    }

    /**
     * Simple implementation of a locationListener that updates the location data in
     * SensorReadings every time a new location is received.
     */
    private val mLocationListener: LocationListener = object : LocationListener {
        //Trigger on location change:
        override fun onLocationChanged(location: Location) {
            mSensorReadings.mLocationLatitude = location.latitude
            mSensorReadings.mLocationLongitude = location.longitude
            mSensorReadings.mAltitude = location.altitude
            mLocationChangedCallback?.invoke(location.altitude, location.latitude, location.longitude)
            mSensorReadings.mHaveChanged = true
        }

        //Empty events:
        override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }


    private val accelerometerListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            mSensorReadings.mAccelerationX = event.values[0].toDouble()
            mSensorReadings.mAccelerationY = event.values[1].toDouble()
            mSensorReadings.mAccelerationZ = event.values[2].toDouble()
            mAccelerometerCallback?.invoke(event.values[0].toDouble(), event.values[1].toDouble(), event.values[2].toDouble())
            mSensorReadings.mHaveChanged = true
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    private val gyroscopeListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            mSensorReadings.mRotationX = event.values[0].toDouble()
            mSensorReadings.mRotationY = event.values[0].toDouble()
            mSensorReadings.mRotationZ = event.values[0].toDouble()
            mGyroscopeCallback?.invoke(event.values[0].toDouble(), event.values[1].toDouble(), event.values[2].toDouble())
            mSensorReadings.mHaveChanged = true
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    private val barometerListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            mSensorReadings.mAltitude =
                145366 * ( 1 - (event.values[0].toDouble() / 1013.25).pow(0.190284) )
            mSensorReadings.mHaveChanged = true
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {  }
    }

    init {
        askForPermissions(context, activity)
        initLocationListener(context)
        initSensorListeners()
    }
}