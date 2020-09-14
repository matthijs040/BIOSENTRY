package com.example.test.ui.gallery

import android.app.Activity
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
import kotlinx.android.synthetic.main.fragment_gallery.*
import java.lang.NullPointerException
import java.util.*

class GalleryFragment : Fragment() {

    private lateinit var galleryViewModel: GalleryViewModel
    private var sensors : Sensors? = null
    private val timer = Timer()

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?  ): View?
    {
        galleryViewModel =
                ViewModelProviders.of(this).get(GalleryViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_gallery, container, false)

        sensors = Sensors(
            context = activity!!.baseContext,
            activity = activity as Activity
        )

        val timerTaskObj: TimerTask = object : TimerTask() {

            override fun run() {
                refreshSensorReadings()
            }
        }
        timer.schedule(timerTaskObj, 0, 200)

        return root
    }

    override fun onDestroy() {
        timer.cancel()
        timer.purge()
        super.onDestroy()
    }

    fun refreshSensorReadings()
    {
        val newReadings = sensors?.read()
        if(newReadings?.haveChanged == true)
            this.activity?.runOnUiThread(){
                val frag : Fragment? = this

                try {
                    frag?.TVLongitude?.text = newReadings.LocationLongitude.toString()
                    frag?.TVLatitude?.text = newReadings.LocationLatitude.toString()

                    frag?.TVAccelX?.text = newReadings.AccelerationX.toString()
                    frag?.TVAccelY?.text = newReadings.AccelerationY.toString()
                    frag?.TVAccelZ?.text = newReadings.AccelerationZ.toString()

                    frag?.TVRoll?.text = newReadings.RotationX.toString()
                    frag?.TVPitch?.text = newReadings.RotationY.toString()
                    frag?.TVYaw?.text = newReadings.RotationZ.toString()
                }
                catch (ex : NullPointerException)
                {
                    // Do nothing. UI elements did not exist anymore at time of writing to them.
                    // This function is run asynchronously so this is an expected exception.
                }

            }
    }
}