package com.droidpilot.feature.projects.di

import android.content.Context
import com.droidpilot.feature.projects.data.ProjectDao
import com.droidpilot.feature.projects.data.ProjectDatabase
import com.droidpilot.feature.projects.data.ProjectRepository
import com.droidpilot.feature.projects.data.RoomProjectRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
public object ProjectsModule {

    @Provides
    @Singleton
    public fun provideProjectDatabase(@ApplicationContext ctx: Context): ProjectDatabase =
        ProjectDatabase.create(ctx)

    @Provides
    public fun provideProjectDao(db: ProjectDatabase): ProjectDao = db.projectDao()

    @Provides
    @Singleton
    public fun provideProjectRepository(dao: ProjectDao): ProjectRepository =
        RoomProjectRepository(dao)
}
