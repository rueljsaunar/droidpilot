package com.droidpilot.backend.accessibility.di

import com.droidpilot.backend.accessibility.AccessibilityDeviceActions
import com.droidpilot.core.automation.DeviceActions
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AccessibilityModule {
    @Binds
    @Singleton
    abstract fun bindDeviceActions(impl: AccessibilityDeviceActions): DeviceActions
}
