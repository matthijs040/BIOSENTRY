package com.biosentry.androidbridge


import android.os.Bundle
import android.util.Log
import android.view.Menu
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.biosentry.androidbridge.ui.home.HomeFragment
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.fragment_home.*

@ExperimentalUnsignedTypes
class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    var mROSBridge : ROSBridge? = null
    private var mROSMessageHandler :ROSMessageHandler? = null

    private var mROSAccelerometer : ROSAccelerometer? = null
    private var mROSGyroscope : ROSGyroscope? = null
    private var mROSGPS : ROSGPS? = null
    var mROSCamera : ROSCamera? = null

    private var mAircraftHandler : DJIAircraftHandler? = null

    private var mHomeFragment : HomeFragment? = null
    //private var

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

        runOnUiThread{
            mAircraftHandler = DJIAircraftHandler(this, null)
        }


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
        println(message)
    }

    fun connectClicked()
    {
        mROSBridge = ROSBridge(TB_URL.text.toString())

        mROSBridge!!.mErrorHandler  = ::writeError
        mROSBridge!!.mStatusHandler = ::writeStatus
        mROSBridge!!.mDataHandler   = ::receiveData

        try {
            mROSMessageHandler = ROSMessageHandler(mROSBridge!!)
        }
        catch (e: Exception)
        {
            Log.println(Log.ERROR, "MainActivity", e.toString())
            return
        }

        // https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/let.html
        mROSAccelerometer?.let { mROSMessageHandler?.attachSensor(it, 0) }
        mROSGyroscope?.let { mROSMessageHandler?.attachSensor(it, 0) }
        mROSGPS?.let { mROSMessageHandler?.attachSensor(it, 0) }
        mROSCamera?.let { mROSMessageHandler?.attachSensor(it, 0L) }
    }

    fun disconnectClicked() {
        mROSMessageHandler?.removeSensors()
        mROSBridge?.disconnect()

    }

    // FUNCTIONS THAT WRITE INFORMATION TO UI! -----------------------------
    private fun writeError(errorMessage: String)
    {
        TV_websocket_error?.text = errorMessage
    }

    private fun writeStatus(statusMessage: String)
    {
        TV_websocket_status?.text = statusMessage
    }

    // PLACEHOLDER
    private fun receiveData(message: ROSMessage<Any>)
    {
        println(message.toString())
    }
}
