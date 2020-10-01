package com.example.biosentry


import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.PendingIntent.getActivity
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.icu.text.SimpleDateFormat
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.biosentry.NavSatStatus.Companion.SERVICE_GLONASS
import com.example.biosentry.NavSatStatus.Companion.STATUS_FIX
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.fragment_home.*
import java.io.File
import java.nio.ByteBuffer
import java.util.*
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime
import kotlin.time.seconds


class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration

    private var mIsAdvertised : Boolean = false

    // WebSocket stuff
    private var mROSBridge : ROSBridge? = null

    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService

    private var mImageSequenceNumber : Long = 0
    private var mCameraAdvertized : Boolean = false

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it) == PackageManager.PERMISSION_GRANTED
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
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }



    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val toolbar: Toolbar = findViewById(R.id.toolbar)

        setSupportActionBar(toolbar)

        val fab: FloatingActionButton = findViewById(R.id.fab)

        fab.setOnClickListener { view ->
          Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
          .setAction("Action", null).show()
        }
        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        val navView: NavigationView = findViewById(R.id.nav_view)
        val navController = findNavController(R.id.nav_host_fragment)

        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.



        appBarConfiguration = AppBarConfiguration(
            setOf(R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow),
            drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }
    }

    companion object {
        private const val TAG = "CameraXBasic"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }

    override fun onResume() {

        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        camera_capture_button.setOnClickListener { takePhoto() }
        mIsAdvertised = false
        // Set up the listener for take photo button
        cameraExecutor = Executors.newSingleThreadExecutor()

        super.onResume()


    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults:
        IntArray) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
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

    private fun writeError(errorMessage: String)
    {
        TV_websocket_error.text = errorMessage
    }

    private fun writeStatus(statusMessage: String)
    {
        TV_websocket_status.text = statusMessage
    }

    // THIS template IMPLEMENTATION IS PLACEHOLDER!!
    private fun receiveData(message: ROSMessage<Any>)
    {
        println(message.toString())
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    fun connectClicked() {
        mROSBridge = ROSBridge(TB_URL.text.toString())
        mROSBridge?.mErrorHandler  = ::writeError
        mROSBridge?.mStatusHandler = ::writeStatus
        mROSBridge?.mDataHandler   = ::receiveData

    }

    fun disconnectClicked() {
        mROSBridge?.disconnect()
        mIsAdvertised = false
        mCameraAdvertized = false
    }

    fun sendData(readings: SensorReadings)
    {
        //val twist = Twist(   Vector3(readings.mAccelerationX, readings.mAccelerationY, readings.mAccelerationZ ),
        //                     Vector3(readings.mRotationX, readings.mRotationY, readings.mRotationZ)    )

        // val linear = Point ( readings.mAccelerationX, readings.mAccelerationY, readings.mAccelerationZ )
        val angular = ROSMessage<Point>(
            type = "geometry_msgs/Point", msg = Point(
                readings.mAccelerationX,
                readings.mAccelerationY,
                readings.mAccelerationZ
            )
        )
        val locationMsg = ROSMessage<NavSatFix>(type = "sensor_msgs/NavSatFix", msg = NavSatFix(
                NavSatStatus(
                    STATUS_FIX,
                    SERVICE_GLONASS
                ),

            readings.mLocationLatitude,
            readings.mLocationLongitude,
            altitude = 0.0,
            position_covariance = DoubleArray(9),
            position_covariance_type = 0
            )
        )

        if (!mIsAdvertised)
        {
            mROSBridge?.advertise("geometry_msgs/Point")
            mROSBridge?.advertise("sensor_msgs/NavSatFix")
            mIsAdvertised = true
        }

        mROSBridge?.send(angular)
        mROSBridge?.send(locationMsg)
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
}
