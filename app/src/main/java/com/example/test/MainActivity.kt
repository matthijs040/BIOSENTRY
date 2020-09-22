package com.example.test


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

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration

    private var mIsAdvertised : Boolean = false

    // WebSocket stuff
    private var mROSBridge : ROSBridge? = null

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

    // THIS template IMPLEMENTATION IS PLACEHOLDER!!
    private fun receiveData(message : ROSMessage<Any>)
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
    }

    fun sendData(readings: SensorReadings)
    {
        //val twist = Twist(   Vector3(readings.mAccelerationX, readings.mAccelerationY, readings.mAccelerationZ ),
        //                    Vector3(readings.mRotationX, readings.mRotationY, readings.mRotationZ)    )

        // val linear = Point ( readings.mAccelerationX, readings.mAccelerationY, readings.mAccelerationZ )
        val angular = ROSMessage<Point>( type= "geometry_msgs/Point", msg= Point(readings.mAccelerationX, readings.mAccelerationY, readings.mAccelerationZ) )

        if (!mIsAdvertised)
        {
            mROSBridge?.advertise("geometry_msgs/Point" )
            mIsAdvertised = true
        }

        mROSBridge?.send(angular)
    }

}