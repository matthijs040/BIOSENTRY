package com.example.biosentry.ui.camera

import android.graphics.SurfaceTexture
import android.media.Image
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import com.example.biosentry.MainActivity
import com.example.biosentry.R
import kotlinx.android.synthetic.main.camera_fragment.*

class CameraFragment : Fragment() {

    companion object {
        fun newInstance() = CameraFragment()
    }

    private val mOutput = camera_output

    private lateinit var viewModel: CameraViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.camera_fragment, container, false)
    }

    override fun onResume() {

        btn_takepicture?.setOnClickListener{ (activity as MainActivity?)?.mROSCamera?.takePicture() }
        (activity as MainActivity?)?.mROSCamera?.mImageHandler = ::showImageOnTexture

        super.onResume()
    }

    private fun showImageOnTexture(image : Image)
    {
        mOutput.apply { image.cropRect }
    }

}