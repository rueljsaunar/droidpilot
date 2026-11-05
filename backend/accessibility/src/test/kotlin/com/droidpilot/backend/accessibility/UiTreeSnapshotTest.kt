package com.droidpilot.backend.accessibility

import com.droidpilot.core.automation.Bounds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UiTreeSnapshotTest {

    @Test
    fun `serializes node text and resourceId`() {
        val node = FakeNode(
            text = "Hello",
            resourceId = "com.example:id/btnLogin",
            isPassword = false,
        )
        val result = UiTreeSnapshot.serialize(node)
        assertEquals("Hello", result.text)
        assertEquals("com.example:id/btnLogin", result.resourceId)
    }

    @Test
    fun `serializes isPassword flag and nulls out text`() {
        val node = FakeNode(
            text = "secret123",
            resourceId = "com.example:id/password",
            isPassword = true,
        )
        val result = UiTreeSnapshot.serialize(node)
        assertTrue(result.isPassword)
        assertNull("password text must be null in serialized tree", result.text)
    }

    @Test
    fun `serializes children recursively`() {
        val child1 = FakeNode(text = "Child1")
        val child2 = FakeNode(text = "Child2")
        val parent = FakeNode(text = "Parent", children = listOf(child1, child2))
        val result = UiTreeSnapshot.serialize(parent)
        assertTrue(result.children.size == 2)
        assertTrue(result.children[0].text == "Child1")
        assertTrue(result.children[1].text == "Child2")
    }
}
