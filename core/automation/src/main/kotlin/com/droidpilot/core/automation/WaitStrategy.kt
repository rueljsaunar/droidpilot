package com.droidpilot.core.automation

public interface WaitStrategy {
    public suspend fun await(timeoutMs: Long, condition: suspend () -> Boolean): Boolean
}
