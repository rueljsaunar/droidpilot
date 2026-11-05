package com.droidpilot.feature.objectrepository.data

import com.droidpilot.core.automation.ObjectRepository
import com.droidpilot.core.automation.RepositoryEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

internal class RoomObjectRepository @Inject constructor(
    private val dao: RepositoryEntryDao,
) : ObjectRepository {

    override fun entriesFlow(): Flow<List<RepositoryEntry>> =
        dao.getAllFlow().map { list -> list.map { it.toDomain() } }

    override suspend fun findByName(name: String): RepositoryEntry? =
        dao.findByName(name)?.toDomain()

    override suspend fun save(entry: RepositoryEntry) {
        dao.insert(RepositoryEntryEntity.from(entry))
    }

    override suspend fun rename(id: Long, newName: String) = dao.rename(id, newName)

    override suspend fun delete(id: Long) = dao.delete(id)

    override suspend fun deleteAll(ids: List<Long>) = dao.deleteAll(ids)
}
