package com.droidpilot.runner

import com.droidpilot.script.python.ScriptRunner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

public object TestExecutor {
    public fun execute(tests: List<TestCase>, config: RunConfig): Flow<RunEvent> = flow {
        var passed = 0
        var failed = 0
        for (testCase in tests) {
            emit(RunEvent.TestStarted(testCase))
            val start = System.currentTimeMillis()
            val result = runWithRetries(testCase, config)
            val durationMs = System.currentTimeMillis() - start
            when (result) {
                is TestResult.Passed -> {
                    passed++
                    emit(RunEvent.TestPassed(testCase, durationMs))
                }
                is TestResult.Failed -> {
                    failed++
                    emit(RunEvent.TestFailed(testCase, result.error, durationMs))
                }
            }
        }
        emit(RunEvent.RunComplete(passed, failed))
    }

    private suspend fun runWithRetries(testCase: TestCase, config: RunConfig): TestResult {
        var lastError = "Unknown error"
        repeat(config.maxRetries + 1) { attempt ->
            val result = runSingle(testCase, config.timeoutMs)
            if (result is TestResult.Passed) return result
            if (result is TestResult.Failed) lastError = result.error
            if (attempt < config.maxRetries) delay(500L)
        }
        return TestResult.Failed(lastError, 0L)
    }

    private suspend fun runSingle(testCase: TestCase, timeoutMs: Long): TestResult =
        runCatching {
            withTimeout(timeoutMs) {
                withContext(Dispatchers.IO) {
                    ScriptRunner.executeCapturingOutput(buildScript(testCase))
                }
            }
        }.fold(
            onSuccess = { TestResult.Passed(0L) },
            onFailure = { e -> TestResult.Failed(e.message ?: e.toString(), 0L) },
        )

    private fun buildScript(testCase: TestCase): String {
        val path = testCase.filePath.replace("\\", "/")
        return buildString {
            appendLine("import sys, importlib.util")
            appendLine("sys.path.insert(0, __import__('os').path.dirname('$path'))")
            appendLine("_spec = importlib.util.spec_from_file_location('_test_mod', '$path')")
            appendLine("_mod = importlib.util.module_from_spec(_spec)")
            appendLine("_spec.loader.exec_module(_mod)")
            if (testCase.hasSetup) appendLine("if hasattr(_mod, 'setup'): _mod.setup()")
            appendLine("_mod.${testCase.functionName}()")
            if (testCase.hasTeardown) appendLine("if hasattr(_mod, 'teardown'): _mod.teardown()")
        }
    }
}
