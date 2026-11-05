package com.droidpilot.backend.accessibility

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.droidpilot.core.automation.Selector
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.runBlocking
import java.util.concurrent.atomic.AtomicReference

internal class DroidPilotAccessibilityService : AccessibilityService(), SnapshotProvider {

    private val _snapshot = AtomicReference(UiTreeSnapshot(root = null, timestamp = 0L))
    override val snapshot: UiTreeSnapshot get() = _snapshot.get()

    private val _eventFlow = MutableSharedFlow<Unit>(replay = 1)
    override val eventFlow: SharedFlow<Unit> = _eventFlow.asSharedFlow()

    override fun onServiceConnected() {
        instance = this
        updateSnapshot()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
            AccessibilityEvent.TYPE_WINDOWS_CHANGED -> {
                updateSnapshot()
                runBlocking { _eventFlow.emit(Unit) }
            }
            AccessibilityEvent.TYPE_VIEW_CLICKED,
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> recorder?.onAccessibilityEvent(event)
        }
    }

    override fun onInterrupt() {
        instance = null
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }

    fun updateSnapshot() {
        val root = getRootInActiveWindow()
        NodeHolder.of(root)?.use { holder ->
            val serialized = UiTreeSnapshot.serialize(AccessibilityNodeInfoAdapter(holder.info))
            _snapshot.set(UiTreeSnapshot(root = serialized, timestamp = System.currentTimeMillis()))
        } ?: _snapshot.set(UiTreeSnapshot(root = null, timestamp = System.currentTimeMillis()))
    }

    /**
     * Resolves [selector] against the current snapshot to find a matching [SerializedNode],
     * then walks the live tree to return the corresponding [AccessibilityNodeInfo].
     * Caller owns the returned node and must recycle it (use [NodeHolder]).
     */
    fun findLiveNode(selector: Selector): AccessibilityNodeInfo? {
        val snapshot = _snapshot.get().root ?: return null
        // Verify the selector matches something in the snapshot
        val matched = try {
            SelectorEngine.find(selector, snapshot)
        } catch (_: com.droidpilot.core.automation.ElementNotFoundError) {
            return null
        }
        // Find the live node by resourceId > text > class fallback
        val root = getRootInActiveWindow() ?: return null
        return findInLiveTree(root, matched).also { if (it == null) root.recycle() }
    }

    private fun findInLiveTree(
        node: AccessibilityNodeInfo,
        target: SerializedNode,
    ): AccessibilityNodeInfo? {
        val matches = when {
            target.resourceId != null -> node.viewIdResourceName == target.resourceId
            target.text != null -> node.text?.toString() == target.text
            else -> node.className?.toString() == target.className
        }
        if (matches) return node
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findInLiveTree(child, target)
            if (found != null) {
                if (found !== child) child.recycle()
                return found
            }
            child.recycle()
        }
        return null
    }

    companion object {
        @Volatile
        var instance: DroidPilotAccessibilityService? = null

        @Volatile
        var recorder: AccessibilityActionRecorder? = null
    }
}
