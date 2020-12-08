package com.biosentry.androidbridge.aircraft

import android.util.Log
import com.biosentry.androidbridge.communication.*
import dji.common.error.DJIError
import dji.common.flightcontroller.FlightControllerState
import dji.common.flightcontroller.virtualstick.*
import dji.common.util.CommonCallbacks
import dji.sdk.products.Aircraft
import dji.sdk.sdkmanager.DJISDKManager
import dji.waypointv2.natives.util.NativeCallbackUtils
import java.util.*

class AircraftFlightController : IROSDevice
{
    // Cached instance of FlightControlData to remove need for reconstructing.
    private var mFlightControlData = FlightControlData(0.0F, 0.0F, 0.0F, 0.0F)
    private val topicPrefix = "/android/drone"

    val mGPS : ROSGPS = ROSGPS( mMessageTopicName = "$topicPrefix/gps")
    val mOdom = ROSOdomSensor(mMessageTopicName = "$topicPrefix/odom")

    override val mControls: List<ROSControl> = listOf(
        ROSControl(SubscribeMessage(type = "/geometry_msgs/Twist", topic = "$topicPrefix/geometry_msgs/Twist"), ::doWriteFlightControlData),
        ROSControl(SubscribeMessage(type = "/biosentry/AircraftFlightActions", topic = "$topicPrefix/biosentry/AircraftFlightActions"),     ::doAircraftFlightAction),
    )

    private var mLastCommandTime : Int = 0

    private val mControllerVirtualStickCallback = object : CommonCallbacks.CompletionCallbackWith<Boolean>
    {
        override fun onSuccess(p0: Boolean?)
        {
            if(p0 != null) {
                if (p0)
                {
                    mLastCommandTime++
                    if (mLastCommandTime >= 10) {
                        val product = DJISDKManager.getInstance().product
                        if (product is Aircraft && product.isConnected) {
                            product.flightController.setVirtualStickModeEnabled(false, mCallback)
                            Log.w(this.javaClass.simpleName, "V-stick mode disabled")
                        }
                    }
                }
                else
                {
                    mLastCommandTime = 0
                    Log.w(this.javaClass.simpleName, "V-stick command timeout reset. ")
                }
            }
        }


        override fun onFailure(p0: DJIError?) {
            p0?.let { error ->
                Log.e(
                    this.javaClass.simpleName,
                    error.errorCode.toString() + " | " + error.description
                )
            }
        }

    }

    private val mTimerVirtualStickCallback = object : TimerTask()
    {
        override fun run() {
        val product = DJISDKManager.getInstance().product
            if(product is Aircraft && product.isConnected)
            {
                product.flightController.getVirtualStickModeEnabled(mControllerVirtualStickCallback)
            }
        }
    }

    private val mTimer = Timer()

    private var mOdomSequenceNumber : Long = 0

    private val mStateCallback = FlightControllerState.Callback { p0 ->
        p0?.let {

            // GPS is kept as it can convey GPS connection-status information.
            mGPS.updateData(NavSatFix(
                mGPS.mHeader,
                mGPS.mStatus,
                if(it.aircraftLocation.latitude.isNaN()) 0.0 else it.aircraftLocation.latitude,
                if(it.aircraftLocation.longitude.isNaN()) 0.0 else it.aircraftLocation.longitude,
                if(it.aircraftLocation.altitude.isNaN()) 0.0 else it.aircraftLocation.altitude.toDouble(),
                DoubleArray(9),
                0
            ))

            mOdom.updateData( Odometry(
                header = Header(mOdomSequenceNumber, time(System.currentTimeMillis() / 1000, System.nanoTime() ), "frame"),
                child_frame_id = "child_frame",
                pose = PoseWithCovariance(
                    pose = Pose(
                        position = Point(
                            (mGPS.mReading.msg as NavSatFix).latitude,
                            (mGPS.mReading.msg as NavSatFix).longitude,
                            (mGPS.mReading.msg as NavSatFix).altitude),
                        orientation = mOdom.EulerToUnitQuaternion(
                            mOdom.toRadians(it.attitude.roll),
                            mOdom.toRadians(it.attitude.pitch),
                            mOdom.toRadians(it.attitude.yaw) )
                    )
                ),
                twist = TwistWithCovariance(
                    twist = Twist(
                        angular = Vector3(
                            0.0,
                            0.0,
                            mFlightControlData.yaw.toDouble() ),
                        linear = Vector3(
                            mFlightControlData.roll.toDouble(),
                            mFlightControlData.pitch.toDouble(),
                            mFlightControlData.verticalThrottle.toDouble() )
                        )
                    )
                )
            )
            mOdomSequenceNumber++
        }
    }

