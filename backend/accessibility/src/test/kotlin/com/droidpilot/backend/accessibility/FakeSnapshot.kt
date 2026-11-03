package com.droidpilot.backend.accessibility

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

internal class FakeSnapshot(root: SerializedNode?) : SnapshotProvider {
    override val snapshot = UiTreeSnapshot(root = root, timestamp = 0L)
    override val eventFlow: SharedFlow<Unit> = MutableSharedFlow(replay = 1)
}
