package com.droidpilot.core.automation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SelectorTest {

    @Test
    fun `all selector subtypes instantiate`() {
        val selectors: List<Selector> = listOf(
            Selector.Text("Login"),
            Selector.TextContains("Log"),
            Selector.TextMatches("Log.*"),
            Selector.ResourceId("com.example:id/btn"),
            Selector.Description("Login button"),
            Selector.ClassName("android.widget.Button"),
            Selector.Package("com.example"),
            Selector.Clickable(true),
            Selector.Enabled(true),
            Selector.Index(0),
            Selector.Instance(1),
            Selector.XPath("//Button[@text='Login']"),
            Selector.Child(Selector.ClassName("LinearLayout"), Selector.Text("OK")),
            Selector.Sibling(Selector.Text("Label"), Selector.ClassName("EditText")),
            Selector.Compound(listOf(Selector.Text("Login"), Selector.Clickable(true))),
        )
        assertEquals(15, selectors.size)
    }

    @Test
    fun `Compound flattens nested compounds`() {
        val inner = Selector.Compound(listOf(Selector.Text("A"), Selector.Clickable(true)))
        val outer = Selector.Compound(listOf(inner, Selector.ClassName("Button")))
        val flat = outer.flatten()
        assertEquals(3, flat.size)
        assertTrue(flat.none { it is Selector.Compound })
    }

    @Test
    fun `AutomationException subtypes carry messages`() {
        val e1 = ElementNotFoundError(Selector.Text("Missing"))
        assertTrue(e1.message!!.contains("Missing"))

        val e2 = BackendUnavailableError("Shizuku", "Enable Wireless Debugging")
        assertTrue(e2.message!!.contains("Shizuku"))
        assertTrue(e2.setupHint.contains("Wireless Debugging"))

        val e3 = TargetCrashedError("com.example.app")
        assertTrue(e3.message!!.contains("com.example.app"))
    }
}
