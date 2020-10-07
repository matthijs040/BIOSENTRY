package com.example.biosentry

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.hardware.camera2.CameraCaptureSession.CaptureCallback
import android.media.Image
import android.media.ImageReader
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import android.view.Surface
import android.view.TextureView
import android.widget.Toast
import androidx.camera.core.ImageProxy
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.camera_fragment.*
import kotlinx.android.synthetic.main.fragment_home.*
import java.io.*
import java.util.*
import kotlin.experimental.and


class ROSCamera(
    private val activity: Activity,
    private val context: Context,
    private val FPS: Int = 10
) : IROSSensor<CompressedImage>
{
    private var cameraDevice: CameraDevice? = null
    private var cameraCaptureSessions: CameraCaptureSession? = null
    private var captureRequestBuilder: CaptureRequest.Builder? = null
    private var imageDimension: Size? = null
    private var imageReader: ImageReader? = null
    private val REQUEST_CAMERA_PERMISSION = 200
    private val mBackgroundHandler: Handler? = null

    // Permission definitions for asking for the right permissions towards the UI.
    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }


    override var mDataHandler :  ((ROSMessage<CompressedImage>) -> Unit )? = null
    var mImageHandler : ((Image) -> Unit)? = null

    private var mSequenceNumber : Long = 0
    private var mReading : CompressedImage = CompressedImage(
        Header(
            mSequenceNumber,
            time(0, 0),
            ""
        ),
        "jpeg",
        shortArrayOf(0)
    )

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            activity.baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun updatePreview() {
        if (null == cameraDevice) {
            Log.e("Camera", "updatePreview error, return")
        }
        captureRequestBuilder!!.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
        try {
            cameraCaptureSessions!!.setRepeatingRequest(
                captureRequestBuilder!!.build(),
                null,
                mBackgroundHandler
            )
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    private val stateCallback: CameraDevice.StateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            //This is called when the camera is open
            Log.e("ROS_CAM", "onOpened")
            cameraDevice = camera
            takePicture()
        }

        override fun onDisconnected(camera: CameraDevice) {
            cameraDevice!!.close()
        }

        override fun onError(camera: CameraDevice, error: Int) {
            cameraDevice?.close()
            cameraDevice = null
        }
    }



    fun openCamera() {
        val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        Log.e("Camera", "is camera open")
        try {
            val cameraId = manager.cameraIdList[0]
            val characteristics = manager.getCameraCharacteristics(cameraId)
            val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!
            imageDimension = map.getOutputSizes(SurfaceTexture::class.java)[0]
            // Add permission for camera and let user grant the permission
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.CAMERA
                ) !== PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) !== PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    REQUEST_CAMERA_PERMISSION
                )
                return
            }
            manager.openCamera(cameraId!!, stateCallback, null)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
        Log.e("Camera", "openCamera X")
    }


    private fun closeCamera() {
        if (null != cameraDevice) {
            cameraDevice!!.close()
            cameraDevice = null
        }
        if (null != imageReader) {
            imageReader?.close()
            imageReader = null
        }
    }

    fun takePicture() {
        if (null == cameraDevice) {
            Log.e("Camera", "cameraDevice is null")
            return
        }
        val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            val characteristics = manager.getCameraCharacteristics(
                cameraDevice!!.id
            )
            var jpegSizes: Array<Size>? = null
            jpegSizes =
                characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!
                    .getOutputSizes(ImageFormat.JPEG)
            var width = 640
            var height = 480

            if (jpegSizes != null && jpegSizes.isNotEmpty()
            ) {
                width = jpegSizes[0].width
                height = jpegSizes[0].height
            }
            val reader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1)
            val outputSurfaces: MutableList<Surface> = ArrayList(1)

            outputSurfaces.add(reader.surface)

            val captureBuilder =
                cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
            captureBuilder.addTarget(reader.surface)
            captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)

            val readerListener: ImageReader.OnImageAvailableListener =
                ImageReader.OnImageAvailableListener { reader ->
                    var image: Image? = null
                    try {
                        image = reader.acquireLatestImage()
                        val buffer = image.planes[0].buffer
                        val bytes = ByteArray(buffer.capacity())
                        buffer.get(bytes, 0, bytes.size)

                        mReading = CompressedImage(
                            mReading.header,
                            mReading.format,
                            bytes.toShortArray()
                        )

                        if(mDataHandler != null)
                            mDataHandler?.invoke(read())
                        if(mImageHandler != null && image != null)
                            mImageHandler?.invoke(image)
                    } catch (e: FileNotFoundException) {
                        e.printStackTrace()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    } finally {
                        image?.close()
                    }
                }
            reader.setOnImageAvailableListener(readerListener, mBackgroundHandler)

            val captureListener: CaptureCallback = object : CaptureCallback() {
            }
            cameraDevice!!.createCaptureSession(
                outputSurfaces,
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        try {
                            session.capture(
                                captureBuilder.build(),
                                captureListener,
                                mBackgroundHandler
                            )
                        } catch (e: CameraAccessException) {
                            e.printStackTrace()
                        }
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {}
                },
                mBackgroundHandler
            )
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    init {
        // Request camera permissions
        if (allPermissionsGranted()) {

            openCamera()
        } else {
            ActivityCompat.requestPermissions(
                activity, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }
    }


    override val mMessageTypeName: String
        get() = "sensor_msgs/CompressedImage"
    override val mMessageTopicName: String
        get() = "bridge/android/image_raw/compressed"

    override fun read(): ROSMessage<CompressedImage> {
        return ROSMessage(
            type = mMessageTypeName,
            topic = mMessageTopicName,
            msg = mReading
        )
    }
}

private fun ByteArray.toShortArray(): ShortArray {
    var ret : ShortArray = ShortArray(this.size)
    var ind : Int = 0
    forEach {
        if(it < 0)
            ret[ind] = (it + 255).toShort()
        else
            ret[ind] = it.toShort()
        ind++
    }
    return ret
}

