package com.droidpilot.reporting

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
public interface ReportingDao {

    @Insert
    public suspend fun insertRunRecord(record: RunRecord): Long

    @Update
    public suspend fun updateRunRecord(record: RunRecord)

    @Insert
    public suspend fun insertTestRecord(record: TestRecord): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    public suspend fun insertLogEntry(entry: LogEntry)

    @Query("SELECT * FROM run_records WHERE projectId = :projectId ORDER BY startedAt DESC")
    public fun getRunsForProject(projectId: Long): Flow<List<RunRecord>>

    @Query("SELECT * FROM test_records WHERE runId = :runId ORDER BY id ASC")
    public suspend fun getTestsForRun(runId: Long): List<TestRecord>

    @Query("SELECT * FROM log_entries WHERE runId = :runId ORDER BY timestampMs ASC")
    public suspend fun getLogsForRun(runId: Long): List<LogEntry>

    @Query("SELECT * FROM run_records WHERE id = :id")
    public suspend fun getRunById(id: Long): RunRecord?
}
