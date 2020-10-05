package com.example.biosentry

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.concurrent.timerTask

class ROSCamera(private val activity: Activity, private val context : Context, private val FPS : Int = 10) : IROSSensor<CompressedImage>, LifecycleOwner
{
    // Lifecycle stuff for allowing the camera to exist.
    private var mLifecycleRegistry: LifecycleRegistry = LifecycleRegistry(this)
    override fun getLifecycle(): Lifecycle {
        return mLifecycleRegistry
    }

    // Permission definitions for asking for the right permissions towards the UI.
    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }


    private var imageCapture: ImageCapture? = null
    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    var mPreviewView : PreviewView? = null

    private val mTimer : Timer = Timer()



    override var mDataHandler :  ( (ROSMessage<CompressedImage>) -> Unit )? = null

    private var mSequenceNumber : Long = 0
    private var mReading : CompressedImage = CompressedImage( Header(
        mSequenceNumber,
        time( 0,0),
        ""
    ),
    "jpeg",
    ubyteArrayOf(0U)
    )

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            activity.baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun takePhoto() {
        // Get a stable reference of the modifiable image capture use case
        val imageCapture = imageCapture ?: return

        // Set up image capture listener, which is triggered after photo has
        // been taken
        imageCapture.takePicture(ContextCompat.getMainExecutor(context), object :
            ImageCapture.OnImageCapturedCallback()
        {
            override fun onCaptureSuccess(image: ImageProxy)
            {
                if(image.format == ImageFormat.JPEG)
                {
                    val arr = image.planes[0].buffer.array().asUByteArray()


                    mReading = CompressedImage(
                        Header(
                            mSequenceNumber,
                            time( image.imageInfo.timestamp, 0),
                            "CameraInfoID"
                        ),
                        format = "jpeg",
                        data = arr
                    )
                }

                image.close()
                super.onCaptureSuccess(image)
            }

            override fun onError(exception: ImageCaptureException)
            {
                println(exception.toString())
                super.onError(exception)
            }
        })
    }

    private fun startCamera()
    {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener(Runnable {
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(mPreviewView?.createSurfaceProvider())
                }

            imageCapture = ImageCapture.Builder()
                .build()


            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture)

            } catch(e: Exception) {
                Log.println(Log.ERROR, "Use case binding failed", e.toString())
            }

        }, ContextCompat.getMainExecutor(context))
    }

    init {
        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                activity, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }

        mTimer.schedule(
            timerTask {
                takePhoto()
            },1000, 1000L / FPS )


    }

    override val mMessageTypeName: String
        get() = "sensor_msgs/CompressedImage"
    override val mMessageTopicName: String
        get() = "bridge/android/image_raw/compressed"

    override fun read(): ROSMessage<CompressedImage> {
        return ROSMessage(
            type= mMessageTypeName,
            topic= mMessageTopicName,
            msg= mReading
        )
    }



}
