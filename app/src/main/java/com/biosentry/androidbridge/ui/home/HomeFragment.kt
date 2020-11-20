package com.biosentry.androidbridge.ui.home


import android.os.Build
import android.os.Bundle

import android.view.*
import androidx.annotation.RequiresApi

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.biosentry.androidbridge.MainActivity
import com.biosentry.androidbridge.R

import kotlinx.android.synthetic.main.fragment_home.*

@RequiresApi(Build.VERSION_CODES.M)
class HomeFragment : Fragment() {

    private lateinit var homeViewModel: HomeViewModel
    private var mActivity : MainActivity? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        homeViewModel = ViewModelProvider(this).get(HomeViewModel::class.java)

        return  inflater.inflate(R.layout.fragment_home, container, false)

    }


    override fun onResume()
    {
        mActivity = activity as MainActivity

        //Setup event handlers for buttons.
        BT_Websocket_Connect.setOnClickListener     { mActivity!!.WebSocketConnectClicked() }
        BT_Websocket_Disconnect.setOnClickListener  { mActivity!!.WebSocketDisconnectClicked() }

        BT_RTMP_connect.setOnClickListener          { mActivity!!.RTMPConnectClicked() }
        BT_RTMP_disconnect.setOnClickListener       { mActivity!!.RTMPDisconnectClicked() }

        super.onResume()
    }



}