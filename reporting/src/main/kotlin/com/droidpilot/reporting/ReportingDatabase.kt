package com.droidpilot.reporting

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [RunRecord::class, TestRecord::class, LogEntry::class],
    version = 1,
    exportSchema = true,
)
public abstract class ReportingDatabase : RoomDatabase() {
    public abstract fun reportingDao(): ReportingDao

    public companion object {
        public fun create(context: Context): ReportingDatabase =
            Room.databaseBuilder(context, ReportingDatabase::class.java, "droidpilot_reports.db")
                .build()
    }
}
