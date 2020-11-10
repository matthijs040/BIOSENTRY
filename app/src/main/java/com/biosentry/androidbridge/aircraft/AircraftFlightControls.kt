package com.biosentry.androidbridge.aircraft

import com.biosentry.androidbridge.communication.*
import dji.common.flightcontroller.virtualstick.FlightControlData
import dji.sdk.products.Aircraft
import dji.sdk.sdkmanager.DJISDKManager
import java.lang.Exception
import kotlin.reflect.KClass

class AircraftFlightControls : IROSDevice
{
    // Cached instance of FlightControlData to remove need for reconstructing.
    private var mFlightControlData = FlightControlData(0.0F, 0.0F, 0.0F, 0.0F)

    private fun doWriteFlightControlData(msg : PublishMessage<AircraftFlightControlData> )
    {
        val product = DJISDKManager.getInstance().product
        if(product is Aircraft && product.isConnected)
        {
            product.flightController.run {

                mFlightControlData.roll = msg.msg.roll
                mFlightControlData.pitch = msg.msg.pitch
                mFlightControlData.yaw = msg.msg.yaw
                mFlightControlData.verticalThrottle = msg.msg.VThrottle

                sendVirtualStickFlightControlData(mFlightControlData, null)
                // MIGHT NEED A CALLBACK TO RESPOND TO STATUS FROM DRONE.
            }
        }
    }

    private fun doAircraftFlightAction(msg : PublishMessage<AircraftFlightActions>)
    {
        val product = DJISDKManager.getInstance().product
        if(product is Aircraft && product.isConnected)
        {
            product.flightController.let {
                when(msg.msg.flightActions)
                {
                    FlightActions.TurnMotorsOn              -> it.turnOnMotors {  }
                    FlightActions.TurnMotorsOff             -> it.turnOffMotors {  }
                    FlightActions.SetUrgentStopModeEnabled  -> it.setUrgentStopModeEnabled(true, null)
                    FlightActions.SetUrgentStopModeDisabled -> it.setUrgentStopModeEnabled(false, null)
                    FlightActions.SetESCBeepEnabled         -> it.setESCBeepEnabled(true, null)
                    FlightActions.SetESCBeepDisabled        -> it.setESCBeepEnabled(false, null)
                    FlightActions.StartTakeoff              -> it.startTakeoff {  }
                    FlightActions.StartPrecisionTakeoff     -> it.startPrecisionTakeoff {  }
                    FlightActions.CancelTakeoff             -> it.cancelTakeoff {  }
                    FlightActions.StartLanding              -> it.startLanding {  }
                    FlightActions.CancelLanding             -> it.cancelLanding {  }
                    FlightActions.ConfirmLanding            -> it.confirmLanding {  }
                    FlightActions.Reboot                    -> it.reboot {  }
                }
            }
        }
    }

    override val mControls: List<ROSControl<*>> = listOf(
        ROSControl(type = AircraftFlightActions::class,     SubscribeMessage(type = "DJIBridge/FlightActions", topic = ""),     ::doAircraftFlightAction),
        ROSControl(type = AircraftFlightControlData::class, SubscribeMessage(type = "DJIBridge/FlightControlData", topic = ""), ::doWriteFlightControlData)
    )

    init {
        val product = DJISDKManager.getInstance().product
        if(product !is Aircraft || !product.isConnected)
            throw Exception("Valid Aircraft required for initialization")
    }
}