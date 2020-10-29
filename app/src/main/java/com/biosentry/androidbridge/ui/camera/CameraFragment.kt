package com.biosentry.androidbridge.ui.camera

import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import com.biosentry.androidbridge.MainActivity
import com.biosentry.androidbridge.R
import kotlinx.android.synthetic.main.camera_fragment.*


class CameraFragment : Fragment() {

    private lateinit var mOutput : ImageView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.camera_fragment, container, false)
    }

    override fun onResume() {

        mOutput = this.activity!!.findViewById(R.id.camera_image_view)
        btn_takepicture?.setOnClickListener{ (activity as MainActivity?)?.mROSCamera?.takePicture() }

        (activity as MainActivity?)?.mROSCamera?.mBitmapHandler = ::showBitmap

        super.onResume()
    }

    private fun showBitmap(bitmap: Bitmap)
    {
        mOutput.setImageBitmap(bitmap)
    }



}