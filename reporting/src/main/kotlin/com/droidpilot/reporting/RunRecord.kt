package com.droidpilot.reporting

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "run_records")
public data class RunRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val projectDir: String,
    val startedAt: Long,
    val finishedAt: Long,
    val passed: Int,
    val failed: Int,
    val xmlReportPath: String? = null,
    val htmlReportPath: String? = null,
)
