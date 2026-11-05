package com.droidpilot.backend.accessibility.di

import com.droidpilot.backend.accessibility.AccessibilityDeviceActions
import com.droidpilot.backend.accessibility.AccessibilityUiInspector
import com.droidpilot.core.automation.DeviceActions
import com.droidpilot.core.automation.UiInspector
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

    @Binds
    @Singleton
    internal abstract fun bindUiInspector(impl: AccessibilityUiInspector): UiInspector
}
