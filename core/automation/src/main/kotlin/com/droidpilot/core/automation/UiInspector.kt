package com.droidpilot.core.automation

import kotlinx.coroutines.flow.Flow

/** Provides live UI tree snapshots from whatever backend is active. */
public interface UiInspector {
    /** Current snapshot; null when the accessibility service is not running. */
    public fun snapshot(): UiNode?

    /** Emits whenever the UI tree changes. */
    public val treeChanges: Flow<Unit>
}
