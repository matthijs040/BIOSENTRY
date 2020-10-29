package com.example.biosentry.ui.home

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import com.example.biosentry.MainActivity
import com.example.biosentry.R
import com.example.biosentry.ROSMessage
import kotlinx.android.synthetic.main.camera_fragment.*

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