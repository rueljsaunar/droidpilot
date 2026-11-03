package com.droidpilot.backend.accessibility

import com.droidpilot.core.automation.Bounds

internal interface NodeInfo {
    val text: String?
    val contentDescription: String?
    val resourceId: String?
    val className: String?
    val packageName: String?
    val isClickable: Boolean
    val isEnabled: Boolean
    val isPassword: Boolean
    val isChecked: Boolean
    val bounds: Bounds
    val indexInParent: Int
    val childCount: Int
    fun childAt(index: Int): NodeInfo
}
