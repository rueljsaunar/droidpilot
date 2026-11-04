package com.droidpilot.reporting

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "log_entries",
    foreignKeys = [
        ForeignKey(
            entity = RunRecord::class,
            parentColumns = ["id"],
            childColumns = ["runId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
public data class LogEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(index = true) val runId: Long,
    val message: String,
    val timestampMs: Long,
)
