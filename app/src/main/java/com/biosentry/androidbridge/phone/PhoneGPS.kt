package com.biosentry.androidbridge.phone

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.biosentry.androidbridge.communication.*
import com.biosentry.androidbridge.communication.NavSatStatus.Companion.SERVICE_GLONASS
import com.biosentry.androidbridge.communication.NavSatStatus.Companion.SERVICE_GPS
import com.biosentry.androidbridge.communication.NavSatStatus.Companion.STATUS_FIX
import com.biosentry.androidbridge.communication.NavSatStatus.Companion.STATUS_NO_FIX

class PhoneGPS(context: Context, activity: Activity) : ROSGPS("/android/phone/GPS")
{
    private var mLocationManager: LocationManager = context.getSystemService(AppCompatActivity.LOCATION_SERVICE) as LocationManager

    private val mLocationListener: LocationListener = object : LocationListener {

        override fun onLocationChanged(location: Location) {

             updateData( NavSatFix(
                this@PhoneGPS.mHeader,
                this@PhoneGPS.mStatus,
                latitude = location.latitude,
                longitude = location.longitude,
                altitude = location.altitude,
                position_covariance = DoubleArray(9),
                position_covariance_type = 0
                )
             )
        }

        override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }




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

    private fun initLocationListener() {
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

    init {
        askForPermissions(context, activity)
        initLocationListener()
    }
}