package com.droidpilot.script.python

import com.droidpilot.core.automation.Bounds
import com.droidpilot.core.automation.DeviceActions
import com.droidpilot.core.automation.ElementInfo
import com.droidpilot.core.automation.Selector
import kotlinx.coroutines.runBlocking

public class UiElementProxy(
    private val selector: Selector,
    private val actions: DeviceActions,
) {
    private fun element() = actions.find(selector)

    public fun click(): Unit = runBlocking {
        check(!isMainThread()) { "UiElementProxy must not be called from the main thread" }
        element().click()
    }

    public fun longClick(): Unit = runBlocking {
        check(!isMainThread()) { "UiElementProxy must not be called from the main thread" }
        element().longClick()
    }

    public fun setText(text: String): Unit = runBlocking {
        check(!isMainThread()) { "UiElementProxy must not be called from the main thread" }
        element().setText(text)
    }

    public fun clearText(): Unit = runBlocking {
        check(!isMainThread()) { "UiElementProxy must not be called from the main thread" }
        element().clearText()
    }

    public fun getText(): String? = runBlocking {
        check(!isMainThread()) { "UiElementProxy must not be called from the main thread" }
        element().getText()
    }

    public fun exists(): Boolean = runBlocking {
        check(!isMainThread()) { "UiElementProxy must not be called from the main thread" }
        element().exists()
    }

    /** Waits until the element exists and returns [this] for chaining. */
    public fun wait(timeoutMs: Long = 5_000): UiElementProxy = runBlocking {
        check(!isMainThread()) { "UiElementProxy must not be called from the main thread" }
        element().wait(timeoutMs)
        this@UiElementProxy
    }

    public fun waitGone(timeoutMs: Long = 5_000): Boolean = runBlocking {
        check(!isMainThread()) { "UiElementProxy must not be called from the main thread" }
        element().waitGone(timeoutMs)
    }

    public fun bounds(): IntArray {
        val b: Bounds = element().bounds()
        return intArrayOf(b.left, b.top, b.right, b.bottom)
    }

    public fun info(): Map<String, Any?> {
        val i: ElementInfo = element().info()
        return mapOf(
            "text" to i.text,
            "description" to i.description,
            "resourceId" to i.resourceId,
            "className" to i.className,
            "packageName" to i.packageName,
            "enabled" to i.isEnabled,
            "clickable" to i.isClickable,
            "password" to i.isPassword,
        )
    }

    private fun isMainThread(): Boolean = try {
        android.os.Looper.getMainLooper().isCurrentThread
    } catch (_: RuntimeException) {
        false
    }
}
