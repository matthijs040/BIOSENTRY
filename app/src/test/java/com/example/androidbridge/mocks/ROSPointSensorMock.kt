package com.example.androidbridge.mocks

import com.biosentry.androidbridge.communication.*

class ROSPointSensorMock : IROSSensor {
    override val mMessageTypeName: String = "/geometry_msgs/Point"
    override val mMessageTopicName: String = "/geometry_msgs/Point"
    override val mAdvertiseMessage: AdvertiseMessage =
        AdvertiseMessage(type =  mMessageTypeName, topic =  mMessageTopicName)

    override var mDataHandler: ((PublishMessage) -> Unit)? = null
    override fun updateData(data: ROSMessage) {
        /* no-op */
    }

    val mReading = PublishMessage(
        topic = mMessageTopicName,
        msg = Point( 1.0,2.0,3.0)
    )

    override fun read(): PublishMessage {
        return mReading
    }

}