    private fun doNothing() /* no-op */
    {}

    private val mCallback = CommonCallbacks.CompletionCallback<DJIError> {
        it?.let { error ->
            println(
                this.javaClass.simpleName +
                "mCallback: " + error.errorCode.toString() + " | " + error.description
            )
        }
    }


    private fun doWriteFlightControlData(msg : ROSMessage )
    {
        if(msg is Twist)
        {
            val product = DJISDKManager.getInstance().product
            if(product is Aircraft && product.isConnected)
            {
                product.flightController.run {

                    // Enable virtual stick control system if not already enabled.
                    if(!isVirtualStickControlModeAvailable)
                    {
                        setVirtualStickModeEnabled(true, mCallback)
                        mLastCommandTime = 0
                    }


                    // Change representation of virtual stick control values.
                    // Geometry_msgs/Twist represents linear and angular VELOCITIES.
                    if(rollPitchControlMode == RollPitchControlMode.ANGLE)
                    {
                        rollPitchControlMode = RollPitchControlMode.VELOCITY
                        return@doWriteFlightControlData
                    }
                    if(yawControlMode == YawControlMode.ANGLE)
                    {
                        yawControlMode = YawControlMode.ANGULAR_VELOCITY
                        return@doWriteFlightControlData
                    }

                    if(rollPitchCoordinateSystem == FlightCoordinateSystem.GROUND)
                        rollPitchCoordinateSystem = FlightCoordinateSystem.BODY


                    // Write virtual stick control values.
                    mFlightControlData.pitch = msg.linear.x.toFloat()
                    mFlightControlData.roll = msg.linear.y.toFloat()
                    mFlightControlData.yaw = msg.angular.z.toFloat()
                    mFlightControlData.verticalThrottle = msg.linear.z.toFloat()

                    if(isVirtualStickControlModeAvailable)
                        sendVirtualStickFlightControlData(mFlightControlData, mCallback)
                }
            }
        }
    }


    private fun doAircraftFlightAction(msg : ROSMessage)
    {
        if(msg is AircraftFlightActions) {

            val product = DJISDKManager.getInstance().product
            if (product is Aircraft && product.isConnected) {

                product.flightController.let {
                    when (msg.flightActions) {
                        FlightActions.TurnMotorsOn -> it.turnOnMotors { mCallback }
                        FlightActions.TurnMotorsOff -> it.turnOffMotors { mCallback }
                        FlightActions.SetUrgentStopModeEnabled -> it.setUrgentStopModeEnabled(
                            true,
                            mCallback
                        )
                        FlightActions.SetUrgentStopModeDisabled -> it.setUrgentStopModeEnabled(
                            false,
                            mCallback
                        )
                        FlightActions.SetESCBeepEnabled -> it.setESCBeepEnabled(true, mCallback)
                        FlightActions.SetESCBeepDisabled -> it.setESCBeepEnabled(false, mCallback)
                        FlightActions.StartTakeoff -> it.startTakeoff { mCallback }
                        FlightActions.StartPrecisionTakeoff -> it.startPrecisionTakeoff { mCallback }
                        FlightActions.CancelTakeoff -> it.cancelTakeoff { mCallback }
                        FlightActions.StartLanding -> it.startLanding { mCallback }
                        FlightActions.CancelLanding -> it.cancelLanding { mCallback }
                        FlightActions.ConfirmLanding -> it.confirmLanding { mCallback }
                        FlightActions.Reboot -> it.reboot { mCallback }

                        FlightActions.StartGoHome -> it.startGoHome { mCallback }
                        FlightActions.CancelGoHome -> it.cancelGoHome { mCallback }
                        FlightActions.SetHomeLocationUsingCurrentAircraftLocation -> it.setHomeLocationUsingAircraftCurrentLocation { mCallback }
                    }
                }
            }
        }
    }

    init {
        val product = DJISDKManager.getInstance().product
        if(product !is Aircraft || !product.isConnected)
            Log.w(this.javaClass.simpleName, "A valid aircraft must be connected for this class to handle commands." )
        else
        {
            product.flightController.setStateCallback(mStateCallback)
            mTimer.schedule( mTimerVirtualStickCallback, 0, 1000)

            // FOR TESTING!!
            // BREAK THIS OUT WHEN ACTUALLY FLYING THE DAMN THING!!
            // product.flightController.setVirtualStickModeEnabled(true, mCallback)

        }


    }
}