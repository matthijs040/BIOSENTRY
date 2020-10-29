package com.biosentry.androidbridge.aircraft

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.requestPermissions

import androidx.core.content.ContextCompat
import com.biosentry.androidbridge.IROSSensor
import com.biosentry.androidbridge.MainActivity
import com.biosentry.androidbridge.ROSMessage
import com.biosentry.androidbridge.Vector3

import dji.common.error.DJIError
import dji.common.error.DJISDKError
import dji.sdk.base.BaseComponent
import dji.sdk.base.BaseProduct
import dji.sdk.products.Aircraft
import dji.sdk.sdkmanager.DJISDKInitEvent
import dji.sdk.sdkmanager.DJISDKManager
import dji.thirdparty.afinal.core.AsyncTask
import java.util.concurrent.atomic.AtomicBoolean



class DJIAircraftHandler(private val act : Activity, statusCallback : ((String) -> Unit)?) : ActivityCompat.OnRequestPermissionsResultCallback{

    private val mHandler: Handler = Handler(Looper.getMainLooper()) //handler thread that takes care of async behaviour.
    var mAircraft : Aircraft? = null
    var mStatusHandler : ((String) -> Unit)? = null // function this class can send status updates about the drone to.
    var mNameHandler : ((String) -> Unit)? = null // function this class can send the name of the drone to when registered.

    private val isRegistrationInProgress: AtomicBoolean = AtomicBoolean(false)

    private val missingPermission: MutableList<String> = ArrayList()

