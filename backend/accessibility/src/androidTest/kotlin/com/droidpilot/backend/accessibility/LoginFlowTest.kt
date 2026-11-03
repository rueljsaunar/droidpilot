package com.droidpilot.backend.accessibility

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.droidpilot.core.automation.Selector
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Drives the :sample-target login flow end-to-end via the AccessibilityService.
 *
 * Prerequisites (set up before running this test):
 *  - DroidPilot accessibility service is enabled in Settings > Accessibility
 *  - com.droidpilot.sampletarget APK is installed on the device
 */
@RunWith(AndroidJUnit4::class)
class LoginFlowTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun `login flow completes successfully`() = runBlocking {
        val service = DroidPilotAccessibilityService.instance
            ?: error("DroidPilot accessibility service is not running. Enable it in Settings > Accessibility.")

        val device = AccessibilityDeviceActions(service)

        // Start sample-target
        device.appStart("com.droidpilot.sampletarget")

        // Wait for username field and fill credentials
        device.find(Selector.ResourceId("username"))
            .wait(timeoutMs = 10_000)
            .setText("qa_user")

        device.find(Selector.ResourceId("password"))
            .setText("secret")

        // Tap the login button
        device.find(Selector.ResourceId("btnLogin")).click()

        // Verify the item list screen appears
        val itemListVisible = device.find(Selector.ResourceId("rvItems"))
            .wait(timeoutMs = 10_000)
            .exists()
        assertTrue("Item list screen should be visible after login", itemListVisible)
    }
}
