package com.biosentry.androidbridge.phone

/**
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
import co.infinum.goldeneye.VideoCallback
import co.infinum.goldeneye.models.Facing
import com.biosentry.androidbridge.communication.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File
import java.lang.Exception
import java.util.*
import kotlin.concurrent.timerTask




class ROSCamera(
    activity: Activity,
    private val context: Context,
    private val FPS: Int = 10
)
{

    private val mGoldenEye = GoldenEye.Builder(activity).build() // Main wrapper object.
    private var mTextureView : TextureView = TextureView(context)  // UI element to show output on.


    private val mVideoCallback = object : VideoCallback()
    {


    }

    private val mInitCallback = object : InitCallback()  // Callback to show error through.
    {
        override fun onError(t: Throwable) {
            Log.e(this.javaClass.simpleName, t.toString())

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
            Log.e(this.javaClass.simpleName, t.toString() )
            mTextureView = TextureView(context)

    }


        override fun onPictureTaken(picture: Bitmap) {
            GlobalScope.launch {

            }
        }
    }

    private val mTimer : Timer = Timer()

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
**/