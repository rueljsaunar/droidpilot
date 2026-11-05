package com.droidpilot.core.automation

import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull

public class PollingWaitStrategy(private val intervalMs: Long = 50L) : WaitStrategy {
    override suspend fun await(timeoutMs: Long, condition: suspend () -> Boolean): Boolean {
        if (condition()) return true
        return withTimeoutOrNull(timeoutMs) {
            while (true) {
                delay(intervalMs)
                if (condition()) return@withTimeoutOrNull true
            }
            @Suppress("UNREACHABLE_CODE")
            false
        } ?: false
    }
}
