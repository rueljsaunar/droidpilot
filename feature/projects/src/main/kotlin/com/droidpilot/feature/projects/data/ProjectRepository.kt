package com.droidpilot.feature.projects.data

import kotlinx.coroutines.flow.Flow

public interface ProjectRepository {
    fun observeAll(): Flow<List<ProjectEntity>>
    suspend fun getById(id: Long): ProjectEntity?
    suspend fun create(name: String, dirPath: String): Long
    suspend fun rename(id: Long, newName: String)
    suspend fun updateSettings(id: Long, settings: ProjectSettingsEmbedded)
    suspend fun delete(id: Long)
}
