package com.biosentry.androidbridge.aircraft

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.SurfaceTexture
import android.os.Looper
import android.util.Log
import android.view.TextureView
import dji.sdk.camera.Camera
import dji.sdk.camera.VideoFeeder
import dji.sdk.codec.DJICodecManager
import dji.sdk.products.Aircraft
import dji.sdk.sdkmanager.DJISDKManager


class AircraftCamera(private val act: Activity) : TextureView.SurfaceTextureListener {

    private var mCodecManager : DJICodecManager? = null
    var mBitmapHandler : ((Bitmap) -> Unit)? = null

    private val mVideoDataListener = VideoFeeder.VideoDataListener {
            p0, p1 ->
        mCodecManager?.sendDataToDecoder(p0, p1)
    }

    private val mBitmapListener = DJICodecManager.OnGetBitmapListener { p0 ->
        if(p0 != null && mBitmapHandler != null)
            mBitmapHandler!!.invoke(p0)
    }

    init {
        val product = DJISDKManager.getInstance().product
        if(product !is Aircraft || !product.isConnected)
            throw Exception("Valid Aircraft required for initialization")
        else {
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
           Looper.prepare()
           mCodecManager = DJICodecManager(act.applicationContext, surface, width, height )
           startRecording()
       }
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
        if (mCodecManager == null) {
            mCodecManager = DJICodecManager(act.applicationContext, surface, width, height)
            startRecording()
        }
        else
        { mCodecManager!!.cleanSurface() }
        Log.e("AircraftCamera", "onSurfaceTextureSizeChanged")
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
    {}

    // -------------------------------------------- SURFACE TEXTURE CALLBACKS. -------------------


}