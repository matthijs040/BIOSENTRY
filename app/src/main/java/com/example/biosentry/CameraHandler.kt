package com.example.biosentry

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Context.*
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.provider.Settings
import android.view.*
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat

/**
 * https://medium.com/@tylerwalker/integrating-camera2-api-on-android-feat-kotlin-4a4e65dc593f
 */
/** Helper to ask camera permission.  */
object CameraPermissionHelper {
    private const val CAMERA_PERMISSION_CODE = 0
    private const val CAMERA_PERMISSION = Manifest.permission.CAMERA

    /** Check to see we have the necessary permissions for this app.  */
    fun hasCameraPermission(context: Context) : Boolean
    {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }
    

    /** Check to see we have the necessary permissions for this app, and ask for them if we don't.  */
    fun requestCameraPermission(activity: Activity) {
        ActivityCompat.requestPermissions(
            activity, arrayOf(CAMERA_PERMISSION), CAMERA_PERMISSION_CODE
        )
    }

    /** Check to see if we need to show the rationale for this permission.  */
    fun shouldShowRequestPermissionRationale(activity: Activity): Boolean {
        return ActivityCompat.shouldShowRequestPermissionRationale(activity, CAMERA_PERMISSION)
    }

    /** Launch Application Setting to grant permission.  */
    fun launchPermissionSettings(activity: Activity) {
        val intent = Intent()
        intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        intent.data = Uri.fromParts("package", activity.packageName, null)
        activity.startActivity(intent)
    }

}


@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class CameraHandler(private val context : Context, private val activity : Activity)
{
    private val cameraManager = context.getSystemService(CAMERA_SERVICE) as CameraManager
    private val windowManager : WindowManager = context.getSystemService(WINDOW_SERVICE) as WindowManager
    private val previewSurface = SurfaceView(context).holder.surface
    private var cameraDevice : CameraDevice? = null

    var mErrorHandler  : ( (String) -> Unit )?      = null
    var mStatusHandler : ( (String) -> Unit )?      = null


    private val captureCallback = object : CameraCaptureSession.StateCallback()
    {
        override fun onConfigureFailed(session: CameraCaptureSession) {}

        override fun onConfigured(session: CameraCaptureSession)
        {
            val previewRequestBuilder = cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            previewRequestBuilder?.addTarget(previewSurface)
            session.setRepeatingRequest( previewRequestBuilder!!.build(), object: CameraCaptureSession.CaptureCallback() {},
            Handler { true }
            )
        }
    }

    val surfaceReadyCallback = object: SurfaceHolder.Callback {
        override fun surfaceCreated(holder: SurfaceHolder) {
            startCameraSession()
        }

        override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}

        override fun surfaceDestroyed(holder: SurfaceHolder) {}


    }


    private fun startCameraSession()
    {
        val cameraManager = context.getSystemService(CAMERA_SERVICE) as CameraManager

        if (cameraManager.cameraIdList.isEmpty())
        {
        // no cameras
        return
        }

        if (!CameraPermissionHelper.hasCameraPermission(context)) {
            CameraPermissionHelper.requestCameraPermission(activity)
            return
        }

        lateinit var firstCamera : String
        try
        {
            firstCamera = cameraManager.cameraIdList[0]
        }
        catch (ex : CameraAccessException )
        {
            ex.printStackTrace()
        }


        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        cameraManager.openCamera(firstCamera, object: CameraDevice.StateCallback()
        {
            override fun onDisconnected(p0: CameraDevice) { }
            override fun onError(p0: CameraDevice, p1: Int)
            {
                when(p1)
                {
                    ERROR_CAMERA_IN_USE         -> mErrorHandler?.invoke("ERROR_CAMERA_IN_USE")
                    ERROR_MAX_CAMERAS_IN_USE    -> mErrorHandler?.invoke("ERROR_MAX_CAMERAS_IN_USE")
                    ERROR_CAMERA_DISABLED       -> mErrorHandler?.invoke("ERROR_CAMERA_DISABLED")
                    ERROR_CAMERA_DEVICE         -> mErrorHandler?.invoke("ERROR_CAMERA_DEVICE")
                    ERROR_CAMERA_SERVICE        -> mErrorHandler?.invoke("ERROR_CAMERA_SERVICE")
                }
            }


            override fun onOpened(cameraDevice: CameraDevice) {
                // use the camera
                val cameraCharacteristics =    cameraManager.getCameraCharacteristics(cameraDevice.id)

                cameraCharacteristics[CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP]?.let { streamConfigurationMap ->
                    streamConfigurationMap.getOutputSizes(ImageFormat.YUV_420_888)?.let { yuvSizes ->
                        val previewSize = yuvSizes.last()

                    }

                }
            }
        }, Handler { true })
    }


    init {
        if(!CameraPermissionHelper.hasCameraPermission(activity))
            CameraPermissionHelper.requestCameraPermission(activity)

        startCameraSession()
    }
}