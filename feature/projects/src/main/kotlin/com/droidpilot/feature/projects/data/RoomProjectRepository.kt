package com.droidpilot.feature.projects.data

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

public class RoomProjectRepository @Inject constructor(
    private val dao: ProjectDao,
) : ProjectRepository {

    override fun observeAll(): Flow<List<ProjectEntity>> = dao.observeAll()

    override suspend fun getById(id: Long): ProjectEntity? = dao.getById(id)

    override suspend fun create(name: String, dirPath: String): Long =
        dao.insert(ProjectEntity(name = name, dirPath = dirPath, createdAt = System.currentTimeMillis()))

    override suspend fun rename(id: Long, newName: String) {
        val entity = dao.getById(id) ?: return
        dao.update(entity.copy(name = newName))
    }

    override suspend fun updateSettings(id: Long, settings: ProjectSettingsEmbedded) {
        val entity = dao.getById(id) ?: return
        dao.update(entity.copy(settings = settings))
    }

    override suspend fun delete(id: Long) {
        val entity = dao.getById(id) ?: return
        dao.delete(entity)
    }
}
