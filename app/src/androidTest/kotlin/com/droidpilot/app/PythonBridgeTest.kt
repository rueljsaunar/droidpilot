package com.droidpilot.app

import androidx.test.platform.app.InstrumentationRegistry
import com.droidpilot.script.python.ScriptRunner
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

// No @HiltAndroidTest needed — DroidPilotApp.onCreate() already called
// Python.start() and DeviceProxy.init() before any test runs.
@RunWith(AndroidJUnit4::class)
class PythonBridgeTest {
    private val ctx = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun hardcodedLoginScriptDrivesSampleTargetEndToEnd() {
        val script = ctx.assets.open("tests/test_sample_target.py")
            .bufferedReader().readText()
        ScriptRunner.execute(script)
        // passes only if no exception is thrown; assertions live inside the Python script
    }
}
