package com.example.androidbridge.mocks

import com.biosentry.androidbridge.communication.*

class ROSTwistSensorMock : IROSSensor{
    override val mMessageTypeName: String = "/geometry_msgs/Twist"
    override val mMessageTopicName: String = "/geometry_msgs/Twist"
    override val mAdvertiseMessage: AdvertiseMessage =
        AdvertiseMessage(type =  mMessageTypeName, topic =  mMessageTopicName)

    override var mDataHandler: ((PublishMessage) -> Unit)? = null

    val mReading = PublishMessage(
        topic = mMessageTopicName,
        msg = Twist(    Vector3(1.0, 2.0, 3.0 ),
                        Vector3(4.0,5.0,6.0 ) )
    )

    override fun read(): PublishMessage {
        return mReading
    }
}