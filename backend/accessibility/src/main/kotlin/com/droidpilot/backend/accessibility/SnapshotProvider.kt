package com.droidpilot.backend.accessibility

import kotlinx.coroutines.flow.SharedFlow

internal interface SnapshotProvider {
    val snapshot: UiTreeSnapshot
    val eventFlow: SharedFlow<Unit>
}
