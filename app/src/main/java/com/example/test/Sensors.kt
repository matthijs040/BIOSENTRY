package com.example.test

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
import androidx.core.content.ContextCompat.getSystemService


class SensorReadings {

    var haveChanged         : Boolean = false
    var LocationLatitude    : Double = Double.NaN
    var LocationLongitude   : Double = Double.NaN

    var AccelerationX       : Double = Double.NaN
    var AccelerationY       : Double = Double.NaN
    var AccelerationZ       : Double = Double.NaN

    var RotationX           : Double = Double.NaN
    var RotationY           : Double = Double.NaN
    var RotationZ           : Double = Double.NaN
}


/**
 * Class containing all sensors in the phone that can be read during runtime of the application.
 *
 */
class Sensors {

    // Sensors, listed as private variables.
    var locationManager: LocationManager? = null
    var sensorManager: SensorManager? = null

    var accelerometer: Sensor? = null
    var gyroscope: Sensor? = null
    var sensorReadings: SensorReadings = SensorReadings()

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
        // Construct a manager object to create a listener from.
        locationManager = context.getSystemService(AppCompatActivity.LOCATION_SERVICE) as LocationManager?

        // Try to construct a listener.
        try {
            // Request location updates
            locationManager?.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER,
                0L,
                0.0f,
                locationListener
            )
        } catch (ex: SecurityException) {
            Log.d("LocationManager", "Security Exception, no location available")
        }
    }

    private fun initSensorListeners(context: Context) {
        // First, init sensor manager to create sensors from.
        sensorManager = context.getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager!!.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        gyroscope = sensorManager!!.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

        // Register accelerometer event.
        sensorManager!!.registerListener(
            accelerometerListener,
            accelerometer,
            SensorManager.SENSOR_DELAY_NORMAL
        )

        sensorManager!!.registerListener(
            gyroscopeListener,
            gyroscope,
            SensorManager.SENSOR_DELAY_NORMAL
        )
    }

    constructor(context: Context, activity: Activity) {
        askForPermissions(context, activity)
        initLocationListener(context)
        initSensorListeners(context)
    }

    fun read(): SensorReadings {
        return sensorReadings
    }

    /**
     * Simple implementation of a locationListener that updates the location data in
     * SensorReadings every time a new location is received.
     */
    val locationListener: LocationListener = object : LocationListener {
        //Trigger on location change:
        override fun onLocationChanged(location: Location) {
            sensorReadings.LocationLatitude = location.latitude
            sensorReadings.LocationLongitude = location.longitude
            sensorReadings.haveChanged = true
        }

        //Empty events:
        override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }


    private val accelerometerListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            sensorReadings.AccelerationX = event.values[0].toDouble()
            sensorReadings.AccelerationY = event.values[1].toDouble()
            sensorReadings.AccelerationZ = event.values[2].toDouble()
            sensorReadings.haveChanged = true
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    private val gyroscopeListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            sensorReadings.RotationX = event.values[0].toDouble()
            sensorReadings.RotationY = event.values[0].toDouble()
            sensorReadings.RotationZ = event.values[0].toDouble()
            sensorReadings.haveChanged = true
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }
}