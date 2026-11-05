package com.droidpilot.feature.objectrepository.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [RepositoryEntryEntity::class],
    version = 1,
    exportSchema = true,
)
internal abstract class ObjectRepositoryDatabase : RoomDatabase() {
    abstract fun dao(): RepositoryEntryDao

    companion object {
        fun create(context: Context): ObjectRepositoryDatabase =
            Room.databaseBuilder(
                context,
                ObjectRepositoryDatabase::class.java,
                "droidpilot_object_repo.db",
            ).build()
    }
}
