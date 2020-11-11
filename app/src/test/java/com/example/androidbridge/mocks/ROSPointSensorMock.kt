package com.example.androidbridge.mocks

import com.biosentry.androidbridge.communication.AdvertiseMessage
import com.biosentry.androidbridge.communication.IROSSensor
import com.biosentry.androidbridge.communication.Point
import com.biosentry.androidbridge.communication.PublishMessage

class ROSPointSensorMock : IROSSensor {
    override val mMessageTypeName: String = "/geometry_msgs/Point"
    override val mMessageTopicName: String = "/mock/Point"
    override val mAdvertiseMessage: AdvertiseMessage =
        AdvertiseMessage(type =  mMessageTypeName, topic =  mMessageTopicName)

    override var mDataHandler: ((PublishMessage) -> Unit)? = null

    val mReading = PublishMessage(
        topic = mMessageTopicName,
        type =  mMessageTypeName,
        msg = Point( 1.0,2.0,3.0)
    )

    override fun read(): PublishMessage {
        return mReading
    }

}