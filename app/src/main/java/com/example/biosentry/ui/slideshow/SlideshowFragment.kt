package com.example.biosentry.ui.slideshow

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import com.example.biosentry.R
import com.example.biosentry.Sensors
import kotlinx.android.synthetic.main.fragment_slideshow.*
import java.util.*

class SlideshowFragment : Fragment() {

    private lateinit var slideshowViewModel: SlideshowViewModel

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        slideshowViewModel =
                ViewModelProviders.of(this).get(SlideshowViewModel::class.java)

        return inflater.inflate(R.layout.fragment_slideshow, container, false)

    }

    override fun onDestroy() {

        super.onDestroy()
    }

}