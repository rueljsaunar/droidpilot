package com.droidpilot.backend.accessibility

import android.view.accessibility.AccessibilityNodeInfo

internal class NodeHolder private constructor(
    private val node: AccessibilityNodeInfo,
) : AutoCloseable {
    val info: AccessibilityNodeInfo get() = node
    override fun close() { node.recycle() }

    companion object {
        fun of(node: AccessibilityNodeInfo?): NodeHolder? = node?.let { NodeHolder(it) }
        fun require(node: AccessibilityNodeInfo?): NodeHolder =
            of(node) ?: error("AccessibilityNodeInfo was null")
    }
}
