package com.example.test.ui.home

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import com.example.test.MainActivity
import com.example.test.R
import com.example.test.CameraHandler
import com.example.test.CameraPermissionHelper
import kotlinx.android.synthetic.main.fragment_home.*

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class HomeFragment : Fragment() {

    private lateinit var homeViewModel: HomeViewModel

    private lateinit var mCameraHandler : CameraHandler


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
            BT_Websocket_Connect.setOnClickListener     { (activity as MainActivity?)?.connectClicked() }
            BT_Websocket_Disconnect.setOnClickListener  { (activity as MainActivity?)?.disconnectClicked() }

        if (!CameraPermissionHelper.hasCameraPermission(activity!!.baseContext)) {
            CameraPermissionHelper.requestCameraPermission(activity as Activity )
            super.onResume()
        }

        mCameraHandler = CameraHandler(context = activity!!.baseContext,
            activity = activity as Activity )

        surfaceView.holder.addCallback(mCameraHandler.surfaceReadyCallback)

        mCameraHandler.mErrorHandler = ::writeCameraError

        super.onResume()
    }

    private fun writeCameraError(errorMessage: String)
    {
        TV_camera_error.text = errorMessage
    }

    private fun writeCameraStatus(statusMessage: String)
    {
        TV_camera_status.text = statusMessage
    }
}