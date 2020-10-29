package com.biosentry.androidbridge.ui.sensors

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.biosentry.androidbridge.R
import com.biosentry.androidbridge.phone.Sensors
import kotlinx.android.synthetic.main.fragment_sensors.*
import java.util.*


class SensorsFragment : Fragment() {

    private lateinit var sensorsViewModel: SensorsViewModel
    private var mSending : Boolean = false
    private var mSendOnce : Boolean = false

    private var mSensors : Sensors? = null
    private val mTimer : Timer = Timer()

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?  ): View?
    {
        sensorsViewModel = ViewModelProvider(this).get(SensorsViewModel::class.java)

        return  inflater.inflate(R.layout.fragment_sensors, container, false)
    }

    override fun onResume() {
        BT_send_data_cont.setOnClickListener{
            mSending = !mSending
            TV_sending_data.text = mSending.toString()
        }

        BT_send_data_once.setOnClickListener{
            mSendOnce = !mSendOnce
            TV_send_once.text = mSendOnce.toString()
        }

        mSensors = Sensors(
            context = requireActivity().baseContext,
            activity = requireActivity()
        )

        mTimer.schedule(
            object : TimerTask()
            {
                override fun run() {
                    getData()
                }
            },
            0,
            1000
        )

        super.onResume()
    }

    fun getData()
    {
        activity?.runOnUiThread()
        {
            try {

                // ----------------- WRITE METADATA
                this.TV_accelerometer_name?.text = mSensors?.mAccelerometer?.name
                this.TV_accelerometer_vendor?.text = mSensors?.mAccelerometer?.vendor
                this.TV_accelerometer_resolution?.text = mSensors?.mAccelerometer?.resolution.toString()

                this.TV_gyroscope_name2?.text = mSensors?.mGyroscope?.name
                this.TV_gyroscope_vendor2?.text = mSensors?.mGyroscope?.vendor
                // frag?.TV_gyroscope_minDelay?.text = mSensors?.mGyroscope?.minDelay.toString()
                this.TV_gyroscope_resolution2?.text = mSensors?.mGyroscope?.resolution.toString()
                // frag?.TV_gyroscope_range?.text = mSensors?.mGyroscope?.maximumRange.toString()

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    this.TV_GPS_name3?.text = mSensors?.mLocationManager?.gnssHardwareModelName

                }
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                    this.TV_GPS_vendor3?.text = mSensors?.mLocationManager?.gnssCapabilities.toString()
                }

                // -------------------------  WRITE SENSOR READINGS

                val readings = mSensors?.read()

                this.TVLatitude.text = readings?.mLocationLatitude?.round(3)
                this.TVLongitude.text = readings?.mLocationLongitude?.round(3)
                this.TV_altitude.text = readings?.mAltitude?.round(3)

                this.TVAccelX.text = readings?.mAccelerationX?.round(3)
                this.TVAccelY.text = readings?.mAccelerationY?.round(3)
                this.TVAccelZ.text = readings?.mAccelerationZ?.round(3)

                this.TVRoll.text = readings?.mRotationX?.round(3)
                this.TVPitch.text = readings?.mRotationY?.round(3)
                this.TVYaw.text = readings?.mRotationZ?.round(3)


            }
            catch ( ex : NullPointerException)
            {
                // Do nothing. This exception happens when a switch in UI is made while changes are being written to it.
                // Even if safe calls are made to them. It will not break the application to silently fail.
            }
        }
    }

    override fun onDestroy() {
        mTimer.cancel()
        mTimer.purge()
        super.onDestroy()
    }

    // From: https://discuss.kotlinlang.org/t/how-do-you-round-a-number-to-n-decimal-places/8843
    private fun Double.round(decimals: Int = 2): String = "%.${decimals}f".format(this)
}
