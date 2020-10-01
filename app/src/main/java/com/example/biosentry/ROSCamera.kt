package com.example.biosentry
/**
import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.util.Log
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.fragment_home.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ROSCamera(val activity: Activity,val context : Context) {

    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService

    private var mImageSequenceNumber : Long = 0
    private var mCameraAdvertized : Boolean = false

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            activity.baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun takePhoto() {
        // Get a stable reference of the modifiable image capture use case
        val imageCapture = imageCapture ?: return

        // Set up image capture listener, which is triggered after photo has
        // been taken
        imageCapture.takePicture(ContextCompat.getMainExecutor(this), object :
            ImageCapture.OnImageCapturedCallback()
        {


            @SuppressLint("UnsafeExperimentalUsageError") // I SHOULD BE ABLE TO DO THIS!
            override fun onCaptureSuccess(image: ImageProxy)
            {
                mROSBridge ?: return

                println(image.toString())


                if(!mCameraAdvertized)
                {
                    mROSBridge?.advertise("sensor_msgs/CompressedImage", "bridge/image_raw/compressed")
                    mCameraAdvertized = true
                }

                val msg = image.image?.toROSCompressedMessage("bridge/image_raw/compressed")

                if(msg != null)
                    mROSBridge?.send( msg)


                super.onCaptureSuccess(image)
                image.close()
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
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener(Runnable {
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(viewFinder.createSurfaceProvider())
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

            } catch(exc: Exception) {
                Log.e(MainActivity.TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    init {
        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                activity, MainActivity.REQUIRED_PERMISSIONS, MainActivity.REQUEST_CODE_PERMISSIONS
            )
        }

        // camera_capture_button.setOnClickListener { takePhoto() }
        mIsAdvertised = false
        // Set up the listener for take photo button
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults:
        IntArray) {
        if (requestCode == MainActivity.REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT).show()
                finish()
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    fun android.media.Image.toROSCompressedMessage(topic : String) : ROSMessage<CompressedImage>?
    {
        if(this.format != ImageFormat.JPEG)
            return null

        val size = this.width * this.height * this.planes.size
        var arr : ByteArray = ByteArray(size)

        var ind = 0
        for( plane in this.planes)
        {
            if(plane.buffer.hasArray())
                System.arraycopy(plane.buffer.array(), 0, arr, ind * plane.buffer.array().size, plane.buffer.array().size )
            ind++
        }

        val uArr = arr.toUByteArray()

        return ROSMessage(
            type= "sensor_msgs/CompressedImage",
            topic= topic,
            msg= CompressedImage(
                header = Header(seq = mImageSequenceNumber,
                    stamp = time( sec= this.timestamp, nsec = 0 ),
                    frame_id = "camera info frame ID" ),

                format = "jpeg",
                data = uArr
            )
        )
    }

    companion object {
        private const val TAG = "CameraXBasic"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }

}
 **/