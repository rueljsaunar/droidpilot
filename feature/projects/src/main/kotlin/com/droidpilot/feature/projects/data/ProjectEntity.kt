package com.droidpilot.feature.projects.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
public data class ProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val dirPath: String,
    val createdAt: Long,
    @Embedded val settings: ProjectSettingsEmbedded = ProjectSettingsEmbedded(),
)
