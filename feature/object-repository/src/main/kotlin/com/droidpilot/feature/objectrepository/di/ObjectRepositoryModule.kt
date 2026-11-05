package com.droidpilot.feature.objectrepository.di

import android.content.Context
import com.droidpilot.core.automation.ObjectRepository
import com.droidpilot.feature.objectrepository.data.ObjectRepositoryDatabase
import com.droidpilot.feature.objectrepository.data.RepositoryEntryDao
import com.droidpilot.feature.objectrepository.data.RoomObjectRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ObjectRepositoryModule {

    @Binds
    @Singleton
    internal abstract fun bindObjectRepository(impl: RoomObjectRepository): ObjectRepository

    companion object {
        @Provides
        @Singleton
        internal fun provideObjectRepositoryDatabase(
            @ApplicationContext context: Context,
        ): ObjectRepositoryDatabase = ObjectRepositoryDatabase.create(context)

        @Provides
        internal fun provideRepositoryEntryDao(db: ObjectRepositoryDatabase): RepositoryEntryDao = db.dao()
    }
}
