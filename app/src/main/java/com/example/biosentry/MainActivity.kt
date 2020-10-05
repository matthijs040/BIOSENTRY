package com.example.biosentry


import android.os.Bundle
import android.view.Menu
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.fragment_home.*
import kotlinx.coroutines.*
import kotlin.system.*

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private var mROSBridge : ROSBridge? = null
    private var mROSMessageHandler :ROSMessageHandler? = null

    private var mROSAccelerometer : ROSAccelerometer? = null
    private var mROSGyroscope : ROSGyroscope? = null
    private var mROSGPS : ROSGPS? = null
    private var mROSCamera : ROSCamera? = null
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
            setOf(R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow),
            drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
    }

    override fun onResume() {
        mROSAccelerometer = ROSAccelerometer(baseContext)
        mROSGyroscope = ROSGyroscope(baseContext)
        mROSGPS = ROSGPS(baseContext, this)
        mROSCamera = ROSCamera(this, this.baseContext)
        mROSCamera?.mPreviewView = viewFinder

        super.onResume()
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

    fun connectClicked()
    {
        mROSBridge = ROSBridge(TB_URL.text.toString())
        mROSBridge?.mErrorHandler  = ::writeError
        mROSBridge?.mStatusHandler = ::writeStatus
        mROSBridge?.mDataHandler   = ::receiveData

        try {
            mROSMessageHandler = ROSMessageHandler(mROSBridge!!)
        }
        catch (e : Exception)
        {
        }

        // https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/let.html
        mROSAccelerometer?.let { mROSMessageHandler?.attachSensor(it, 0) }
        mROSGyroscope?.let { mROSMessageHandler?.attachSensor(it, 0) }
        mROSGPS?.let { mROSMessageHandler?.attachSensor(it, 0) }
        mROSCamera?.let { mROSMessageHandler?.attachSensor(it, 1000) }
    }

    fun disconnectClicked() {
        mROSMessageHandler?.removeSensors()
        mROSBridge?.disconnect()

    }
}
