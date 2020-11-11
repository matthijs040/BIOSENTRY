package com.biosentry.androidbridge.communication

enum class STATE
{
    NOT_CONNECTED,
    CONNECTED
}

/**
 * Interface for a class that can send and receive JSON.
 */
interface IJSONTranceiver {
    var mReceiver   : ( (String) -> Unit)?
    var mStateHandler : ((STATE) -> Unit)?

    fun send(data : String)
    fun recv() : String
}