package com.biosentry.androidbridge.phone

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.hardware.camera2.CameraDevice.TEMPLATE_RECORD
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.os.Handler
import android.util.Log
import android.view.Surface
import androidx.core.app.ActivityCompat
import com.biosentry.androidbridge.communication.ROSCamera

/**
 * Can provide optional camera. Will select a rear-facing camera by default.
 */
@SuppressLint("NewApi", "InlinedApi")
class PhoneCamera(val mContext : Context, val mActivity : Activity, var cameraID : String = String()) : ROSCamera("android/phone/camera"){

    private val mCameraManager : CameraManager = mContext.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private val mSelectableCameras = mCameraManager.cameraIdList

    /** Check if this device has a camera */
    private fun checkCameraHardware(context: Context): Boolean {
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA)
    }


    private val mCameraCaptureSessionCaptureCallback = object : CameraCaptureSession.CaptureCallback()
    {
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
            mutableListOf(OutputConfiguration( Surface(SurfaceTexture(0)))),
            {  },
            mCameraCaptureSessionStateCallback )

    private val mCameraDeviceStateCallback = object : CameraDevice.StateCallback()  // https://developer.android.com/reference/android/hardware/camera2/CameraDevice
    {
        override fun onOpened(camera: CameraDevice) {
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
            mCameraManager.openCamera(cameraID, mCameraDeviceStateCallback, null)
        }
        catch (ex: SecurityException) {
            Log.e("PhoneCamera", "Android system is rude. No permission for camera" )
        }

    }
}