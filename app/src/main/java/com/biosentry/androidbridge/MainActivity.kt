package com.biosentry.androidbridge


import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.Menu
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.biosentry.androidbridge.aircraft.*
import com.biosentry.androidbridge.communication.ROSBridge
import com.biosentry.androidbridge.communication.PublishMessage
import com.biosentry.androidbridge.communication.ROSMessageHandler
import com.biosentry.androidbridge.communication.SubscribeMessage
import com.biosentry.androidbridge.phone.*
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.fragment_aircraft.*
import kotlinx.android.synthetic.main.fragment_home.*
import java.util.*
import kotlin.concurrent.timerTask


class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    var mROSBridge : ROSBridge? = null
    private var mROSMessageHandler : ROSMessageHandler? = null

    private var mROSAccelerometer : ROSAccelerometer? = null
    private var mROSGyroscope : ROSGyroscope? = null
    private var mROSGPS : ROSGPS? = null
    var mROSCamera : ROSCamera? = null
    
    private var mAircraftIMU : AircraftIMU? = null
    private var mAircraftGyro : AircraftGyroscope? = null
    private var mAircraftGPS : AircraftGPS? = null
    var mAircraftCamera : AircraftCamera? = null

    var mAircraftHandler : DJIAircraftHandler? = null
    var mLatestAircraftStatus : String? = null

    private val mTimer = Timer()


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
            setOf(R.id.nav_home, R.id.nav_sensors, R.id.nav_camera, R.id.nav_aircraft),
            drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
    }

    override fun onResume() {

        // Setup "ROS" hardware classes
        mROSAccelerometer   = ROSAccelerometer(baseContext)
        mROSGyroscope       = ROSGyroscope(baseContext)
        mROSGPS             = ROSGPS(baseContext, this)
        //mROSCamera          = ROSCamera(this, this.baseContext)


        mAircraftHandler = DJIAircraftHandler(this, null)

        mAircraftHandler?.mStatusHandler = ::droneWriteStatus
        mAircraftHandler?.mNameHandler = ::droneWriteName


        mTimer.schedule(
            timerTask {
                if(mAircraftHandler?.mAircraftConnected!! &&
                        mAircraftCamera == null)
                {
                    Looper.myLooper() ?: Looper.prepare()
                    mAircraftCamera = AircraftCamera(this@MainActivity)
                }
            },1000, 1000 )

        super.onResume()
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

    fun printDroneStatus(message : String)
    {
        Toast.makeText(this.baseContext, message, Toast.LENGTH_LONG).show()

        println(message)
    }

    private fun attachDevices()
    {
        // https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/let.html
        mROSAccelerometer?.let { mROSMessageHandler?.attachSensor(it, 0) }
        mROSGyroscope?.let { mROSMessageHandler?.attachSensor(it, 0) }
        mROSGPS?.let { mROSMessageHandler?.attachSensor(it, 0) }
        mROSCamera?.let { mROSMessageHandler?.attachSensor(it, 0L) }

        mAircraftIMU?.let {
            //TV_Debug?.text = "${TV_Debug.text}\nAircraft IMU is attached."
            mROSMessageHandler?.attachSensor(it, 0)
        }
        mAircraftGyro?.let {
            //TV_Debug?.text = "${TV_Debug.text}\nAircraft Gyro is attached."
            mROSMessageHandler?.attachSensor(it, 0)
        }
        mAircraftGPS?.let {
            //TV_Debug?.text = "${TV_Debug.text}\nAircraft GPS is attached."
            mROSMessageHandler?.attachSensor(it, 0)
        }

        mROSMessageHandler?.sub(SubscribeMessage(topic = "/test/point"))
    }

    private fun detachDevices()
    {
        mROSMessageHandler?.removeSensors()
    }

    @SuppressLint("SetTextI18n")
    fun WebSocketConnectClicked()
    {
        mROSBridge = ROSBridge(TB_URL.text.toString())

        mROSBridge!!.mErrorHandler  = ::webSocketWriteError
        mROSBridge!!.mStatusHandler = ::webSocketWriteStatus
        mROSBridge!!.mDataHandler   = ::receiveData

        try {
            mROSMessageHandler = ROSMessageHandler(mROSBridge!!)
        }
        catch (e: Exception)
        {
            Log.println(Log.ERROR, "MainActivity", e.toString())
            return
        }

        if(mAircraftHandler?.mAircraftConnected!!)
        {
            mAircraftIMU = AircraftIMU()
            mAircraftGyro = AircraftGyroscope()
            mAircraftGPS = AircraftGPS()
            TV_Debug?.text = "${TV_Debug.text}\nConstructing Aircraft sensors."
        }
    }

    fun WebSocketDisconnectClicked() {

        mROSBridge?.disconnect()
    }

    fun RTMPConnectClicked()
    {
        val ret = mAircraftCamera?.startStreaming(TB_RTMP_URL.text.toString())
        this.runOnUiThread{ TV_RTMP_error.text = ret.toString()}
    }

    fun RTMPDisconnectClicked()
    {
        mAircraftCamera?.stopStreaming()
    }

    // FUNCTIONS THAT WRITE INFORMATION TO UI! -----------------------------
    private fun webSocketWriteError(s: String) {
        runOnUiThread{ TV_websocket_error?.text = s }
    }
    private fun webSocketWriteStatus(s: String) {
        when(s)
        {
            "OPEN" -> attachDevices()
            "CLOSED" -> detachDevices()
        }
        runOnUiThread{ TV_websocket_status?.text = s }
    }

    // PLACEHOLDER
    private fun receiveData(message: Any) {
        println(message.toString() )
    }

    private fun droneWriteName(s : String) { runOnUiThread{ TV_drone_name?.text = s
                                                            TV_aircraft_name?.text = s} }

    private fun droneWriteStatus(s : String)
    {
        mLatestAircraftStatus = s
        runOnUiThread{
            TV_drone_status?.text = s
            TV_aircraft_status?.text = s}
    }



}
