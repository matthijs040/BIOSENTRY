package com.biosentry.androidbridge.communication

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class ROSOdomSensor(    override val mMessageTopicName : String,
                        override val mMessageTypeName : String = "/nav_msgs/Odometry",
                        override val mAdvertiseMessage : AdvertiseMessage = AdvertiseMessage(
                            type = mMessageTypeName,
                            topic = mMessageTopicName
                        ) ) : IROSSensor
{

    fun toRadians(degrees : Double) : Double
    {
        return degrees / ( 2 * PI )
    }

    /**
     * Helper function for conversion from real data to unit quaternion representation
     * From: https://en.wikipedia.org/wiki/Conversion_between_quaternions_and_Euler_angles
     * Note: assumes radian angles: https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.math/cos.html
     */
    fun EulerToUnitQuaternion(r : Double, p : Double, y : Double) : Quaternion
    {
        // Abbreviations for the various angular functions
        val cr = cos(r * 0.5)
        val sr = sin(r * 0.5)
        val cp = cos(p * 0.5)
        val sp = sin(p * 0.5)
        val cy = cos(y * 0.5)
        val sy = sin(y * 0.5)

        return Quaternion(
            w = cr * cp * cy + sr * sp * sy,
            x = sr * cp * cy - cr * sp * sy,
            y = cr * sp * cy + sr * cp * sy,
            z = cr * cp * sy - sr * sp * cy
        )
    }

    private var mReading : PublishMessage =
        PublishMessage(
            topic = mMessageTopicName,
            msg = Odometry(
                header = Header(0,time(0,0),""),
                child_frame_id = "",
                pose = PoseWithCovariance( pose = Pose(Point(0.0,0.0,0.0),
                                    orientation = Quaternion(0.0,0.0,0.0,0.0) ),
                ),
                twist = TwistWithCovariance(twist = Twist(
                    linear = Vector3(0.0,0.0,0.0),
                   angular = Vector3(0.0,0.0,0.0)
                ))
            )
        )

    override var mDataHandler: ((PublishMessage) -> Unit)? = null

    override fun updateData(data: ROSMessage) {
        if(data is Odometry)
        {
            mReading.msg = data
            mDataHandler?.invoke(mReading)

        }
    }

    override fun read(): PublishMessage {
        return mReading
    }


}