package com.droidpilot.script.python

import android.content.Context
import com.droidpilot.backend.shizuku.ShizukuRepository
import com.droidpilot.core.automation.DeviceActions
import com.droidpilot.core.automation.DeviceKey
import com.droidpilot.core.automation.ObjectRepository
import com.droidpilot.core.automation.Selector
import com.droidpilot.core.automation.ShellResult
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
        fun shizukuRepository(): ShizukuRepository
        fun objectRepository(): ObjectRepository
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
            ?: entry().deviceActions()

    private val shizuku: ShizukuRepository
        get() = entry().shizukuRepository()

    private val objectRepo: ObjectRepository
        get() = entry().objectRepository()

    private fun entry(): Entry = EntryPointAccessors.fromApplication(
        checkNotNull(appContext) { "DeviceProxy.init(context) was not called" },
        Entry::class.java,
    )

    // ── Element finders ───────────────────────────────────────────────────────

    public fun find(enc: String): UiElementProxy =
        UiElementProxy(parseSelector(enc), actions)

    /** Look up a saved selector by friendly name from the Object Repository. */
    public fun repo(name: String): UiElementProxy {
        val entry = runBlocking {
            check(!isMainThread()) { "DeviceProxy must not be called from the main thread" }
            objectRepo.findByName(name)
        } ?: throw IllegalArgumentException("No repository entry named \"$name\"")
        return UiElementProxy(entry.selector, actions)
    }

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

    // ── Gestures ──────────────────────────────────────────────────────────────

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

    // ── App / window info ────────────────────────────────────────────────────

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

    // ── Shell / app management (Shizuku) ─────────────────────────────────────

    public fun shell(command: String): ShellResult = runBlocking {
        check(!isMainThread()) { "DeviceProxy must not be called from the main thread" }
        shizuku.shell(command)
    }

    public fun appStop(pkg: String): Unit = runBlocking {
        check(!isMainThread()) { "DeviceProxy must not be called from the main thread" }
        shizuku.appStop(pkg)
    }

    public fun appClear(pkg: String): Unit = runBlocking {
        check(!isMainThread()) { "DeviceProxy must not be called from the main thread" }
        shizuku.appClear(pkg)
    }

    public fun appInstall(apkPath: String): Unit = runBlocking {
        check(!isMainThread()) { "DeviceProxy must not be called from the main thread" }
        shizuku.appInstall(apkPath)
    }

    public fun screencap(): ByteArray = runBlocking {
        check(!isMainThread()) { "DeviceProxy must not be called from the main thread" }
        shizuku.screencap()
    }

    public fun logcat(): String = runBlocking {
        check(!isMainThread()) { "DeviceProxy must not be called from the main thread" }
        shizuku.logcat()
    }

    // ── Internal ─────────────────────────────────────────────────────────────

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
