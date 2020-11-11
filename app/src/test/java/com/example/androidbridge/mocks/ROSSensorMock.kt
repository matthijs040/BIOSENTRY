package com.example.androidbridge.mocks

import com.biosentry.androidbridge.communication.*
import java.util.*
import kotlin.concurrent.timerTask

/**
 * Mock that sends a const NavSatFix message on request or once per timer instance.
 * If the datahandler is used.
 */
class ROSSensorMock() : IROSSensor
{
    override val mMessageTypeName: String = "sensor_msgs/NavSatFix"
    override val mMessageTopicName: String = "android/mock/GPS"
    override val mAdvertiseMessage: AdvertiseMessage =
        AdvertiseMessage(topic = mMessageTopicName, type = mMessageTypeName )

    private val mTimer = Timer()

    override var mDataHandler: ((PublishMessage) -> Unit)? = null
    set(value){
        if(value != null)
            mTimer.schedule( timerTask{

                mDataHandler?.invoke(read())
            }, 1000, 1000)
        else
            mTimer.purge()

        field = value
    }

    private val mHeader = Header(1, time(2,3), "N.A.")
    private val mStatus = NavSatStatus(
        NavSatStatus.STATUS_NO_FIX,
        NavSatStatus.SERVICE_GLONASS + NavSatStatus.SERVICE_GPS
    )
    private val  mReading : NavSatFix = NavSatFix ( mHeader,
        mStatus,
        0.0,
        0.0,
        0.0,
        DoubleArray(9),
        0 )

    override fun read(): PublishMessage {
        return PublishMessage(type =  mMessageTypeName, topic = mMessageTopicName, msg = mReading)
    }

}