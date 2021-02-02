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

    protected var mSeq = 0L
    protected val mTimeInSeconds : Long
    get() { return System.currentTimeMillis() / 1000 }
    protected val mTimeInNanos : Long
    get() { return System.currentTimeMillis() * 1000 }

    protected val mFrameID = "phone_camera"


    private var mReading : PublishMessage =
        PublishMessage(
            //type = mMessageTypeName,
            topic = mMessageTopicName,
            msg = CompressedImage(
                header = Header(mSeq, time(mTimeInSeconds, mTimeInNanos ), mFrameID),
                format = "jpeg",
                ByteArray(0)
            )
        )


    override fun read(): PublishMessage {
        return mReading
    }
}