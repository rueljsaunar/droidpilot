package com.droidpilot.core.automation

public enum class DeviceKey { BACK, HOME, RECENTS }

public interface DeviceActions {
    public suspend fun tap(x: Float, y: Float)
    public suspend fun swipe(startX: Float, startY: Float, endX: Float, endY: Float, durationMs: Long = 300)
    public suspend fun drag(startX: Float, startY: Float, endX: Float, endY: Float, durationMs: Long = 500)
    public suspend fun pinch(centerX: Float, centerY: Float, scale: Float)
    public suspend fun press(key: DeviceKey)
    public suspend fun openNotifications()
    public suspend fun screenshot(filename: String? = null): ByteArray
    public suspend fun windowSize(): Pair<Int, Int>
    public suspend fun currentApp(): String?
    public suspend fun appStart(packageName: String, activity: String? = null)
    public suspend fun appStop(packageName: String)
    public suspend fun appClear(packageName: String)
    public suspend fun appInstall(apkPath: String)
    public suspend fun shell(command: String): ShellResult
    public fun find(selector: Selector): UiElement
    public fun findAll(selector: Selector): List<UiElement>
}
