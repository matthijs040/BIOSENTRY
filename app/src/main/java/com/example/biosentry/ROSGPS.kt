package com.example.biosentry

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
import com.example.biosentry.NavSatStatus.Companion.SERVICE_GLONASS
import com.example.biosentry.NavSatStatus.Companion.SERVICE_GPS
import com.example.biosentry.NavSatStatus.Companion.STATUS_FIX
import com.example.biosentry.NavSatStatus.Companion.STATUS_NO_FIX

class ROSGPS(context: Context, activity: Activity) : IROSSensor<NavSatFix>
{
    private var mLocationManager: LocationManager = context.getSystemService(AppCompatActivity.LOCATION_SERVICE) as LocationManager

    // Data that will be read from the outside.
    override var mDataHandler :  ( (ROSMessage<NavSatFix>) -> Unit )? = null

    private var mStatus = NavSatStatus(
        STATUS_NO_FIX,
        SERVICE_GLONASS + SERVICE_GPS
    )

    private var  mReading : NavSatFix = NavSatFix ( mStatus,
                                                    Double.NaN,
                                                    Double.NaN,
                                                    Double.NaN,
                                                    DoubleArray(0),
                                                    0 )

    override val mMessageTypeName: String
        get() = "sensor_msgs/NavSatFix"
    override val mMessageTopicName: String
        get() = "bridge/android/gps"

    private val mLocationListener: LocationListener = object : LocationListener {

        override fun onLocationChanged(location: Location) {
            mStatus = NavSatStatus(status = STATUS_FIX, service = SERVICE_GLONASS + SERVICE_GPS)

            mReading = NavSatFix(
                mStatus,
                latitude = location.latitude,
                longitude = location.longitude,
                altitude = location.altitude,
                position_covariance = DoubleArray(0),
                position_covariance_type = 0
            )

            mDataHandler?.invoke( read() )
        }

        override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }

    override fun read(): ROSMessage<NavSatFix> {
            return ROSMessage(
                topic = mMessageTopicName,
                type = mMessageTypeName,
                msg = mReading
            )
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