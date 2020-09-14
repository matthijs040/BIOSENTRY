package com.example.test


import android.location.LocationManager
import android.os.Bundle

import android.view.Menu

import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar

import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.fragment_gallery.*
import java.util.*


class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private var sensors : Sensors? = null

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

        sensors = Sensors(
            context = this.baseContext,
            activity = this
        )
        val timerObj = Timer()
        val timerTaskObj: TimerTask = object : TimerTask() {
            override fun run() {
                refreshSensorReadings()
            }
        }
        timerObj.schedule(timerTaskObj, 0, 500)

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

    fun refreshSensorReadings()
    {
        val newReadings = sensors!!.read()
        if(newReadings.haveChanged)
            runOnUiThread(){
                val frag : Fragment? = supportFragmentManager.findFragmentById(R.layout.fragment_gallery.toInt())
                frag?.TVLongitude?.text = newReadings.LocationLongitude.toString()
                frag?.TVLatitude?.text = newReadings.LocationLatitude.toString()

                frag?.TVAccelX?.text = newReadings.AccelerationX.toString()
                frag?.TVAccelY?.text = newReadings.AccelerationY.toString()
                frag?.TVAccelZ?.text = newReadings.AccelerationZ.toString()

                frag?.TVRoll?.text = newReadings.RotationX.toString()
                frag?.TVPitch?.text = newReadings.RotationY.toString()
                frag?.TVYaw?.text = newReadings.RotationZ.toString()
            }
    }
}