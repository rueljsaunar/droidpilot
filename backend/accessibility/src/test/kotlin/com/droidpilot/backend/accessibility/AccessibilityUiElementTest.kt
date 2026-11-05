package com.droidpilot.backend.accessibility

import com.droidpilot.core.automation.Bounds
import com.droidpilot.core.automation.Selector
import com.droidpilot.core.automation.TimeoutError
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccessibilityUiElementTest {

    private fun makeElement(selector: Selector, snapshot: FakeSnapshot) =
        AccessibilityUiElement(selector, snapshot)

    @Test
    fun `getText returns REDACTED for password field`() = runTest {
        val snapshot = FakeSnapshot(
            SerializedNode(
                text = null, // password text stripped in serialization
                contentDescription = null,
                resourceId = "com.example:id/password",
                className = "android.widget.EditText",
                packageName = "com.example",
                isClickable = true,
                isEnabled = true,
                isPassword = true,
                isChecked = false,
                bounds = Bounds(0, 0, 200, 50),
                indexInParent = 0,
                children = emptyList(),
            )
        )
        val element = makeElement(Selector.ResourceId("password"), snapshot)
        assertEquals("[REDACTED]", element.getText())
    }

    @Test
    fun `exists returns false when selector not in snapshot`() = runTest {
        val snapshot = FakeSnapshot(
            SerializedNode(
                text = "Hello",
                contentDescription = null,
                resourceId = null,
                className = "android.widget.TextView",
                packageName = "com.example",
                isClickable = false,
                isEnabled = true,
                isPassword = false,
                isChecked = false,
                bounds = Bounds(0, 0, 100, 30),
                indexInParent = 0,
                children = emptyList(),
            )
        )
        val element = makeElement(Selector.Text("NonExistent"), snapshot)
        assertFalse(element.exists())
    }

    @Test(expected = TimeoutError::class)
    fun `wait throws TimeoutError when element never appears`() = runTest {
        val snapshot = FakeSnapshot(root = null)
        val element = makeElement(Selector.Text("Missing"), snapshot)
        element.wait(timeoutMs = 100)
    }
}
