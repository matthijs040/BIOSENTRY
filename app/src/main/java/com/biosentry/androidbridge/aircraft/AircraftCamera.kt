package com.biosentry.androidbridge.aircraft

import android.app.Activity
import android.graphics.SurfaceTexture
import android.os.Build
import android.os.Looper
import android.util.Log
import android.view.TextureView
import androidx.annotation.RequiresApi
import dji.sdk.camera.Camera
import dji.sdk.camera.VideoFeeder
import dji.sdk.codec.DJICodecManager
import dji.sdk.products.Aircraft
import dji.sdk.sdkmanager.DJISDKManager
import dji.sdk.sdkmanager.LiveStreamManager.LiveStreamVideoSource.Primary
import java.util.*
import kotlin.concurrent.timerTask


@RequiresApi(Build.VERSION_CODES.M)
class AircraftCamera(private val act: Activity) : TextureView.SurfaceTextureListener {


    private var dummyTexture : SurfaceTexture = SurfaceTexture(0)
    var mCodecManager : DJICodecManager = DJICodecManager(act.baseContext, dummyTexture , 640, 480 )
    private val mTimer = Timer()

    // THIS BYTE BUFFER IS H264 ENCODED
    private val mVideoDataListener = VideoFeeder.VideoDataListener { p0, p1 ->

        mCodecManager.sendDataToDecoder(p0, p1)

    }

    init {
        Looper.myLooper() ?: Looper.prepare()
        val product = DJISDKManager.getInstance().product
        if(product !is Aircraft || !product.isConnected)
            throw Exception("Valid Aircraft required for initialization")
        else {
            VideoFeeder.getInstance().primaryVideoFeed.addVideoDataListener(mVideoDataListener)
        }
    }

    // from: https://stackoverflow.com/questions/55731431/getting-livestreammanager-error-3-in-dji-mobile-sdk-when-trying-to-stream-to-cu
    fun startDJIStream(rtmpURL: String) : Int
    {
        val liveStreamManager = DJISDKManager.getInstance().liveStreamManager
        liveStreamManager!!.setVideoSource(Primary)
        liveStreamManager.isVideoEncodingEnabled = true
        liveStreamManager.liveUrl = rtmpURL
        liveStreamManager.setAudioStreamingEnabled(true)

        val ret = liveStreamManager.startStream()
        if(ret == 0)
            mTimer.schedule(
                timerTask {
                    Log.i( this.javaClass.simpleName, "video bitrate: " + liveStreamManager.liveVideoBitRate)
                    Log.i( this.javaClass.simpleName, "video frame-rate" + liveStreamManager.liveVideoFps)
                }, 1000, 1000
            )

        return ret
    }

    fun stopDJIStream()
    {
        DJISDKManager.getInstance().liveStreamManager.stopStream()
        mTimer.cancel()
        mTimer.purge()
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

        Looper.myLooper() ?: Looper.prepare()
        mCodecManager = DJICodecManager(act.applicationContext, surface, 1280, 720)
        startRecording()
       
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int)
    {
        mCodecManager = DJICodecManager(act.applicationContext, surface, width, height)
        Log.w(this.javaClass.simpleName, "onSurfaceTextureSizeChanged")
    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        mCodecManager.cleanSurface()
        dummyTexture = SurfaceTexture(1) // re-init texture
        mCodecManager = DJICodecManager(act.baseContext, dummyTexture , 1280, 720 )

        return false
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture)
    {}

    // -------------------------------------------- SURFACE TEXTURE CALLBACKS. -------------------


}