package com.droidpilot.app

import dagger.hilt.android.HiltAndroidApp
import org.junit.Assert.assertNotNull
import org.junit.Test

class DroidPilotAppTest {
    @Test
    fun `DroidPilotApp is annotated with HiltAndroidApp`() {
        val annotation = DroidPilotApp::class.java.getAnnotation(HiltAndroidApp::class.java)
        assertNotNull("@HiltAndroidApp annotation missing", annotation)
    }
}
