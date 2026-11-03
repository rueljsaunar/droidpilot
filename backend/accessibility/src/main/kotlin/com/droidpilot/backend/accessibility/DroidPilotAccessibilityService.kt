package com.droidpilot.backend.accessibility

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.runBlocking
import java.util.concurrent.atomic.AtomicReference

class DroidPilotAccessibilityService : AccessibilityService() {

    private val _snapshot = AtomicReference(UiTreeSnapshot(root = null, timestamp = 0L))
    internal val snapshot: UiTreeSnapshot get() = _snapshot.get()

    private val _eventFlow = MutableSharedFlow<Unit>(replay = 1)
    val eventFlow: SharedFlow<Unit> = _eventFlow.asSharedFlow()

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

    companion object {
        @Volatile
        var instance: DroidPilotAccessibilityService? = null
    }
}
