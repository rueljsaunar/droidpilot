package com.droidpilot.feature.projects.data

import androidx.room.ColumnInfo

public data class ProjectSettingsEmbedded(
    @ColumnInfo(name = "target_package") val targetPackage: String = "",
    @ColumnInfo(name = "target_activity") val targetActivity: String = "",
    @ColumnInfo(name = "timeout_ms") val timeoutMs: Long = 30_000L,
    @ColumnInfo(name = "max_retries") val maxRetries: Int = 1,
    @ColumnInfo(name = "launch_app_before_run") val launchAppBeforeRun: Boolean = false,
    @ColumnInfo(name = "clear_data_before_run") val clearDataBeforeRun: Boolean = false,
)
