package com.droidpilot.reporting

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "test_records",
    foreignKeys = [
        ForeignKey(
            entity = RunRecord::class,
            parentColumns = ["id"],
            childColumns = ["runId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
public data class TestRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(index = true) val runId: Long,
    val filePath: String,
    val functionName: String,
    val passed: Boolean,
    val durationMs: Long,
    val errorMessage: String? = null,
)
