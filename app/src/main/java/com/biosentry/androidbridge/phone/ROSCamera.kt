package com.biosentry.androidbridge.phone

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.SurfaceTexture
import android.util.Log
import android.view.TextureView
import androidx.core.content.ContextCompat
import co.infinum.goldeneye.GoldenEye
import co.infinum.goldeneye.InitCallback
import co.infinum.goldeneye.PictureCallback
import co.infinum.goldeneye.models.Facing
import com.biosentry.androidbridge.communication.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.lang.Exception
import java.util.*
import kotlin.concurrent.timerTask



@Suppress("EXPERIMENTAL_API_USAGE")
class ROSCamera(
    activity: Activity,
    private val context: Context,
    private val FPS: Int = 10
) : IROSSensor<CompressedImage>
{
    // ===================================== IMPLEMENTATION OF ROS-SENSOR INTERFACE =====================================
    override var mDataHandler :  ((PublishMessage<CompressedImage>) -> Unit )? = null
    var mErrorHandler : ((String) -> Unit)? = null

    private var mSequenceNumber : Long = 0
    private var mReading : CompressedImage = CompressedImage(
        Header(
            mSequenceNumber,
            time(0, 0),
            ""
        ),
        "jpeg",
        ""
    )

    override val mMessageTypeName: String
        get() = "sensor_msgs/CompressedImage"
    override val mMessageTopicName: String
        get() = "android/phone/image_raw/compressed"

    override fun read(): PublishMessage<CompressedImage> {
        return PublishMessage(
            type = mMessageTypeName,
            topic = mMessageTopicName,
            msg = mReading
        )
    }

    // ===================================== /IMPLEMENTATION OF ROS-SENSOR INTERFACE =====================================

    private val mGoldenEye = GoldenEye.Builder(activity).build() // Main wrapper object.
    private var mTextureView : TextureView = TextureView(context)  // UI element to show output on.


    private val mInitCallback = object : InitCallback()  // Callback to show error through.
    {
        override fun onError(t: Throwable) {
            Log.println(Log.ERROR, "Camera", t.toString())
            mErrorHandler?.invoke("InitCallback error: $t")
        }

        override fun onActive() {

            // If push interface is desired using the user-definable callback functions exposed in this class.
            // Else the user pulls data when relevant to him using the takePicture function manually.
            if( FPS > 0 )
            {
                val rateInMs = FPS * 1000L
                mTimer.schedule(
                    timerTask {
                        if(mGoldenEye.config != null)
                            try {
                                mGoldenEye.takePicture(mPictureCallback)
                            }
                            catch ( e : Exception) // Exception can fire if camera is not properly accessable.
                            {
                                Log.println(Log.ERROR, "Camera", e.toString() )
                            }

                    },1000, rateInMs )
            }

            super.onActive()
        }

    }


    private val mPictureCallback = object : PictureCallback()
    {
        override fun onError(t: Throwable) {
            Log.println(Log.ERROR, "ROSCamera picture", t.toString() )
            mErrorHandler?.invoke("PictureCallback error: $t")
            mTextureView = TextureView(context)

    }


        override fun onPictureTaken(picture: Bitmap) {
            mBitmapHandler?.invoke(picture)

            GlobalScope.launch {

                // https://stackoverflow.com/questions/20329090/how-to-convert-a-bitmap-to-a-jpeg-file-in-android
                val stream = ByteArrayOutputStream()
                picture.compress(Bitmap.CompressFormat.JPEG, 0, stream)
                val arr = stream.toByteArray().toUByteArray()



                mReading = CompressedImage(
                    Header(
                        mSequenceNumber,
                        time( Calendar.getInstance().time.time, 0 ),
                        "Corresponding Camera Info"
                    ),
                    "jpeg",
                    arr.contentToString()
                )

                mSequenceNumber++
            }


            mDataHandler?.invoke( read() )

            //picture.recycle()
        }
    }

    private val mTimer : Timer = Timer()
    var mBitmapHandler : ( (Bitmap) -> Unit)? = null

    init {
        mTextureView.setSurfaceTexture(SurfaceTexture(1))

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED )
        {
            /* Find back camera */
            val backCamera = mGoldenEye.availableCameras.find { it.facing == Facing.BACK }
            /* Open back camera */
            if (backCamera != null) {
                mGoldenEye.open(mTextureView, backCamera, mInitCallback )
            }
        }

    }

    fun takePicture()
    {
        mGoldenEye.takePicture(mPictureCallback)
    }
}
