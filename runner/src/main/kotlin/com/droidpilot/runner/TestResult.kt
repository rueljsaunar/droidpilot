package com.droidpilot.runner

public sealed interface TestResult {
    public data class Passed(val durationMs: Long) : TestResult
    public data class Failed(val error: String, val durationMs: Long) : TestResult
}
