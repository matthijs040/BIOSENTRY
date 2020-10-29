package com.biosentry.androidbridge.ui.aircraft

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.biosentry.androidbridge.R

class AircraftFragment : Fragment() {

    private lateinit var aircraftViewModel: AircraftViewModel

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        aircraftViewModel =
            ViewModelProvider(this).get(AircraftViewModel::class.java)

        return inflater.inflate(R.layout.fragment_aircraft, container, false)

    }

    override fun onDestroy() {

        super.onDestroy()
    }

}