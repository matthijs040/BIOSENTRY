package com.biosentry.androidbridge.aircraft

import com.biosentry.androidbridge.communication.*
import dji.sdk.products.Aircraft
import dji.sdk.sdkmanager.DJISDKManager
import java.lang.Exception

class AircraftWaypointControls : IROSDevice
{

    fun doAircraftFlightAction(msg : PublishMessage<AircraftFlightActions>)
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
        ROSControl(PublishMessage(type = "DJIBridge/FlightActions", topic = "", msg = AircraftFlightActions(FlightActions.Reboot)), ::doAircraftFlightAction)
    )

    init {
        val product = DJISDKManager.getInstance().product
        if(product !is Aircraft || !product.isConnected)
            throw Exception("Valid Aircraft required for initialization")
    }
}