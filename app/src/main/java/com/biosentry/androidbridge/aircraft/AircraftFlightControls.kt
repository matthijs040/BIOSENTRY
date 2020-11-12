package com.biosentry.androidbridge.aircraft

import android.util.Log
import android.util.Log.WARN
import com.biosentry.androidbridge.communication.*
import dji.common.error.DJIError
import dji.common.flightcontroller.virtualstick.FlightControlData
import dji.common.util.CommonCallbacks
import dji.sdk.products.Aircraft
import dji.sdk.sdkmanager.DJISDKManager
import java.lang.Exception
import kotlin.reflect.KClass
import kotlin.reflect.jvm.javaMethod

class AircraftFlightControls : IROSDevice
{
    // Cached instance of FlightControlData to remove need for reconstructing.
    private var mFlightControlData = FlightControlData(0.0F, 0.0F, 0.0F, 0.0F)

    private fun doWriteFlightControlData(msg : ROSMessage )
    {
        if(msg is FlightControlData)
        {
            val product = DJISDKManager.getInstance().product
            if(product is Aircraft && product.isConnected)
            {
                product.flightController.run {

                    mFlightControlData.roll = msg.roll
                    mFlightControlData.pitch = msg.pitch
                    mFlightControlData.yaw = msg.yaw
                    mFlightControlData.verticalThrottle = msg.verticalThrottle

                    sendVirtualStickFlightControlData(mFlightControlData, null)
                    // MIGHT NEED A CALLBACK TO RESPOND TO STATUS FROM DRONE.
                }
            }
        }
    }

    private val mCallback = CommonCallbacks.CompletionCallback<DJIError> { p0 -> println(p0) }

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

    override val mControls: List<ROSControl> = listOf(
        ROSControl(SubscribeMessage(type = "/biosentry/AircraftFlightActions", topic = "/biosentry/AircraftFlightActions"),     ::doAircraftFlightAction),
        ROSControl(SubscribeMessage(type = "/geometry_msgs/Twist", topic = "/geometry_msgs/Twist"), ::doWriteFlightControlData)
    )

    init {
        val product = DJISDKManager.getInstance().product
        if(product !is Aircraft || !product.isConnected)
            Log.w(this.javaClass.simpleName, "A valid aircraft must be connected for this class to handle commands." )
    }
}