package com.biosentry.androidbridge.communication

import com.biosentry.androidbridge.BuildConfig


open class ROSCamera(
    final override val mMessageTopicName : String,
    override val mMessageTypeName: String = "/sensor_msgs/Image",
    override val mAdvertiseMessage : AdvertiseMessage =
        AdvertiseMessage(
            type = mMessageTypeName,
            topic = mMessageTopicName ) ) : IROSSensor
{
    override var mDataHandler: ((PublishMessage) -> Unit)? = null
    override fun updateData(data: ROSMessage) {
        if (BuildConfig.DEBUG && data !is CompressedImage) {
            error("Assertion failed")
        }
        mReading.msg = data
        mDataHandler?.invoke(mReading)
        mSeq++
    }

    private var mSeq = 0L
    private var mTimeInSeconds = System.currentTimeMillis() / 1000
    private var mTimeInNanos = System.currentTimeMillis() * 1000
    private val mFrameID = "phone_camera"


    private var mReading : PublishMessage =
        PublishMessage(
            //type = mMessageTypeName,
            topic = mMessageTopicName,
            msg = CompressedImage(
                header = Header(mSeq, time(mTimeInSeconds, mTimeInNanos ), mFrameID),
                format = "png",
                ByteArray(0)
            )
        )


    override fun read(): PublishMessage {
        return mReading
    }
}