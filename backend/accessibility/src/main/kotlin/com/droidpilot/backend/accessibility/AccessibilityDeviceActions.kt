package com.droidpilot.backend.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Build
import android.view.WindowManager
import com.droidpilot.core.automation.BackendUnavailableError
import com.droidpilot.core.automation.DeviceActions
import com.droidpilot.core.automation.DeviceKey
import com.droidpilot.core.automation.Selector
import com.droidpilot.core.automation.ShellResult
import com.droidpilot.core.automation.UiElement
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

private const val TAP_DURATION_MS = 100L

@Singleton
class AccessibilityDeviceActions private constructor(
    private val context: Context?,
    private val overrideProvider: SnapshotProvider?,
) : DeviceActions {

    @Inject constructor(@ApplicationContext context: Context) : this(context, null)

    // For unit testing without an application context
    internal constructor(provider: SnapshotProvider) : this(null, provider)

    private val service: DroidPilotAccessibilityService
        get() = DroidPilotAccessibilityService.instance
            ?: throw BackendUnavailableError(
                backend = "Accessibility",
                setupHint = "Enable DroidPilot accessibility service in Settings > Accessibility",
            )

    private val provider: SnapshotProvider
        get() = overrideProvider ?: service

    // ── Gestures ─────────────────────────────────────────────────────────────

    override suspend fun tap(x: Float, y: Float) {
        val path = Path().apply { moveTo(x, y) }
        dispatchGesture(GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, TAP_DURATION_MS))
            .build())
    }

    override suspend fun swipe(startX: Float, startY: Float, endX: Float, endY: Float, durationMs: Long) {
        val path = Path().apply { moveTo(startX, startY); lineTo(endX, endY) }
        dispatchGesture(GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, durationMs))
            .build())
    }

    override suspend fun drag(startX: Float, startY: Float, endX: Float, endY: Float, durationMs: Long) =
        swipe(startX, startY, endX, endY, durationMs)

    override suspend fun pinch(centerX: Float, centerY: Float, scale: Float) {
        val offset = 100f
        val inward = scale < 1f
        val startOffset = if (inward) offset * scale else offset
        val endOffset = if (inward) offset else offset * scale
        val path1 = Path().apply { moveTo(centerX - startOffset, centerY); lineTo(centerX - endOffset, centerY) }
        val path2 = Path().apply { moveTo(centerX + startOffset, centerY); lineTo(centerX + endOffset, centerY) }
        val builder = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path1, 0, 300))
            .addStroke(GestureDescription.StrokeDescription(path2, 0, 300))
        dispatchGesture(builder.build())
    }

    // ── Navigation ───────────────────────────────────────────────────────────

    override suspend fun press(key: DeviceKey) {
        val action = when (key) {
            DeviceKey.BACK -> AccessibilityService.GLOBAL_ACTION_BACK
            DeviceKey.HOME -> AccessibilityService.GLOBAL_ACTION_HOME
            DeviceKey.RECENTS -> AccessibilityService.GLOBAL_ACTION_RECENTS
        }
        service.performGlobalAction(action)
    }

    override suspend fun openNotifications() {
        service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS)
    }

    // ── Window / app info ─────────────────────────────────────────────────────

    override suspend fun windowSize(): Pair<Int, Int> {
        val ctx = context ?: throw BackendUnavailableError("Accessibility", "No application context")
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val wm = ctx.getSystemService(WindowManager::class.java)
            val bounds = wm.currentWindowMetrics.bounds
            bounds.width() to bounds.height()
        } else {
            @Suppress("DEPRECATION")
            val dm = ctx.resources.displayMetrics
            dm.widthPixels to dm.heightPixels
        }
    }

    override suspend fun currentApp(): String? {
        val node = service.rootInActiveWindow ?: return null
        return NodeHolder.require(node).use { it.info.packageName?.toString() }
    }

    override suspend fun screenshot(filename: String?): ByteArray {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            throw BackendUnavailableError(
                backend = "Accessibility",
                setupHint = "Screenshot requires Android 11+ (API 30) or Shizuku (Phase 6)",
            )
        }
        throw BackendUnavailableError(
            backend = "Accessibility",
            setupHint = "Screenshot via AccessibilityService will be wired in Phase 5 final polish",
        )
    }

    // ── Shell / app management — not available without Shizuku ───────────────

    override suspend fun shell(command: String): ShellResult =
        throw BackendUnavailableError(
            backend = "Shell",
            setupHint = "shell() requires Shizuku (Phase 6). Install and authorize Shizuku first.",
        )

    override suspend fun appStart(packageName: String, activity: String?) {
        val ctx = context ?: throw BackendUnavailableError("Accessibility", "No application context")
        val intent = ctx.packageManager.getLaunchIntentForPackage(packageName)
            ?: throw BackendUnavailableError("Accessibility", "Cannot launch $packageName — not installed")
        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        ctx.startActivity(intent)
    }

    override suspend fun appStop(packageName: String) =
        throw BackendUnavailableError("Shell", "appStop() requires Shizuku (Phase 6)")

    override suspend fun appClear(packageName: String) =
        throw BackendUnavailableError("Shell", "appClear() requires Shizuku (Phase 6)")

    override suspend fun appInstall(apkPath: String) =
        throw BackendUnavailableError("Shell", "appInstall() requires Shizuku (Phase 6)")

    // ── Element finders ───────────────────────────────────────────────────────

    override fun find(selector: Selector): UiElement =
        AccessibilityUiElement(selector, provider)

    override fun findAll(selector: Selector): List<UiElement> {
        val root = provider.snapshot.root ?: return emptyList()
        return SelectorEngine.findAll(selector, root).map { node ->
            AccessibilityUiElement(
                selector = Selector.Compound(
                    listOf(
                        Selector.ResourceId(node.resourceId ?: ""),
                        Selector.Text(node.text ?: ""),
                    ).filter { s -> when (s) {
                        is Selector.ResourceId -> s.id.isNotEmpty()
                        is Selector.Text -> s.value.isNotEmpty()
                        else -> true
                    }}
                ).let { if (it.parts.size == 1) it.parts[0] else it },
                provider = provider,
            )
        }
    }

    // ── Internal gesture dispatcher ───────────────────────────────────────────

    private suspend fun dispatchGesture(gesture: GestureDescription) = suspendCoroutine { cont ->
        service.dispatchGesture(gesture, object : AccessibilityService.GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription) = cont.resume(Unit)
            override fun onCancelled(gestureDescription: GestureDescription) = cont.resume(Unit)
        }, null)
    }
}
