package com.biosentry.androidbridge.communication

//////////////////////// ROSBridge data classes ////////////////////////
// ROSBridge message specs are from: https://github.com/RobotWebTools/rosbridge_suite/blob/develop/ROSBRIDGE_PROTOCOL.md

interface BridgeMessage

data class AdvertiseMessage(
    val op : String = "advertise",
    val topic : String,
    val type : String,
    val id : String? = null
) : BridgeMessage

data class UnadvertiseMessage (
    val op : String = "unadvertise",
    val topic : String,
    val id : String? = null
) : BridgeMessage

open class PublishMessage(val op : String = "publish",
                             val type: String,
                             var topic : String,
                             var msg : ROSMessage
) : BridgeMessage

data class SubscribeMessage( val op : String = "subscribe",
                            val id : String? = null,
                            val topic : String,
                            val type : String? = null,
                            val throttle_rate : Int? = null,
                            val queue_length : Int? = null,
                            val fragment_size : Int? = null,
                            val compression : String? = null
): BridgeMessage

data class UnsubscribeMessage( val op : String = "unsubscribe",
                               val id : String? = null,
                               val topic : String
): BridgeMessage

//////////////////////// ROS MESSAGE DATA CLASSES ////////////////////////
/**
 * http://docs.ros.org/noetic/api/std_msgs/html/msg/Header.html
 */

interface  ROSMessage

data class time(val sec : Long, val nsec: Long) : ROSMessage

data class Header(val seq : Long, val stamp : time, val frame_id : String ): ROSMessage

data class CameraInfo(val header: Header, val height: Long, val width: Long,
                      val distortion_model : String, val D : DoubleArray, val K : DoubleArray,
                      val R : DoubleArray, val P : DoubleArray,
                      val binning_x : Long, val binning_y : Long ): ROSMessage

/**
 * http://docs.ros.org/noetic/api/sensor_msgs/html/msg/Image.html
 */
data class Image(val header : Header, val height : Long, val width : Long, val encoding : String,
                 val is_bigendian : Byte, val step : Long, val data : String ): ROSMessage

/**
 * http://docs.ros.org/noetic/api/sensor_msgs/html/msg/CameraInfo.html
 */
data class CompressedImage(val header: Header, val format: String, val data: String): ROSMessage

data class RegionOfInterest( val x_offset : Long, val y_offset : Long,
                             val height : Long, val width: Long, val do_rectify : Boolean): ROSMessage

/**
 * http://docs.ros.org/noetic/api/nav_msgs/html/msg/Odometry.html
 */
data class Vector3( val x : Double, val y : Double, val z : Double): ROSMessage

data class Point( val x : Double, val y : Double, val z : Double): ROSMessage

data class Quaternion( val x : Double, val y : Double, val z : Double, val w : Double): ROSMessage

/**
 * http://docs.ros.org/noetic/api/geometry_msgs/html/msg/Pose.html
 */
data class Pose(val position : Point, val orientation : Quaternion): ROSMessage

data class Twist(val linear : Vector3, val angular : Vector3): ROSMessage

/**
 * http://docs.ros.org/noetic/api/sensor_msgs/html/msg/NavSatStatus.html
 */
data class NavSatStatus(val status : Int, val service : Int): ROSMessage
{
    companion object {

        const val STATUS_NO_FIX   = -1
        const val STATUS_FIX      =  0
        const val STATUS_SBAS_FIX =  1
        const val STATUS_GBAS_FIX =  2

        const val SERVICE_GPS     = 1
        const val SERVICE_GLONASS = 2
        const val SERVICE_COMPASS = 4
        const val SERVICE_GALILEO = 8
    }
}

/**
 * http://docs.ros.org/noetic/api/sensor_msgs/html/msg/NavSatFix.html
 */
data class NavSatFix(val header : Header, val status : NavSatStatus, val latitude : Double, val longitude : Double,
                     val altitude : Double, val position_covariance : DoubleArray,
                     val position_covariance_type : Byte ): ROSMessage
{


    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as NavSatFix

        if (status != other.status) return false
        if (latitude != other.latitude) return false
        if (longitude != other.longitude) return false
        if (altitude != other.altitude) return false
        if (!position_covariance.contentEquals(other.position_covariance)) return false
        if (position_covariance_type != other.position_covariance_type) return false

        return true
    }

    override fun hashCode(): Int {
        var result = status.hashCode()
        result = 31 * result + latitude.hashCode()
        result = 31 * result + longitude.hashCode()
        result = 31 * result + altitude.hashCode()
        result = 31 * result + position_covariance.contentHashCode()
        result = 31 * result + position_covariance_type
        return result
    }
}

//////////////////////// Custom data class messages ////////////////////////

// Corresponding to the flight actions tab in the DJI documentation:
// https://developer.dji.com/api-reference/android-api/Components/FlightController/DJIFlightController.html
enum class FlightActions()
{
    TurnMotorsOn,
    TurnMotorsOff,
    SetUrgentStopModeEnabled,
    SetUrgentStopModeDisabled,
    SetESCBeepEnabled,
    SetESCBeepDisabled,
    StartTakeoff,
    StartPrecisionTakeoff,
    CancelTakeoff,
    StartLanding,
    CancelLanding,
    ConfirmLanding,
    Reboot,

    // Home actions also included here.
    StartGoHome,
    CancelGoHome,
    SetHomeLocationUsingCurrentAircraftLocation;
    // To convert from int to enum!
    // from: https://stackoverflow.com/questions/53523948/how-do-i-create-an-enum-from-a-int-in-kotlin
    companion object {
        private val VALUES = values()
        fun getByValue(value: Int) = VALUES.firstOrNull() { value == it.ordinal }
    }
}

//type: "dji_msgs/AircraftFlightActions"
data class AircraftFlightActions(val flightActions: FlightActions): ROSMessage

// https://developer.dji.com/api-reference/android-api/Components/FlightController/DJIFlightController_DJIVirtualStickFlightControlData.html#djiflightcontroller_djivirtualstickflightcontroldata_constructor_inline
data class AircraftFlightControlData(val roll : Float, val pitch : Float, val yaw : Float, val VThrottle : Float): ROSMessage