package com.droidpilot.feature.projects.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeProjectRepository : ProjectRepository {
    private val _projects = MutableStateFlow<Map<Long, ProjectEntity>>(emptyMap())
    private var nextId = 1L

    override fun observeAll(): Flow<List<ProjectEntity>> =
        _projects.map { it.values.sortedByDescending { p -> p.createdAt } }

    override suspend fun getById(id: Long): ProjectEntity? = _projects.value[id]

    override suspend fun create(name: String, dirPath: String): Long {
        val id = nextId++
        val entity = ProjectEntity(id = id, name = name, dirPath = dirPath, createdAt = id)
        _projects.value = _projects.value + (id to entity)
        return id
    }

    override suspend fun rename(id: Long, newName: String) {
        val entity = _projects.value[id] ?: return
        _projects.value = _projects.value + (id to entity.copy(name = newName))
    }

    override suspend fun updateSettings(id: Long, settings: ProjectSettingsEmbedded) {
        val entity = _projects.value[id] ?: return
        _projects.value = _projects.value + (id to entity.copy(settings = settings))
    }

    override suspend fun delete(id: Long) {
        _projects.value = _projects.value - id
    }
}
