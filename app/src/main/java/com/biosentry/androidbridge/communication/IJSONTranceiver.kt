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
    val mReceivers : MutableList< ( (String) -> Unit) >
    val mStateHandlers : MutableList< ( (STATE) -> Unit) >

    fun send(data : String)
    fun recv() : String

    fun attachReceiver( receiver : ((String) -> Unit) )
    { mReceivers.add(receiver) }

    fun detachReceiver( receiver : ((String) -> Unit)  ) : Boolean
    { return mReceivers.remove(receiver) }

    fun invokeReceivers( data : String)
    { mReceivers.forEach{ it.invoke(data) } }

    fun attachHandler( handler : ((STATE) -> Unit) )
    { mStateHandlers.add(handler) }

    fun detachHandler( handler : ((STATE) -> Unit) ) : Boolean
    { return mStateHandlers.remove(handler) }

    fun invokeHandlers( state : STATE)
    { mStateHandlers.forEach { it.invoke(state) } }
}