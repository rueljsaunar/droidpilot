package com.droidpilot.backend.accessibility

import com.droidpilot.core.automation.Bounds

internal class FakeNode(
    override val text: String? = null,
    override val contentDescription: String? = null,
    override val resourceId: String? = null,
    override val className: String? = null,
    override val packageName: String? = null,
    override val isClickable: Boolean = false,
    override val isEnabled: Boolean = true,
    override val isPassword: Boolean = false,
    override val isChecked: Boolean = false,
    override val bounds: Bounds = Bounds(0, 0, 100, 100),
    override val indexInParent: Int = 0,
    private val children: List<FakeNode> = emptyList(),
) : NodeInfo {
    override val childCount: Int get() = children.size
    override fun childAt(index: Int): NodeInfo = children[index]
}
