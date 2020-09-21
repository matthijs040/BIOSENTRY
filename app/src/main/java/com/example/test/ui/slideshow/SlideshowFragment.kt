package com.example.test.ui.slideshow

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.example.test.R
import com.example.test.Sensors
import kotlinx.android.synthetic.main.fragment_slideshow.*
import java.util.*

import com.example.test.MainActivity

class SlideshowFragment : Fragment() {

    private lateinit var slideshowViewModel: SlideshowViewModel

    private var sensors : Sensors? = null
    private val timer = Timer()

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        slideshowViewModel =
                ViewModelProviders.of(this).get(SlideshowViewModel::class.java)

        val root = inflater.inflate(R.layout.fragment_slideshow, container, false)

        sensors = Sensors(
            context = activity!!.baseContext,
            activity = activity as Activity
        )

        val timerTaskObj: TimerTask = object : TimerTask() {
            override fun run() {
                getSensorMetadata()
            }
        }
        timer.schedule(timerTaskObj, 0, 1000)

        return root
    }

    override fun onDestroy() {
        timer.cancel()
        timer.purge()
        super.onDestroy()
    }

    fun getSensorMetadata()
    {
        activity?.runOnUiThread()
        {
            try {
                val frag : Fragment? = this
                frag?.TV_accelerometer_name?.text = sensors?.mAccelerometer?.name
                frag?.TV_accelerometer_vendor?.text = sensors?.mAccelerometer?.vendor
                frag?.TV_accelerometer_resolution?.text = sensors?.mAccelerometer?.resolution.toString()
                frag?.TV_accelerometer_minDelay?.text = sensors?.mAccelerometer?.minDelay.toString()
                frag?.TV_accelerometer_range?.text = sensors?.mAccelerometer?.maximumRange.toString()

                frag?.TV_gyroscope_name?.text = sensors?.mGyroscope?.name
                frag?.TV_gyroscope_vendor?.text = sensors?.mGyroscope?.vendor
                frag?.TV_gyroscope_minDelay?.text = sensors?.mGyroscope?.minDelay.toString()
                frag?.TV_gyroscope_resolution?.text = sensors?.mGyroscope?.resolution.toString()
                frag?.TV_gyroscope_range?.text = sensors?.mGyroscope?.maximumRange.toString()

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    frag?.TV_GPS_name?.text = sensors?.mLocationManager?.gnssHardwareModelName

                }
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                    frag?.TV_GPS_vendor?.text = sensors?.mLocationManager?.gnssCapabilities.toString()
                }

            }
            catch ( ex : NullPointerException)
            {
                // Do nothing. This exception happens when a switch in UI is made while changes are being written to it.
                // Even if safe calls are made to them. It will not break the application to silently fail.
            }
        }
    }
}