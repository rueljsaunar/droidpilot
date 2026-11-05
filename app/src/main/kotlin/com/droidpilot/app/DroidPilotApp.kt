package com.droidpilot.app

import android.app.Application
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.droidpilot.backend.shizuku.ShizukuRepository
import com.droidpilot.script.python.DeviceProxy
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class DroidPilotApp : Application() {

    @Inject lateinit var shizukuRepository: ShizukuRepository

    override fun onCreate() {
        super.onCreate()
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(this))
        }
        DeviceProxy.init(this)
        shizukuRepository.init()
    }
}
