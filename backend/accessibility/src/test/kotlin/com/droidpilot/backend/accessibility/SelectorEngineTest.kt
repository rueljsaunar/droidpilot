package com.droidpilot.backend.accessibility

import com.droidpilot.core.automation.Bounds
import com.droidpilot.core.automation.ElementNotFoundError
import com.droidpilot.core.automation.Selector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SelectorEngineTest {

    private fun node(
        text: String? = null,
        contentDescription: String? = null,
        resourceId: String? = null,
        className: String? = "android.view.View",
        packageName: String? = "com.example",
        isClickable: Boolean = false,
        isEnabled: Boolean = true,
        isPassword: Boolean = false,
        isChecked: Boolean = false,
        bounds: Bounds = Bounds(0, 0, 100, 100),
        indexInParent: Int = 0,
        children: List<SerializedNode> = emptyList(),
    ) = SerializedNode(
        text, contentDescription, resourceId, className, packageName,
        isClickable, isEnabled, isPassword, isChecked, bounds, indexInParent, children,
    )

    // Fixture 1 — Login screen (shallow)
    private val loginScreen = node(
        className = "android.widget.FrameLayout",
        children = listOf(
            node(text = "Username", resourceId = "com.example:id/username", className = "android.widget.EditText"),
            node(resourceId = "com.example:id/password", className = "android.widget.EditText", isPassword = true),
            node(text = "Login", resourceId = "com.example:id/btnLogin", className = "android.widget.Button", isClickable = true),
            node(text = "Forgot Password?", className = "android.widget.TextView", isClickable = true),
        ),
    )

    // Fixture 2 — Item list (RecyclerView + children)
    private val itemList = node(
        className = "android.widget.FrameLayout",
        children = listOf(
            node(
                className = "androidx.recyclerview.widget.RecyclerView",
                children = listOf(
                    node(text = "Item 0", className = "android.widget.TextView", indexInParent = 0),
                    node(text = "Item 1", className = "android.widget.TextView", indexInParent = 1),
                    node(text = "Item 2", className = "android.widget.TextView", indexInParent = 2),
                    node(text = "Item 3", className = "android.widget.TextView", indexInParent = 3),
                ),
            ),
            node(text = "Button A", className = "android.widget.Button"),
            node(text = "Button B", className = "android.widget.Button"),
        ),
    )

    // Fixture 3 — Nested containers
    private val nestedContainers = node(
        className = "android.widget.LinearLayout",
        children = listOf(
            node(
                className = "android.widget.LinearLayout",
                children = listOf(
                    node(text = "Inner Label", className = "android.widget.TextView"),
                    node(text = "Inner Button", className = "android.widget.Button", isClickable = true),
                ),
            ),
            node(text = "Outer Text", className = "android.widget.TextView"),
        ),
    )

    // Fixture 4 — Multi-package tree
    private val multiPackage = node(
        className = "android.widget.FrameLayout",
        packageName = "com.host",
        children = listOf(
            node(text = "Host View", contentDescription = "host-desc", packageName = "com.host", isEnabled = true),
            node(text = "Guest View", packageName = "com.guest", isEnabled = false),
            node(contentDescription = "Settings", packageName = "com.host", isEnabled = true),
        ),
    )

    // Fixture 6 — Sibling tree
    private val siblingTree = node(
        className = "android.widget.LinearLayout",
        children = listOf(
            node(text = "Label", className = "android.widget.TextView"),
            node(text = "Value", className = "android.widget.TextView"),
            node(text = "OtherLabel", className = "android.widget.TextView"),
            node(text = "OtherValue", className = "android.widget.TextView"),
        ),
    )

    // ── Fixture 1 ────────────────────────────────────────────────────────────

    @Test
    fun `ResourceId finds by short id`() {
        val result = SelectorEngine.find(Selector.ResourceId("btnLogin"), loginScreen)
        assertEquals("com.example:id/btnLogin", result.resourceId)
    }

    @Test
    fun `Text finds exact match`() {
        val result = SelectorEngine.find(Selector.Text("Login"), loginScreen)
        assertEquals("Login", result.text)
    }

    @Test
    fun `ClassName finds button`() {
        val result = SelectorEngine.find(Selector.ClassName("android.widget.Button"), loginScreen)
        assertEquals("android.widget.Button", result.className)
    }

    @Test
    fun `Clickable true finds clickable nodes`() {
        val result = SelectorEngine.find(Selector.Clickable(true), loginScreen)
        assertTrue(result.isClickable)
    }

    // ── Fixture 2 ────────────────────────────────────────────────────────────

    @Test
    fun `Index finds node by position in parent`() {
        val recycler = SelectorEngine.find(Selector.ClassName("androidx.recyclerview.widget.RecyclerView"), itemList)
        val result = SelectorEngine.find(Selector.Index(2), recycler)
        assertEquals("Item 2", result.text)
    }

    @Test
    fun `Instance finds nth global match`() {
        val buttons = SelectorEngine.findAll(Selector.ClassName("android.widget.Button"), itemList)
        assertEquals(2, buttons.size)
        assertTrue(buttons[0].text == "Button A")
        assertTrue(buttons[1].text == "Button B")
        // Compound with Instance(1) picks the 2nd Button (0-indexed)
        val second = SelectorEngine.find(
            Selector.Compound(listOf(Selector.ClassName("android.widget.Button"), Selector.Instance(1))),
            itemList,
        )
        assertTrue(second.text == "Button B")
    }

    @Test
    fun `TextContains finds substring`() {
        val result = SelectorEngine.find(Selector.TextContains("Item"), itemList)
        assertTrue(result.text?.contains("Item") == true)
    }

    @Test
    fun `TextMatches finds regex match`() {
        val result = SelectorEngine.find(Selector.TextMatches("Item [0-9]"), itemList)
        assertTrue(result.text?.matches(Regex("Item [0-9]")) == true)
    }

    // ── Fixture 3 ────────────────────────────────────────────────────────────

    @Test
    fun `Child finds node within parent scope`() {
        val result = SelectorEngine.find(
            Selector.Child(
                parent = Selector.ClassName("android.widget.LinearLayout"),
                child = Selector.ClassName("android.widget.Button"),
            ),
            nestedContainers,
        )
        assertEquals("android.widget.Button", result.className)
        assertEquals("Inner Button", result.text)
    }

    @Test
    fun `Compound AND-matches multiple conditions`() {
        val result = SelectorEngine.find(
            Selector.Compound(listOf(Selector.ClassName("android.widget.Button"), Selector.Clickable(true))),
            nestedContainers,
        )
        assertEquals("Inner Button", result.text)
        assertTrue(result.isClickable)
    }

    // ── Fixture 4 ────────────────────────────────────────────────────────────

    @Test
    fun `Package matches by package name`() {
        val result = SelectorEngine.find(Selector.Package("com.guest"), multiPackage)
        assertEquals("com.guest", result.packageName)
    }

    @Test
    fun `Description matches contentDescription`() {
        val result = SelectorEngine.find(Selector.Description("Settings"), multiPackage)
        assertEquals("Settings", result.contentDescription)
    }

    @Test
    fun `Enabled false filters disabled nodes`() {
        val result = SelectorEngine.find(
            Selector.Compound(listOf(Selector.Package("com.guest"), Selector.Enabled(false))),
            multiPackage,
        )
        assertTrue(!result.isEnabled)
    }

    // ── Fixture 5 error paths ─────────────────────────────────────────────────

    @Test(expected = ElementNotFoundError::class)
    fun `find throws ElementNotFoundError when not found`() {
        SelectorEngine.find(Selector.Text("NoSuchElement"), loginScreen)
    }

    @Test(expected = UnsupportedOperationException::class)
    fun `XPath throws UnsupportedOperationException`() {
        SelectorEngine.find(Selector.XPath("//Button"), loginScreen)
    }

    // ── Fixture 6 sibling ─────────────────────────────────────────────────────

    @Test
    fun `Sibling finds node adjacent to anchor`() {
        val result = SelectorEngine.find(
            Selector.Sibling(anchor = Selector.Text("Label"), sibling = Selector.Text("Value")),
            siblingTree,
        )
        assertEquals("Value", result.text)
    }
}
