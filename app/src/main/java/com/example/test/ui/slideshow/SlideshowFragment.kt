package com.example.test.ui.slideshow

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
import kotlinx.android.synthetic.main.fragment_slideshow.*
import java.util.*

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

        val root = inflater.inflate(R.layout.fragment_gallery, container, false)

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
        activity?.runOnUiThread() {
            val frag : Fragment? = this
            frag?.TV_accelerometer_name?.text = sensors?.accelerometer?.name

        }
    }
}