package com.biosentry.androidbridge.aircraft

import android.app.Activity
import android.graphics.SurfaceTexture
import android.util.Log
import android.view.TextureView
import dji.sdk.camera.Camera
import dji.sdk.camera.VideoFeeder
import dji.sdk.codec.DJICodecManager
import dji.sdk.products.Aircraft
import dji.sdk.sdkmanager.DJISDKManager
import java.util.*

class AircraftCamera(private val act: Activity) : TextureView.SurfaceTextureListener {

    private var mCodecManager : DJICodecManager? = null

    private val mVideoDataListener = VideoFeeder.VideoDataListener {
            p0, p1 -> mCodecManager?.sendDataToDecoder(p0, p1) }


    private val mTimer = Timer()

    init {
        val product = DJISDKManager.getInstance().product
        if(product !is Aircraft || !product.isConnected)
            throw Exception("Valid Aircraft required for initialization")
        else
        {
           VideoFeeder.getInstance().primaryVideoFeed.addVideoDataListener(mVideoDataListener)

        }
    }

    private fun startRecording()
    {
        val camera = DJISDKManager.getInstance().product
        if(camera is Camera)
        {
            camera.startRecordVideo {
                it?.let { Log.d(this.javaClass.simpleName.toString(), it.description) }
            }
        }
    }

    private fun stopRecording()
    {
        val camera = DJISDKManager.getInstance().product
        if(camera is Camera)
        {
            camera.stopRecordVideo {
                it?.let { Log.d(this.javaClass.simpleName.toString(), it.description) }
            }
        }
    }

    // SURFACE TEXTURE CALLBACKS FOR RENDERING / INITIALIZING THE DJI CODEC MANAGER OBJECT.
    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
        if (mCodecManager == null) {
            mCodecManager = DJICodecManager(act.applicationContext, surface, width, height)
            startRecording()
        }
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
        Log.e("AircraftCamera", "onSurfaceTextureSizeChanged");
    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        stopRecording()
        if (mCodecManager != null) {
            mCodecManager!!.cleanSurface()
            mCodecManager = null
        }
        return false
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture)
    {
       // if (mCodecManager == null) {
       //     mCodecManager = DJICodecManager(act.applicationContext, surface)
       //     startRecording()
       // }
    }

    // -------------------------------------------- SURFACE TEXTURE CALLBACKS. -------------------


}