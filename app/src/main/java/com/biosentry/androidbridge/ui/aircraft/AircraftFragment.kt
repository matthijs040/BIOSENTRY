package com.biosentry.androidbridge.ui.aircraft

import android.graphics.Bitmap
import android.graphics.SurfaceTexture
import android.os.Build
import android.os.Bundle
import android.view.*
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.biosentry.androidbridge.MainActivity
import com.biosentry.androidbridge.R
import dji.sdk.products.Aircraft
import dji.sdk.sdkmanager.DJISDKManager
import kotlinx.android.synthetic.main.fragment_aircraft.*
import java.util.*

@RequiresApi(Build.VERSION_CODES.M)
class AircraftFragment : Fragment() {

    private lateinit var aircraftViewModel: AircraftViewModel

    private val mTimer : Timer = Timer()

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        aircraftViewModel =
            ViewModelProvider(this).get(AircraftViewModel::class.java)

        return inflater.inflate(R.layout.fragment_aircraft, container, false)

    }

    override fun onResume() {
        mTimer.schedule( object : TimerTask() {
            override fun run() {
                updateUI()
            }
        },
            500,
            500
        )
        super.onResume()
    }

    override fun onDestroy() {

        mTimer.cancel()
        mTimer.purge()

        super.onDestroy()
    }

    fun updateUI() {
        val act = requireActivity()
        if (act is MainActivity)
        {
            if (act.mAircraftHandler != null && act.mAircraftHandler!!.mAircraftConnected)
            {
                act.runOnUiThread()
                { TV_aircraft_status?.text = act.mLatestAircraftStatus }
            }
            val prod = DJISDKManager.getInstance().product
            if (prod is Aircraft && prod.model != null) {

                act.runOnUiThread{
                    TV_aircraft_status?.text = act.mLatestAircraftStatus
                    TV_aircraft_name?.text = prod.toString()
                }

                prod.flightController?.state?.let {
                    act.runOnUiThread()
                    {
                        TV_aircraft_latitude?.text = it.aircraftLocation.latitude.toString()
                        TV_aircraft_longitude?.text = it.aircraftLocation.longitude.toString()
                        TV_aircraft_altitude?.text = it.aircraftLocation.altitude.toString()

                        TV_aircraft_velocityX?.text = it.velocityX.toString()
                        TV_aircraft_velocityY?.text = it.velocityY.toString()
                        TV_aircraft_velocityZ?.text = it.velocityZ.toString()

                        TV_aircraft_roll?.text = it.attitude.roll.toString()
                        TV_aircraft_pitch?.text = it.attitude.pitch.toString()
                        TV_aircraft_yaw?.text = it.attitude.yaw.toString()

                        TV_signal_level?.text = it.gpsSignalLevel.toString()
                        TV_sat_count?.text = it.satelliteCount.toString()
                    }
                }
            }
            // check if texture already has camera assigned to it.
            if( TX_aircraft_camera?.surfaceTextureListener == null
                && act.mAircraftCamera != null)
            {
                val tex = requireActivity().findViewById<TextureView>(R.id.TX_aircraft_camera)
                tex.surfaceTextureListener = act.mAircraftCamera
                if(tex.isAvailable)
                    act.mAircraftCamera!!.onSurfaceTextureAvailable(tex.surfaceTexture!!, tex.width, tex.height)
                    act.mAircraftCamera!!.mCodecManager.getBitmap( ::setAircraftBitmap )
            }
        }
    }

    private fun setAircraftBitmap(p0 : Bitmap)
    {
        IV_aircraft_bitmap.setImageBitmap(p0)
    }
}