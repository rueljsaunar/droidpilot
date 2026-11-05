package com.droidpilot.reporting.di

import android.content.Context
import com.droidpilot.reporting.ReportingDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
public object ReportingModule {

    @Provides
    @Singleton
    public fun provideReportingDatabase(@ApplicationContext context: Context): ReportingDatabase =
        ReportingDatabase.create(context)
}
