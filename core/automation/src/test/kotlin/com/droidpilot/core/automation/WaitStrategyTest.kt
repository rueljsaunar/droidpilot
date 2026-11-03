package com.droidpilot.core.automation

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WaitStrategyTest {

    private val strategy = PollingWaitStrategy(intervalMs = 10)

    @Test
    fun `await returns true immediately when condition already true`() = runBlocking {
        val result = strategy.await(timeoutMs = 1_000) { true }
        assertTrue(result)
    }

    @Test
    fun `await returns true when condition becomes true before timeout`() = runBlocking {
        var count = 0
        val result = strategy.await(timeoutMs = 2_000) { ++count >= 3 }
        assertTrue(result)
    }

    @Test
    fun `await returns false when condition stays false until timeout`() = runBlocking {
        val result = strategy.await(timeoutMs = 100) { false }
        assertFalse(result)
    }
}
