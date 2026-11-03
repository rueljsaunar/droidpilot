package com.droidpilot.backend.accessibility

import android.os.Bundle
import android.view.accessibility.AccessibilityNodeInfo
import com.droidpilot.core.automation.Bounds
import com.droidpilot.core.automation.ElementInfo
import com.droidpilot.core.automation.ElementNotFoundError
import com.droidpilot.core.automation.Selector
import com.droidpilot.core.automation.TimeoutError
import com.droidpilot.core.automation.UiElement
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

private const val POLL_INTERVAL_MS = 100L

internal class AccessibilityUiElement(
    private val selector: Selector,
    private val provider: SnapshotProvider,
) : UiElement {

    override suspend fun exists(): Boolean =
        try {
            SelectorEngine.find(selector, provider.snapshot.root ?: return false)
            true
        } catch (_: ElementNotFoundError) {
            false
        }

    override fun bounds(): Bounds {
        val node = resolveNode()
        return node.bounds
    }

    override fun info(): ElementInfo {
        val node = resolveNode()
        return ElementInfo(
            text = if (node.isPassword) "[REDACTED]" else node.text,
            description = node.contentDescription,
            resourceId = node.resourceId,
            className = node.className,
            packageName = node.packageName,
            isClickable = node.isClickable,
            isEnabled = node.isEnabled,
            isPassword = node.isPassword,
            boundsLeft = node.bounds.left,
            boundsTop = node.bounds.top,
            boundsRight = node.bounds.right,
            boundsBottom = node.bounds.bottom,
        )
    }

    override suspend fun getText(): String? {
        val node = resolveNode()
        if (node.isPassword) return "[REDACTED]"
        return node.text
    }

    override suspend fun click() = performNodeAction(AccessibilityNodeInfo.ACTION_CLICK)

    override suspend fun longClick() = performNodeAction(AccessibilityNodeInfo.ACTION_LONG_CLICK)

    override suspend fun setText(text: String) {
        val service = requireService()
        val liveNode = service.findLiveNode(selector)
            ?: throw ElementNotFoundError(selector)
        NodeHolder.require(liveNode).use { holder ->
            val bundle = Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
            }
            holder.info.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, bundle)
        }
    }

    override suspend fun clearText() = setText("")

    override suspend fun wait(timeoutMs: Long): UiElement {
        val succeeded = withTimeoutOrNull(timeoutMs) {
            while (true) {
                withTimeoutOrNull(POLL_INTERVAL_MS) { provider.eventFlow.first() }
                if (exists()) return@withTimeoutOrNull
            }
        }
        if (!exists()) throw TimeoutError(selector, timeoutMs)
        return this
    }

    override suspend fun waitGone(timeoutMs: Long): Boolean {
        if (!exists()) return true
        return withTimeoutOrNull(timeoutMs) {
            while (true) {
                withTimeoutOrNull(POLL_INTERVAL_MS) { provider.eventFlow.first() }
                if (!exists()) return@withTimeoutOrNull
            }
        } != null
    }

    override suspend fun scrollTo(selector: Selector): UiElement {
        val service = requireService()
        val scrollable = service.findLiveNode(Selector.Compound(listOf(this.selector)))
        NodeHolder.of(scrollable)?.use { holder ->
            holder.info.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
        }
        return AccessibilityUiElement(selector, provider)
    }

    override fun child(selector: Selector): UiElement =
        AccessibilityUiElement(Selector.Child(parent = this.selector, child = selector), provider)

    override fun sibling(selector: Selector): UiElement =
        AccessibilityUiElement(Selector.Sibling(anchor = this.selector, sibling = selector), provider)

    private fun resolveNode(): SerializedNode =
        SelectorEngine.find(selector, provider.snapshot.root ?: throw ElementNotFoundError(selector))

    private suspend fun performNodeAction(action: Int) {
        val service = requireService()
        val liveNode = service.findLiveNode(selector)
            ?: throw ElementNotFoundError(selector)
        NodeHolder.require(liveNode).use { holder ->
            holder.info.performAction(action)
        }
    }

    private fun requireService(): DroidPilotAccessibilityService =
        (provider as? DroidPilotAccessibilityService)
            ?: DroidPilotAccessibilityService.instance
            ?: throw com.droidpilot.core.automation.BackendUnavailableError(
                backend = "Accessibility",
                setupHint = "Enable DroidPilot accessibility service in Settings > Accessibility",
            )
}
