package com.droidpilot.backend.accessibility

import com.droidpilot.core.automation.BackendUnavailableError
import com.droidpilot.core.automation.Selector
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AccessibilityDeviceActionsTest {

    private val emptySnapshot = FakeSnapshot(root = null)
    private val actions = AccessibilityDeviceActions(emptySnapshot)

    @Test(expected = BackendUnavailableError::class)
    fun `shell throws BackendUnavailableError`() = runTest {
        actions.shell("ls")
    }

    @Test(expected = BackendUnavailableError::class)
    fun `appClear throws BackendUnavailableError`() = runTest {
        actions.appClear("com.example")
    }

    @Test
    fun `find returns AccessibilityUiElement`() {
        val element = actions.find(Selector.Text("anything"))
        assertNotNull(element)
        assertTrue(element is AccessibilityUiElement)
    }
}
