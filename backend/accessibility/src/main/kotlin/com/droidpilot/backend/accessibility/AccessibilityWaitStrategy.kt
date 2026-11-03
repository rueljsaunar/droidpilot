package com.droidpilot.backend.accessibility

import com.droidpilot.core.automation.BackendUnavailableError
import com.droidpilot.core.automation.WaitStrategy
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

private const val POLL_INTERVAL_MS = 100L

class AccessibilityWaitStrategy : WaitStrategy {

    override suspend fun await(timeoutMs: Long, condition: suspend () -> Boolean): Boolean {
        val service = DroidPilotAccessibilityService.instance
            ?: throw BackendUnavailableError(
                backend = "Accessibility",
                setupHint = "Enable DroidPilot accessibility service in Settings > Accessibility",
            )

        // Subscribe before checking so we don't miss an event between check and subscribe.
        // SharedFlow(replay=1) guarantees the last emission is replayed on subscription.
        if (condition()) return true

        return withTimeoutOrNull(timeoutMs) {
            while (true) {
                // wait for next event tick OR fall through after poll interval
                withTimeoutOrNull(POLL_INTERVAL_MS) { service.eventFlow.first() }
                if (condition()) break
            }
        } != null
    }
}