    init {
        mStatusHandler = statusCallback
        // When the compile and target version is higher than 22, please request the following permission at runtime to ensure the SDK works well.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkAndRequestPermissions()
        }
        startSDKRegistration()
    }

    private fun initComponents()
    {
        if(mAircraft == null)
            return

        mAircraft!!.let {
            mNameHandler?.invoke(it.model.displayName)




        }
    }

    private fun deinitComponents()
    {
        //Destruct instances of nested classes.
    }

    /**
     * Checks if there is any missing permissions, and
     * requests runtime permission if needed.
     */
    private fun checkAndRequestPermissions() {
        // Check for permissions
        for (eachPermission in REQUIRED_PERMISSION_LIST) {
            if (ContextCompat.checkSelfPermission(
                    act.applicationContext,
                    eachPermission
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                missingPermission.add(eachPermission)
            }
        }
        // Request for missing permissions
        if (missingPermission.isEmpty()) {
            startSDKRegistration()
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mStatusHandler?.invoke("Need to grant the permissions!")
            requestPermissions(
                act,
                missingPermission.toTypedArray(),
                REQUEST_PERMISSION_CODE
            )
        }
    }

    private fun startSDKRegistration()
    {

            if (isRegistrationInProgress.compareAndSet(false, true)) {
                AsyncTask.execute {
                    mStatusHandler?.invoke("registering, please wait...")
                    DJISDKManager.getInstance().registerApp(act.applicationContext, object :
                        DJISDKManager.SDKManagerCallback {
                        override fun onRegister(djiError: DJIError) {
                            if (djiError === DJISDKError.REGISTRATION_SUCCESS) {
                                mStatusHandler?.invoke("Registration Success")
                                DJISDKManager.getInstance().startConnectionToProduct()
                            } else {
                                mStatusHandler?.invoke("Register sdk fails, please check the bundle id and network connection!")

                            }
                            Log.v(TAG, djiError.description)
                        }

                        override fun onProductDisconnect() {
                            val message = "Product Disconnected"
                            mStatusHandler?.invoke(message)
                            mNameHandler?.invoke("....")
                            Log.d(TAG, message)
                            notifyStatusChange()
                        }

                        override fun onProductConnect(newProduct: BaseProduct?) {
                            val message: String
                            if (newProduct != null ) {
                                message = String.format("onProductConnect newProduct:%s", newProduct)
                                if(newProduct.model != null  && newProduct is Aircraft)
                                {
                                    mAircraft = newProduct
                                    initComponents() //Valid controller with aircraft are active. Can now initialize class components.

                                }
                            } else
                                message = String.format("onProductConnect newProduct:%s", "NULL")

                            mStatusHandler?.invoke(message)
                            Log.d(TAG, message)
                            notifyStatusChange()
                        }

                        override fun onProductChanged(p0: BaseProduct?) {
                            val message: String
                            if (p0 != null)
                            {
                                message = String.format("onProductChanged: newProduct:%s", p0)
                                if(p0.model != null && p0 is Aircraft)
                                {
                                    mAircraft = p0
                                    initComponents()
                                }
                                else
                                {
                                    mAircraft = null
                                    mNameHandler?.invoke("Controller")
                                }


                            }
                            else
                                message = "onProductChanged: newProduct is null"
                            mStatusHandler?.invoke(message)
                        }

                        override fun onComponentChange(
                            componentKey: BaseProduct.ComponentKey,
                            oldComponent: BaseComponent?,
                            newComponent: BaseComponent?
                        ) {
                            newComponent?.setComponentListener { isConnected ->
                                Log.d(TAG, "onComponentConnectivityChanged: $isConnected")

                                notifyStatusChange()
                            }
                            val message: String

                            if (oldComponent != null)
                                message = String.format(
                                    "onComponentChange key:%s, oldComponent:%s, newComponent:%s",
                                    componentKey,
                                    oldComponent,
                                    newComponent
                                )
                            else
                                message = String.format(
                                    "onComponentChange key:%s, oldComponent:%s, newComponent:%s",
                                    componentKey,
                                    "null",
                                    newComponent
                                )

                            Log.d(TAG, message)
                            //mStatusHandler?.invoke(message)
                        }

                        override fun onInitProcess(djisdkInitEvent: DJISDKInitEvent, i: Int) {}
                        override fun onDatabaseDownloadProgress(l: Long, l1: Long) {}
                    }
                    )
                }

            }
    }

    private fun notifyStatusChange() {
        mHandler.removeCallbacks(updateRunnable)
        mHandler.postDelayed(updateRunnable, 500)
    }

    private val updateRunnable = Runnable {
        val intent = Intent(FLAG_CONNECTION_CHANGE)
        act.sendBroadcast(intent)
    }

    companion object {
        private val TAG = MainActivity::class.java.name
        const val FLAG_CONNECTION_CHANGE = "dji_sdk_connection_change"
        private val REQUIRED_PERMISSION_LIST = arrayOf<String>(
            Manifest.permission.VIBRATE,
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.WAKE_LOCK,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.READ_PHONE_STATE

        )
        private const val REQUEST_PERMISSION_CODE = 12345
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        // Check for granted permission and remove from missing list
        if (requestCode == REQUEST_PERMISSION_CODE) {
            for (i in grantResults.indices.reversed()) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    missingPermission.remove(permissions[i])
                }
            }
        }
        // If there is enough permission, we will start the registration
        if (missingPermission.isEmpty()) {
            startSDKRegistration()
        } else {
            mStatusHandler?.invoke("Still missing Permissions...")
        }
    }

    /**
    inner class AircraftIMU : IROSSensor<Vector3>
    {
        private val name : String = mAircraft!!.model.displayName
        
        override val mMessageTypeName: String = "geometry_msgs/Vector3";
        override val mMessageTopicName: String = "android/$name/IMU"
        override var mDataHandler: ((ROSMessage<Vector3>) -> Unit)? = null

        override fun read(): ROSMessage<Vector3>
        {
            mAircraft!!.flightController.state.let {
                return ROSMessage(
                    type = mMessageTypeName,
                    topic = mMessageTopicName,
                    msg = Vector3(
                        it.velocityX.toDouble(),
                        it.velocityY.toDouble(),
                        it.velocityZ.toDouble() )
                    )
            }

            private val mIMUListener = object : IMUListener
        }

        init {
            if(mAircraft == null)
                throw IllegalArgumentException()
        }

    }
    **/

}