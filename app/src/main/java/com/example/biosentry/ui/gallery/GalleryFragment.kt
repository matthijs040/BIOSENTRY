package com.example.biosentry.ui.gallery

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import com.example.biosentry.*
import kotlinx.android.synthetic.main.fragment_gallery.*
import java.lang.NullPointerException
import java.util.*

class GalleryFragment : Fragment() {

    private lateinit var galleryViewModel: GalleryViewModel
    private var mSending : Boolean = false
    private var mSendOnce : Boolean = false

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?  ): View?
    {
        galleryViewModel = ViewModelProviders.of(this).get(GalleryViewModel::class.java)

        return  inflater.inflate(R.layout.fragment_gallery, container, false)
    }

    override fun onResume() {
        BT_send_data_cont.setOnClickListener{
            mSending = !mSending
            TV_sending_data.text = mSending.toString()
        }

        BT_send_data_once.setOnClickListener{
            mSendOnce = !mSendOnce
            TV_send_once.text = mSendOnce.toString()
        }
                super.onResume()
    }

    override fun onDestroy() {
        super.onDestroy()
    }    

}