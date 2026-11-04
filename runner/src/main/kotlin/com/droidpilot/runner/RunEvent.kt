package com.droidpilot.runner

public sealed interface RunEvent {
    public data class RunStarted(val projectId: Long, val projectDir: String) : RunEvent
    public data class TestStarted(val testCase: TestCase) : RunEvent
    public data class TestPassed(val testCase: TestCase, val durationMs: Long) : RunEvent
    public data class TestFailed(val testCase: TestCase, val error: String, val durationMs: Long) : RunEvent
    public data class StepLogged(val message: String) : RunEvent
    public data class RunComplete(val passed: Int, val failed: Int) : RunEvent
}
