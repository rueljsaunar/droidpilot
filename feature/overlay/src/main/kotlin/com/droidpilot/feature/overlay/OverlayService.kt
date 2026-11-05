package com.droidpilot.feature.overlay

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.droidpilot.core.automation.ActionRecorder
import com.droidpilot.runner.RunRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
public class OverlayService : Service() {

    @Inject public lateinit var runRepository: RunRepository
    @Inject public lateinit var actionRecorder: ActionRecorder

    private val lifecycleOwner = OverlayLifecycleOwner()
    private var overlayView: ComposeView? = null
    private lateinit var windowManager: WindowManager

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        showBubble()
    }

    private fun showBubble() {
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 16
            y = 100
        }

        val view = ComposeView(this).apply {
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)
            setContent {
                val stepsState = actionRecorder.steps.collectAsState()
                val stepCountState = androidx.compose.runtime.remember {
                    androidx.compose.runtime.derivedStateOf { stepsState.value.size }
                }
                OverlayBubble(
                    isRunning = runRepository.isRunning.collectAsState(),
                    currentStep = runRepository.currentStep.collectAsState(),
                    passCount = runRepository.passCount.collectAsState(),
                    failCount = runRepository.failCount.collectAsState(),
                    isRecording = actionRecorder.isRecording.collectAsState(),
                    recordedStepCount = stepCountState,
                    onStartRecording = actionRecorder::startRecording,
                    onStopRecording = actionRecorder::stopRecording,
                    onOpenRecorder = {
                        val intent = packageManager.getLaunchIntentForPackage(packageName)
                            ?.apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                                putExtra(EXTRA_OPEN_RECORDER, true)
                            }
                        intent?.let { startActivity(it) }
                    },
                    onMove = { dx, dy ->
                        params.x = (params.x + dx.toInt()).coerceAtLeast(0)
                        params.y = (params.y + dy.toInt()).coerceAtLeast(0)
                        windowManager.updateViewLayout(this, params)
                    },
                    onDismiss = { stopSelf() },
                )
            }
        }

        lifecycleOwner.start()
        overlayView = view
        windowManager.addView(view, params)
    }

    override fun onDestroy() {
        overlayView?.let { windowManager.removeView(it) }
        lifecycleOwner.stop()
        super.onDestroy()
    }

    public companion object {
        public const val ACTION_SHOW: String = "com.droidpilot.overlay.SHOW"
        public const val ACTION_HIDE: String = "com.droidpilot.overlay.HIDE"
        public const val EXTRA_OPEN_RECORDER: String = "open_recorder"
    }
}
