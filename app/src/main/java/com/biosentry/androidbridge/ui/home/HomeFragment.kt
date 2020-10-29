package com.biosentry.androidbridge.ui.home


import android.os.Bundle

import android.view.*

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import com.biosentry.androidbridge.MainActivity
import com.biosentry.androidbridge.R

import kotlinx.android.synthetic.main.fragment_home.*

class HomeFragment : Fragment() {

    private lateinit var homeViewModel: HomeViewModel
    private var mActivity : MainActivity? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        homeViewModel = ViewModelProviders.of(this).get(HomeViewModel::class.java)

        return  inflater.inflate(R.layout.fragment_home, container, false)

    }

    override fun onResume()
    {
        mActivity = activity as MainActivity

        BT_Websocket_Connect.setOnClickListener     { mActivity!!.connectClicked() }
        BT_Websocket_Disconnect.setOnClickListener  { mActivity!!.disconnectClicked() }

        super.onResume()
    }



}