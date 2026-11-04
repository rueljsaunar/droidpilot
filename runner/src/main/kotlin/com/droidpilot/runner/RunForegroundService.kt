package com.droidpilot.runner

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
public class RunForegroundService : Service() {

    @Inject public lateinit var runRepository: RunRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var runJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        ensureNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                runJob?.cancel()
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_START -> {
                val projectDir = intent.getStringExtra(EXTRA_PROJECT_DIR)
                    ?: run { stopSelf(); return START_NOT_STICKY }
                val config = RunConfig(
                    timeoutMs = intent.getLongExtra(EXTRA_TIMEOUT_MS, 30_000L),
                    maxRetries = intent.getIntExtra(EXTRA_MAX_RETRIES, 0),
                )
                startForeground(NOTIFICATION_ID, buildNotification("Discovering tests…"))
                startRun(projectDir, config)
            }
        }
        return START_NOT_STICKY
    }

    private fun startRun(projectDir: String, config: RunConfig) {
        runRepository.reset()
        runJob = serviceScope.launch {
            val tests = TestDiscovery.discover(projectDir)
            if (tests.isEmpty()) {
                runRepository.emit(RunEvent.RunComplete(0, 0))
                stopSelf()
                return@launch
            }
            notify("Running 0 / ${tests.size}")
            TestExecutor.execute(tests, config).collect { event ->
                runRepository.emit(event)
                when (event) {
                    is RunEvent.TestStarted ->
                        notify("Running: ${event.testCase.functionName}")
                    is RunEvent.RunComplete ->
                        notify("Done — ${event.passed} passed, ${event.failed} failed")
                    else -> {}
                }
            }
            stopSelf()
        }
    }

    private fun ensureNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID, "Test Runner", NotificationManager.IMPORTANCE_LOW,
        )
        nm().createNotificationChannel(channel)
    }

    private fun buildNotification(text: String) =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("DroidPilot")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .addAction(0, "Stop", stopPendingIntent())
            .setOngoing(true)
            .build()

    private fun notify(text: String) = nm().notify(NOTIFICATION_ID, buildNotification(text))

    private fun stopPendingIntent() = PendingIntent.getService(
        this, 0,
        Intent(this, RunForegroundService::class.java).apply { action = ACTION_STOP },
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun nm() = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    override fun onDestroy() {
        runJob?.cancel()
        super.onDestroy()
    }

    public companion object {
        public const val ACTION_START: String = "com.droidpilot.runner.START"
        public const val ACTION_STOP: String = "com.droidpilot.runner.STOP"
        public const val EXTRA_PROJECT_DIR: String = "projectDir"
        public const val EXTRA_TIMEOUT_MS: String = "timeoutMs"
        public const val EXTRA_MAX_RETRIES: String = "maxRetries"
        private const val CHANNEL_ID = "droidpilot_runner"
        private const val NOTIFICATION_ID = 1001
    }
}
