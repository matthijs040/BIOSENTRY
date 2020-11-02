package com.biosentry.androidbridge

/**
 * https://github.com/RobotWebTools/rosbridge_suite/blob/master/ROSBRIDGE_PROTOCOL.md#343-publish--publish-
 * By default publish to make data classes work as publish messages.
 * typename by default child classname. overridable for non-publish messages.
 */
open class ROSMessage<T>(val op : String = "publish"
                         , val type: String
                         , var topic : String = "bridge/$type"
                         , var msg : T)

/**
 * http://docs.ros.org/noetic/api/std_msgs/html/msg/Header.html
 */
data class time(val sec : Long, val nsec: Long)

data class Header( val seq : Long, val stamp : time, val frame_id : String )

data class CameraInfo( val header: Header, val height: Long, val width: Long,
                       val distortion_model : String, val D : DoubleArray, val K : DoubleArray,
                       val R : DoubleArray, val P : DoubleArray,
                       val binning_x : Long, val binning_y : Long )

/**
 * http://docs.ros.org/noetic/api/sensor_msgs/html/msg/Image.html
 */
data class Image(val header : Header, val height : Long, val width : Long, val encoding : String,
                 val is_bigendian : Byte, val step : Long, val data : String )

/**
 * http://docs.ros.org/noetic/api/sensor_msgs/html/msg/CameraInfo.html
 */
data class CompressedImage(val header: Header, val format: String, val data: String)

data class RegionOfInterest( val x_offset : Long, val y_offset : Long,
                             val height : Long, val width: Long, val do_rectify : Boolean)

/**
 * http://docs.ros.org/noetic/api/nav_msgs/html/msg/Odometry.html
 */
data class Vector3( val x : Double, val y : Double, val z : Double)

data class Point( val x : Double, val y : Double, val z : Double)

data class Quaternion( val x : Double, val y : Double, val z : Double, val w : Double)

/**
 * http://docs.ros.org/noetic/api/geometry_msgs/html/msg/Pose.html
 */
data class Pose( val position : Point, val orientation : Quaternion )

data class Twist(val linear : Vector3, val angular : Vector3)

/**
 * http://docs.ros.org/noetic/api/sensor_msgs/html/msg/NavSatStatus.html
 */
data class NavSatStatus(val status : Int, val service : Int)
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
                     val altitude : Double , val position_covariance : DoubleArray,
                     val position_covariance_type : Byte )
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
