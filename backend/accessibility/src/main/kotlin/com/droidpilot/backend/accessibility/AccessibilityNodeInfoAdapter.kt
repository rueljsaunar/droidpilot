package com.droidpilot.backend.accessibility

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import com.droidpilot.core.automation.Bounds

internal class AccessibilityNodeInfoAdapter(
    private val node: AccessibilityNodeInfo,
) : NodeInfo {
    override val text: String? get() = node.text?.toString()
    override val contentDescription: String? get() = node.contentDescription?.toString()
    override val resourceId: String? get() = node.viewIdResourceName
    override val className: String? get() = node.className?.toString()
    override val packageName: String? get() = node.packageName?.toString()
    override val isClickable: Boolean get() = node.isClickable
    override val isEnabled: Boolean get() = node.isEnabled
    override val isPassword: Boolean get() = node.isPassword
    override val isChecked: Boolean get() = node.isChecked
    override val bounds: Bounds get() {
        val r = Rect()
        node.getBoundsInScreen(r)
        return Bounds(r.left, r.top, r.right, r.bottom)
    }
    override val indexInParent: Int get() = -1 // not available on AccessibilityNodeInfo directly
    override val childCount: Int get() = node.childCount
    override fun childAt(index: Int): NodeInfo {
        val child = node.getChild(index)
            ?: error("child at $index is null")
        return AccessibilityNodeInfoAdapter(child)
    }
}
