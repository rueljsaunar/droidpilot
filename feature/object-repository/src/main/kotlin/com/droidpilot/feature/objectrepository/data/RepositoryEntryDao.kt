package com.droidpilot.feature.objectrepository.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
internal interface RepositoryEntryDao {

    @Query("SELECT * FROM repository_entries ORDER BY screen_label ASC, name ASC")
    fun getAllFlow(): Flow<List<RepositoryEntryEntity>>

    @Query("SELECT * FROM repository_entries WHERE name = :name LIMIT 1")
    suspend fun findByName(name: String): RepositoryEntryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: RepositoryEntryEntity): Long

    @Query("UPDATE repository_entries SET name = :newName WHERE id = :id")
    suspend fun rename(id: Long, newName: String)

    @Query("DELETE FROM repository_entries WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM repository_entries WHERE id IN (:ids)")
    suspend fun deleteAll(ids: List<Long>)
}
