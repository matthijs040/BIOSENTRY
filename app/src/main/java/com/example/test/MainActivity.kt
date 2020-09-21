package com.example.test


import android.os.Bundle
import android.view.Menu
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.test.ui.gallery.GalleryFragment
import com.example.test.ui.slideshow.SlideshowFragment
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.neovisionaries.ws.client.*
import kotlinx.android.synthetic.main.fragment_home.*
import java.lang.reflect.Type


class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration


    private var mIsAdvertised : Boolean = false

    // WebSocket stuff
    private var mROSBridge : ROSBridge? = null

    private var mGson : Gson = Gson()

    private val mWebSocketListener : WebSocketAdapter = object : WebSocketAdapter() {
        override fun onTextFrame(websocket: WebSocket?, frame: WebSocketFrame?) {
            frame?.toString()
        }

        override fun onConnectError(websocket: WebSocket?, exception: WebSocketException?) {

            TV_websocket_error.text = exception.toString()

        }

        override fun onStateChanged(websocket: WebSocket?, newState: WebSocketState?) {

            TV_websocket_status.text = newState.toString()
        }
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
    }

    private fun writeError(errorMessage : String)
    {
        TV_websocket_error.text = errorMessage
    }

    private fun writeStatus(statusMessage : String)
    {
        TV_websocket_status.text = statusMessage
    }

    private fun receiveData(message : ROSMessage)
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
    }

    fun sendData(readings: SensorReadings)
    {
        val twist = Twist(   Vector3(readings.mAccelerationX, readings.mAccelerationY, readings.mAccelerationZ ),
                             Vector3(readings.mRotationX, readings.mRotationY, readings.mRotationZ)    )

        if (!mIsAdvertised)
        {
            mROSBridge?.advertise("geometry_msgs/Twist" )
            mIsAdvertised = true
        }

        mROSBridge?.send(twist)
    }

}