package com.biosentry.androidbridge.aircraft

import android.app.Activity
import android.graphics.BitmapFactory
import android.graphics.SurfaceTexture
import android.media.MediaCodec
import android.media.MediaCodecList
import android.media.MediaFormat
import android.media.MediaRecorder
import android.os.Build
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder
import android.view.TextureView
import androidx.annotation.RequiresApi
import dji.sdk.camera.Camera
import dji.sdk.camera.VideoFeeder
import dji.sdk.codec.DJICodecManager
import dji.sdk.products.Aircraft
import dji.sdk.sdkmanager.DJISDKManager
import dji.sdk.sdkmanager.LiveStreamManager
import dji.sdk.sdkmanager.LiveStreamManager.LiveStreamVideoSource.Primary
import net.butterflytv.rtmp_client.RTMPMuxer
import net.butterflytv.rtmp_client.RtmpClient


@RequiresApi(Build.VERSION_CODES.M)
class AircraftCamera(private val act: Activity) : TextureView.SurfaceTextureListener {


    private var dummyTexture : SurfaceTexture = SurfaceTexture(0)
   // private val mSurface : Surface = Surface(dummyTexture)


   // private val mMediaCodec = MediaCodecList(MediaCodecList.ALL_CODECS).findEncoderForFormat(
   //     MediaFormat.createVideoFormat())


    var mCodecManager : DJICodecManager = DJICodecManager(act.baseContext, dummyTexture , 640, 480 )
    private val mRtmpClient : RTMPMuxer = RTMPMuxer()

private fun sendData( data : ByteArray, size : Int)
{
    if( mRtmpClient.writeVideo(data, 0, size, System.currentTimeMillis()) == 1)
        Log.w(this.javaClass.simpleName, "Transmission failed")
}

    // THIS BYTE BUFFER IS H264 ENCODED
    private val mVideoDataListener = VideoFeeder.VideoDataListener { p0, p1 ->


        if(mRtmpClient.isConnected)
        {
            println("raw data array size: " + p0.size)
            sendData(p0, p1)
        }


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

    fun startAntStream(rtmpURL: String) : Int
    {
        if(!mRtmpClient.isConnected)
            return mRtmpClient.open(rtmpURL, 640, 480 )
        return 0 //Already connected. report success?
    }

    fun stopAntStream()
    {
        if(mRtmpClient.isConnected)
            mRtmpClient.close()
    }

    // from: https://stackoverflow.com/questions/55731431/getting-livestreammanager-error-3-in-dji-mobile-sdk-when-trying-to-stream-to-cu
    fun startDJIStream(rtmpURL: String) : Int
    {
        val liveStreamManager = DJISDKManager.getInstance().liveStreamManager
        liveStreamManager!!.setVideoSource(Primary)
        liveStreamManager.isVideoEncodingEnabled = true
        liveStreamManager.liveUrl = rtmpURL
        liveStreamManager.setAudioStreamingEnabled(true)



        return liveStreamManager.startStream()
    }

    fun stopDJIStream()
    {
        DJISDKManager.getInstance().liveStreamManager.stopStream()
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
        //mCodecManager?.getBitmap(mBitmapListener)

        Log.e("AircraftCamera", "onSurfaceTextureSizeChanged")
    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        //stopRecording()
        mCodecManager.cleanSurface()
        dummyTexture = SurfaceTexture(1) // re-init texture
        mCodecManager = DJICodecManager(act.baseContext, dummyTexture , 1280, 720 )

        return false
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture)
    {}

    // -------------------------------------------- SURFACE TEXTURE CALLBACKS. -------------------


}