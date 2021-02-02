package com.biosentry.androidbridge.phone

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.hardware.HardwareBuffer
import android.hardware.camera2.*
import android.hardware.camera2.CameraDevice.TEMPLATE_RECORD
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import android.view.Surface
import androidx.core.app.ActivityCompat
import com.biosentry.androidbridge.BuildConfig
import com.biosentry.androidbridge.communication.CompressedImage
import com.biosentry.androidbridge.communication.Header
import com.biosentry.androidbridge.communication.ROSCamera
import com.biosentry.androidbridge.communication.time


/**
 * Can provide optional camera. Will select a rear-facing camera by default.
 */
@SuppressLint("NewApi", "InlinedApi")
class PhoneCamera(
    private val mContext: Context,
    private val mActivity: Activity,
    private var cameraID: String = String()
) : ROSCamera("android/phone/camera"){

    private val mCameraManager = mContext.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private val mSelectableCameras = mCameraManager.cameraIdList



    private val mFrameFormat     = ImageFormat.JPEG


    private val mHandlerThread : HandlerThread = HandlerThread("thread_name")
    private lateinit var mImageHandler : Handler

    private var mImagereader : ImageReader? = null
    private var mFinalBufferSize = 0
    private var mOutputBuffer = ByteArray(mFinalBufferSize)

    private val mOnImageAvailableListener =
        ImageReader.OnImageAvailableListener { reader ->
            val data = reader!!.acquireNextImage()

            //      Needed if i'm going to stitch the image.
            // Check if the size of outputbuffer is correct.
            // Reconstruct buffer if not.
            // if(mFinalBufferSize != ( data.planes.size * data.planes.first().buffer.array().size ) )
            // {
            //     mFinalBufferSize = (data.planes.size * data.planes.first().buffer.array().size)
            //     mOutputBuffer = ByteArray(mFinalBufferSize)
            // }

            // Going to try the first plane of the image first.
            // From: https://cmsdk.com/android/how-to-convert-android-media-image-to-bitmap-object.html
            // Might need to stitch planes.
            updateData( CompressedImage(
                Header( mSeq, time(mTimeInSeconds, mTimeInNanos), mFrameID),
                "jpeg",
                data = data.planes.first().buffer.array()
            ))
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
        override fun onReady(session: CameraCaptureSession) {
            Log.i(this.javaClass.simpleName, "CameraCaptureSession::onReady")
            super.onReady(session)
        }

        override fun onConfigured(session: CameraCaptureSession) {
            Log.d(this.javaClass.simpleName, "session is configured.")

            /**
             * Template record:
             * Create a request suitable for video recording.
             * Specifically, this means that a stable frame rate is used, and post-processing is set for recording quality.
             * These requests would commonly be used with the CameraCaptureSession#setRepeatingRequest method.
             */
            session.setRepeatingRequest(
                session.device.createCaptureRequest(TEMPLATE_RECORD).build(),
                mCameraCaptureSessionCaptureCallback,
                null
            )
        }

        override fun onActive(session: CameraCaptureSession) {
            Log.i(this.javaClass.simpleName, "CameraCaptureSession::onActive")
        }

        override fun onSurfacePrepared(session: CameraCaptureSession, surface: Surface) {
            Log.i(this.javaClass.simpleName, "CameraCaptureSession::onSurfacePrepared")
            super.onSurfacePrepared(session, surface)
        }

        override fun onConfigureFailed(session: CameraCaptureSession) {
            Log.e(this.javaClass.simpleName, "session configuration failed.")
        }

    }

    private val mCameraDeviceStateCallback = object : CameraDevice.StateCallback()  // https://developer.android.com/reference/android/hardware/camera2/CameraDevice
    {


        override fun onOpened(camera: CameraDevice) {
            val characteristics = mCameraManager.getCameraCharacteristics(camera.id)
            val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!


            for (size in map.getOutputSizes(mFrameFormat)) {
                Log.i(this.javaClass.simpleName, "imageDimension $size")
            }
            val availableSizes = map.getOutputSizes(mFrameFormat)
            val max_latency = 330867200L

            var sizeToUse = Size(0, 0)

            availableSizes.forEach {
                if ((it.width * it.height) > (sizeToUse.width * sizeToUse.height)
                    && map.getOutputMinFrameDuration(mFrameFormat, it)
                    + map.getOutputStallDuration(mFrameFormat, it) < max_latency
                ) // Look for resolution with most pixels.
                {
                    sizeToUse = it
                }
            }

            mImagereader =
                ImageReader.newInstance(sizeToUse.width, sizeToUse.height, mFrameFormat, 1)
            mImagereader!!.setOnImageAvailableListener(mOnImageAvailableListener, mImageHandler)
            val surface = mImagereader!!.surface
            if (BuildConfig.DEBUG && !surface.isValid) {
                error("Assertion failed")
            }
            val conf = OutputConfiguration(surface)

            val mSessionConfiguration = SessionConfiguration(
                                        SessionConfiguration.SESSION_REGULAR,
                                        mutableListOf(conf),
                                        { },
                                        mCameraCaptureSessionStateCallback )

            camera.createCaptureSession(mSessionConfiguration)
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
        mHandlerThread.start()
        mImageHandler = Handler(mHandlerThread.looper)

        askForPermissions()
        if(!mSelectableCameras.contains(cameraID))
            cameraID = mSelectableCameras.first()

        try
        {
            mCameraManager.openCamera(cameraID, mCameraDeviceStateCallback, mImageHandler)   // Uses current thread if null. Might make UI thread hang???
        }
        catch (ex: SecurityException) {
            Log.e("PhoneCamera", "Android system is rude. No permission for camera")
        }

    }
}