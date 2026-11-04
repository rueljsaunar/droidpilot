package com.droidpilot.script.python

import android.content.Context
import com.droidpilot.core.automation.DeviceActions
import com.droidpilot.core.automation.DeviceKey
import com.droidpilot.core.automation.Selector
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.runBlocking

public object DeviceProxy {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Entry {
        fun deviceActions(): DeviceActions
    }

    @Volatile private var appContext: Context? = null
    @Volatile private var testActions: DeviceActions? = null

    internal fun initForTest(a: DeviceActions) {
        testActions = a
    }

    public fun init(ctx: Context) {
        appContext = ctx.applicationContext
    }

    private val actions: DeviceActions
        get() = testActions
            ?: EntryPointAccessors.fromApplication(
                checkNotNull(appContext) { "DeviceProxy.init(context) was not called" },
                Entry::class.java,
            ).deviceActions()

    public fun find(enc: String): UiElementProxy =
        UiElementProxy(parseSelector(enc), actions)

    public fun findAll(enc: String): List<UiElementProxy> {
        val base = parseSelector(enc)
        val count = runBlocking {
            check(!isMainThread()) { "DeviceProxy must not be called from the main thread" }
            actions.findAll(base).size
        }
        return List(count) { i ->
            UiElementProxy(Selector.Compound(listOf(base, Selector.Instance(i))), actions)
        }
    }

    public fun tap(x: Float, y: Float): Unit = runBlocking {
        check(!isMainThread()) { "DeviceProxy must not be called from the main thread" }
        actions.tap(x, y)
    }

    public fun swipe(sx: Float, sy: Float, ex: Float, ey: Float, durationMs: Long = 300): Unit = runBlocking {
        check(!isMainThread()) { "DeviceProxy must not be called from the main thread" }
        actions.swipe(sx, sy, ex, ey, durationMs)
    }

    public fun drag(sx: Float, sy: Float, ex: Float, ey: Float, durationMs: Long = 500): Unit = runBlocking {
        check(!isMainThread()) { "DeviceProxy must not be called from the main thread" }
        actions.drag(sx, sy, ex, ey, durationMs)
    }

    public fun pinch(cx: Float, cy: Float, scale: Float): Unit = runBlocking {
        check(!isMainThread()) { "DeviceProxy must not be called from the main thread" }
        actions.pinch(cx, cy, scale)
    }

    public fun press(key: String): Unit = runBlocking {
        check(!isMainThread()) { "DeviceProxy must not be called from the main thread" }
        val deviceKey = when (key.uppercase()) {
            "BACK" -> DeviceKey.BACK
            "HOME" -> DeviceKey.HOME
            "RECENTS" -> DeviceKey.RECENTS
            else -> throw IllegalArgumentException("Unknown key: $key. Valid: BACK, HOME, RECENTS")
        }
        actions.press(deviceKey)
    }

    public fun appStart(pkg: String, activity: String? = null): Unit = runBlocking {
        check(!isMainThread()) { "DeviceProxy must not be called from the main thread" }
        actions.appStart(pkg, activity)
    }

    public fun windowSize(): IntArray = runBlocking {
        check(!isMainThread()) { "DeviceProxy must not be called from the main thread" }
        val (w, h) = actions.windowSize()
        intArrayOf(w, h)
    }

    public fun currentApp(): String? = runBlocking {
        check(!isMainThread()) { "DeviceProxy must not be called from the main thread" }
        actions.currentApp()
    }

    private fun parseSelector(enc: String): Selector {
        val colon = enc.indexOf(':')
        require(colon > 0) { "Invalid selector encoding: '$enc'. Expected '<type>:<value>'." }
        val type = enc.substring(0, colon)
        val value = enc.substring(colon + 1)
        return when (type) {
            "id" -> Selector.ResourceId(value)
            "text" -> Selector.Text(value)
            "text_contains" -> Selector.TextContains(value)
            "text_matches" -> Selector.TextMatches(value)
            "desc" -> Selector.Description(value)
            "xpath" -> Selector.XPath(value)
            "class" -> Selector.ClassName(value)
            "pkg" -> Selector.Package(value)
            else -> throw IllegalArgumentException("Unknown selector type: '$type'")
        }
    }

    private fun isMainThread(): Boolean = try {
        android.os.Looper.getMainLooper().isCurrentThread
    } catch (_: RuntimeException) {
        false
    }
}
