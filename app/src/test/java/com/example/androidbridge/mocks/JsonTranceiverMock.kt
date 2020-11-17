package com.example.androidbridge.mocks

import com.biosentry.androidbridge.communication.IJSONTranceiver
import com.biosentry.androidbridge.communication.STATE
import kotlin.reflect.KFunction

/**
 * Mock that echoes the last message sent when read.
 */
class JsonTranceiverMock : IJSONTranceiver{

    private var latestData : String = ""
    override val mReceivers: MutableList<(String) -> Unit> = mutableListOf()
    override val mStateHandlers: MutableList<(STATE) -> Unit> = mutableListOf()

    override fun send(data: String) {
        latestData = data
        mReceivers.forEach {
            it.invoke(data)
        }
    }

    override fun recv(): String {
        return latestData
    }

}