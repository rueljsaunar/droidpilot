package com.droidpilot.feature.projects.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [ProjectEntity::class], version = 1, exportSchema = true)
public abstract class ProjectDatabase : RoomDatabase() {
    public abstract fun projectDao(): ProjectDao

    public companion object {
        public fun create(context: Context): ProjectDatabase =
            Room.databaseBuilder(context, ProjectDatabase::class.java, "droidpilot.db").build()
    }
}
