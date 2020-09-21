package com.example.test.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import com.example.test.MainActivity
import com.example.test.R

class HomeFragment : Fragment() {

    private lateinit var homeViewModel: HomeViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View?
    {
        homeViewModel =
                ViewModelProviders.of(this).get(HomeViewModel::class.java)

        val root = inflater.inflate(R.layout.fragment_home, container, false)

        val button : Button = root.findViewById(R.id.BT_Websocket_Connect)
        button.setOnClickListener { (activity as MainActivity?)?.connectClicked() }

        val disconnectButton : Button = root.findViewById(R.id.BT_Websocket_Disconnect)
        disconnectButton.setOnClickListener { (activity as MainActivity?)?.disconnectClicked() }

        return root

    }
}