package com.biosentry.androidbridge

import android.app.Application
import android.content.Context
import com.secneo.sdk.Helper


class MApplication : Application()
{
    override fun attachBaseContext(paramContext: Context)
    {
        super.attachBaseContext(paramContext)
        Helper.install(this)
    }
}