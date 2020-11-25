package com.biosentry.androidbridge.phone

import com.biosentry.androidbridge.communication.*

class PhoneLoopback : IROSDevice, IROSSensor {
    private val topicPrefix = "/android/phone"

    override val mMessageTypeName: String = "/geometry_msgs/Point"
    override val mMessageTopicName: String = "$topicPrefix/loopback"

    override val mAdvertiseMessage: AdvertiseMessage = AdvertiseMessage(
        topic = mMessageTopicName,
        type = mMessageTypeName
    )

    private val mPublishMessage = PublishMessage(
        topic = mMessageTopicName,
        msg = Point(0.0,0.0,0.0)
    )

    override var mDataHandler: ((PublishMessage) -> Unit)? = null

    override val mControls: List<ROSControl> = listOf(
        ROSControl(SubscribeMessage(type = mMessageTypeName, topic = "$mMessageTopicName$mMessageTypeName"), ::updateData)
    )

    override fun updateData(data: ROSMessage) {
        mDataHandler?.invoke(mPublishMessage)
    }

    override fun read(): PublishMessage {
        return mPublishMessage
    }
}