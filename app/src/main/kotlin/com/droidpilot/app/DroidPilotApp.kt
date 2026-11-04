package com.droidpilot.app

import android.app.Application
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.droidpilot.script.python.DeviceProxy
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class DroidPilotApp : Application() {
    override fun onCreate() {
        super.onCreate()
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(this))
        }
        DeviceProxy.init(this)
    }
}
