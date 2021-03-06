package com.example.biosentry

/**
 * https://github.com/RobotWebTools/rosbridge_suite/blob/master/ROSBRIDGE_PROTOCOL.md#343-publish--publish-
 * By default publish to make data classes work as publish messages.
 * typename by default child classname. overridable for non-publish messages.
 */
open class ROSMessage<T>(val op : String = "publish"
                         , val type: String
                         , val topic : String = "bridge/$type"
                         , val msg : T)

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
data class NavSatStatus(val status : Byte, val service : Short)

/**
 * http://docs.ros.org/noetic/api/sensor_msgs/html/msg/NavSatFix.html
 */
data class NavSatFix(val status : NavSatStatus, val latitude : Double, val longitude : Double,
                     val altitude : Double , val position_covariance : Array<Double>,
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
