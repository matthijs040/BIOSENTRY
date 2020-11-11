package com.example.androidbridge.mocks

import com.biosentry.androidbridge.communication.IJSONTranceiver
import com.biosentry.androidbridge.communication.STATE

/**
 * Mock that echoes the last message sent when read.
 */
class JsonTranceiverMock : IJSONTranceiver{
    override var mReceiver: ((String) -> Unit)? = null
    override var mStateHandler: ((STATE) -> Unit)? = null
    private var latestData : String = ""

    override fun send(data: String) {
        latestData = data
        mReceiver?.invoke(data)
    }

    override fun recv(): String {
        return latestData
    }

}