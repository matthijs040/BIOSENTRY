package com.biosentry.androidbridge.phone

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.graphics.PixelFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.hardware.camera2.CameraDevice.TEMPLATE_RECORD
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.RecommendedStreamConfigurationMap
import android.hardware.camera2.params.SessionConfiguration
import android.media.MediaCodec
import android.media.MediaCodec.createEncoderByType
import android.media.MediaCodecList
import android.media.MediaFormat
import android.util.Log
import android.util.Size
import android.view.Surface
import android.view.SurfaceControl
import androidx.core.app.ActivityCompat
import com.biosentry.androidbridge.communication.CompressedImage
import com.biosentry.androidbridge.communication.Header
import com.biosentry.androidbridge.communication.ROSCamera
import com.biosentry.androidbridge.communication.time

/**
 * Can provide optional camera. Will select a rear-facing camera by default.
 */
@SuppressLint("NewApi", "InlinedApi")
class PhoneCamera(private val mContext : Context, private val mActivity : Activity, private var cameraID : String = String()) : ROSCamera("android/phone/camera"){

    private val mCameraManager = mContext.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private val mSelectableCameras = mCameraManager.cameraIdList


    private val mVideoFormatMime : String = "image/jpeg"
    private val mMediaCodec = createEncoderByType(mVideoFormatMime)



    private val mUnderlyingSurface = Surface(SurfaceTexture(0))


    /**
     * The callback on which events about the real underlying, encoded data occur.
     * This is the callback that creates the byteBuffer that will be sent out.
     */
    private val mMediaCodecCallback = object : MediaCodec.Callback()
    {
        override fun onInputBufferAvailable(codec: MediaCodec, index: Int) { }

        override fun onOutputBufferAvailable(
            codec: MediaCodec,
            index: Int,
            info: MediaCodec.BufferInfo
        ) {
            Log.d(this.javaClass.simpleName, "onOutputBufferAvailable with info: $info")

            updateData(CompressedImage(
                header = Header(0L, time(System.currentTimeMillis() / 1000, System.currentTimeMillis() * 1000), "phone_camera"),
                format = "jpeg",
                data = mMediaCodec.getOutputBuffer(index)!!.array()
            ))


        }

        override fun onError(codec: MediaCodec, e: MediaCodec.CodecException) {
            TODO("Not yet implemented")
        }

        override fun onOutputFormatChanged(codec: MediaCodec, format: MediaFormat) {
            Log.w(this.javaClass.simpleName, "onOutputFormatChanged to: $format")
        }

    }

    private val mCameraCaptureSessionCaptureCallback = object : CameraCaptureSession.CaptureCallback()
    {
        override fun onCaptureCompleted(
            session: CameraCaptureSession,
            request: CaptureRequest,
            result: TotalCaptureResult
        ) {
            val data = result.physicalCameraResults


            super.onCaptureCompleted(session, request, result)
        }

        override fun onCaptureFailed(
            session: CameraCaptureSession,
            request: CaptureRequest,
            failure: CaptureFailure
        ) {
            Log.e(this.javaClass.simpleName, "onCaptureFailed")
            super.onCaptureFailed(session, request, failure)
        }
    }

    private val mCameraCaptureSessionStateCallback = object : CameraCaptureSession.StateCallback()
    {

        override fun onConfigured(session: CameraCaptureSession) {
            Log.d(this.javaClass.simpleName, "session is configured.")

            /**
             * Template record:
             * Create a request suitable for video recording.
             * Specifically, this means that a stable frame rate is used, and post-processing is set for recording quality.
             * These requests would commonly be used with the CameraCaptureSession#setRepeatingRequest method.
             */
            session.setRepeatingRequest(session.device.createCaptureRequest(TEMPLATE_RECORD).build(), mCameraCaptureSessionCaptureCallback, null )
        }

        override fun onConfigureFailed(session: CameraCaptureSession) {
            Log.e(this.javaClass.simpleName, "session configuration failed.")
        }

    }

    private val mSessionConfiguration : SessionConfiguration =
        SessionConfiguration(
            SessionConfiguration.SESSION_REGULAR,
            mutableListOf(),
            {  },
            mCameraCaptureSessionStateCallback )

    private val mCameraDeviceStateCallback = object : CameraDevice.StateCallback()  // https://developer.android.com/reference/android/hardware/camera2/CameraDevice
    {
        override fun onOpened(camera: CameraDevice)
        {
            val characteristics = mCameraManager.getCameraCharacteristics(camera.id)
            val recommendations = characteristics.getRecommendedStreamConfigurationMap(RecommendedStreamConfigurationMap.USECASE_RECORD)!!
            val availableSizes = recommendations.getOutputSizes(ImageFormat.JPEG)!!
            var sizeToUse = Size(0,0)

            availableSizes.forEach{
                if( ( it.width * it.height) > ( sizeToUse.width * sizeToUse.height) ) // Look for resolution with most pixels.
                {
                    sizeToUse = it
                }
            }

            mMediaCodec.configure( MediaFormat.createVideoFormat( mVideoFormatMime
                                                                , sizeToUse.width
                                                                , sizeToUse.height )
                                 , mUnderlyingSurface
                                 , 0
                                 ,null )

            mMediaCodec.setCallback(mMediaCodecCallback)

            mSessionConfiguration.outputConfigurations.add(OutputConfiguration(mMediaCodec.createInputSurface()))

            mMediaCodec.start()

            camera.createCaptureSession (mSessionConfiguration)
        }

        override fun onDisconnected(camera: CameraDevice) {
           camera.close() // How to stop CaptureSession? close?
        }

        override fun onError(camera: CameraDevice, error: Int) {
            Log.e(this.javaClass.simpleName, "onError code: $error")
        }

    }

    private fun askForPermissions()
    {
        if (ActivityCompat.checkSelfPermission(
                mContext,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                mActivity,
                arrayOf(Manifest.permission.CAMERA),
                50
            )
        }
    }



    init {
        askForPermissions()
        if(!mSelectableCameras.contains(cameraID))
            cameraID = mSelectableCameras.first()

        try
        {
            mCameraManager.openCamera(cameraID, mCameraDeviceStateCallback, null)   // Uses current thread if null. Might make UI thread hang???
        }
        catch (ex: SecurityException) {
            Log.e("PhoneCamera", "Android system is rude. No permission for camera" )
        }

    }
}