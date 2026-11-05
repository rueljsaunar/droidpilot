package com.droidpilot.script.python

import com.droidpilot.core.automation.Bounds
import com.droidpilot.core.automation.DeviceActions
import com.droidpilot.core.automation.DeviceKey
import com.droidpilot.core.automation.ElementInfo
import com.droidpilot.core.automation.Selector
import com.droidpilot.core.automation.ShellResult
import com.droidpilot.core.automation.UiElement
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DeviceProxyTest {
    private val fakeActions = FakeDeviceActions()

    @Before fun setUp() { DeviceProxy.initForTest(fakeActions) }

    @Test fun `find returns proxy that delegates click`() = runBlocking {
        DeviceProxy.find("id:username").click()
        assertTrue(fakeActions.lastClicked == Selector.ResourceId("username"))
    }

    @Test fun `find with colon in resource id parses correctly`() = runBlocking {
        DeviceProxy.find("id:com.example:id/button").exists()
        assertTrue(fakeActions.lastFound == Selector.ResourceId("com.example:id/button"))
    }

    @Test fun `tap delegates x y to actions`() = runBlocking {
        DeviceProxy.tap(100f, 200f)
        assertEquals(100f to 200f, fakeActions.lastTap)
    }

    @Test fun `press HOME delegates correct key`() = runBlocking {
        DeviceProxy.press("HOME")
        assertEquals(DeviceKey.HOME, fakeActions.lastKey)
    }
}

class FakeDeviceActions : DeviceActions {
    var lastClicked: Selector? = null
    var lastFound: Selector? = null
    var lastTap: Pair<Float, Float>? = null
    var lastKey: DeviceKey? = null

    inner class FakeUiElement(val selector: Selector) : UiElement {
        override suspend fun click() { lastClicked = selector }
        override suspend fun longClick() {}
        override suspend fun setText(text: String) {}
        override suspend fun clearText() {}
        override suspend fun getText(): String? = null
        override suspend fun exists(): Boolean { lastFound = selector; return true }
        override suspend fun wait(timeoutMs: Long): UiElement = this
        override suspend fun waitGone(timeoutMs: Long): Boolean = false
        override fun bounds(): Bounds = Bounds(0, 0, 100, 100)
        override fun info(): ElementInfo =
            ElementInfo(null, null, null, null, null, false, true, false, 0, 0, 100, 100)
        override suspend fun scrollTo(selector: Selector): UiElement = this
        override fun child(selector: Selector): UiElement = this
        override fun sibling(selector: Selector): UiElement = this
    }

    override fun find(selector: Selector): UiElement { lastFound = selector; return FakeUiElement(selector) }
    override fun findAll(selector: Selector): List<UiElement> {
        lastFound = selector; return listOf(FakeUiElement(selector))
    }
    override suspend fun tap(x: Float, y: Float) { lastTap = x to y }
    override suspend fun swipe(sx: Float, sy: Float, ex: Float, ey: Float, durationMs: Long) {}
    override suspend fun drag(sx: Float, sy: Float, ex: Float, ey: Float, durationMs: Long) {}
    override suspend fun pinch(cx: Float, cy: Float, scale: Float) {}
    override suspend fun press(key: DeviceKey) { lastKey = key }
    override suspend fun openNotifications() {}
    override suspend fun screenshot(filename: String?): ByteArray = ByteArray(0)
    override suspend fun windowSize(): Pair<Int, Int> = 1080 to 1920
    override suspend fun currentApp(): String? = null
    override suspend fun appStart(packageName: String, activity: String?) {}
    override suspend fun appStop(packageName: String) {}
    override suspend fun appClear(packageName: String) {}
    override suspend fun appInstall(apkPath: String) {}
    override suspend fun shell(command: String): ShellResult = ShellResult("", "", 0)
}
