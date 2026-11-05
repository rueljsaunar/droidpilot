package com.droidpilot.core.automation

import kotlinx.coroutines.flow.Flow

/** Persisted named selectors — the Object Repository. */
public interface ObjectRepository {
    public fun entriesFlow(): Flow<List<RepositoryEntry>>
    public suspend fun findByName(name: String): RepositoryEntry?
    public suspend fun save(entry: RepositoryEntry)
    public suspend fun rename(id: Long, newName: String)
    public suspend fun delete(id: Long)
    public suspend fun deleteAll(ids: List<Long>)
}

public data class RepositoryEntry(
    val id: Long = 0,
    val name: String,
    val selector: Selector,
    val selectorType: String,
    val appPackage: String,
    val screenLabel: String,
    val screenshotPath: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
)
