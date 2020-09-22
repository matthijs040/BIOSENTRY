package com.example.test.ui.gallery

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.example.test.*
import kotlinx.android.synthetic.main.fragment_gallery.*
import java.lang.NullPointerException
import java.util.*

class GalleryFragment : Fragment() {

    private lateinit var galleryViewModel: GalleryViewModel
    private var mROSBridge : ROSBridge? = null
    var mSensors : Sensors? = null
    var mSending : Boolean = false

    var mSendOnce : Boolean = false
    private val timer = Timer()

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?  ): View?
    {
        galleryViewModel = ViewModelProviders.of(this).get(GalleryViewModel::class.java)

        return  inflater.inflate(R.layout.fragment_gallery, container, false)
    }

    override fun onResume() {
        mSensors = Sensors(
            context = activity!!.baseContext,
            activity = activity as Activity
        )

        BT_send_data_cont.setOnClickListener{
            mSending = !mSending
            TV_sending_data.text = mSending.toString()
        }

        BT_send_data_once.setOnClickListener{
            mSendOnce = !mSendOnce
            TV_send_once.text = mSendOnce.toString()
        }

        val timerTaskObj = object : TimerTask() {
            override fun run() {
                refreshSensorReadings()
            }
        }
        timer.schedule(timerTaskObj, 0, 200)

        super.onResume()
    }

    override fun onDestroy() {
        timer.cancel()
        timer.purge()
        super.onDestroy()
    }

    fun refreshSensorReadings()
    {
        val newReadings = mSensors?.read()
        if(newReadings?.mHaveChanged == true)
            this.activity?.runOnUiThread(){
                val frag : Fragment? = this

                try {
                    frag?.TVLongitude?.text = newReadings.mLocationLongitude.toString()
                    frag?.TVLatitude?.text = newReadings.mLocationLatitude.toString()

                    frag?.TVAccelX?.text = newReadings.mAccelerationX.toString()
                    frag?.TVAccelY?.text = newReadings.mAccelerationY.toString()
                    frag?.TVAccelZ?.text = newReadings.mAccelerationZ.toString()

                    frag?.TVRoll?.text = newReadings.mRotationX.toString()
                    frag?.TVPitch?.text = newReadings.mRotationY.toString()
                    frag?.TVYaw?.text = newReadings.mRotationZ.toString()
                }
                catch (ex : NullPointerException)
                {
                    // Do nothing. UI elements did not exist anymore at time of writing to them.
                    // This function is run asynchronously so this is an expected exception.
                }
                sendSensorReadings(newReadings)
            }


    }

    fun sendSensorReadings(readings : SensorReadings?)
    {
        if(mSending)
            if(activity is MainActivity && readings != null)
            {
                (activity as MainActivity).sendData(readings)
                if(mSendOnce)
                {
                    mSending = false
                    TV_sending_data.text = mSending.toString()
                }

            }

    }
}