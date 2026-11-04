package com.droidpilot.reporting

import android.content.Context
import com.droidpilot.runner.RunEvent
import com.droidpilot.runner.RunRepository
import com.droidpilot.runner.TestCase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
public class ReportRepository @Inject constructor(
    private val runRepository: RunRepository,
    private val db: ReportingDatabase,
    @ApplicationContext private val context: Context,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var accumulator: RunAccumulator? = null

    init {
        scope.launch { observeRuns() }
    }

    public fun getRunsForProject(projectId: Long): Flow<List<RunRecord>> =
        db.reportingDao().getRunsForProject(projectId)

    private suspend fun observeRuns() {
        runRepository.recordEvents.collect { event ->
            when (event) {
                is RunEvent.RunStarted -> {
                    accumulator = RunAccumulator(event.projectId, event.projectDir)
                }
                is RunEvent.TestStarted -> {
                    accumulator?.currentTestStart = System.currentTimeMillis()
                    accumulator?.currentTest = event.testCase
                }
                is RunEvent.StepLogged -> {
                    accumulator?.logs?.add(
                        LogEntry(runId = 0, message = event.message, timestampMs = System.currentTimeMillis()),
                    )
                }
                is RunEvent.TestPassed -> {
                    val start = accumulator?.currentTestStart ?: System.currentTimeMillis()
                    accumulator?.tests?.add(
                        TestRecord(
                            runId = 0,
                            filePath = event.testCase.filePath,
                            functionName = event.testCase.functionName,
                            passed = true,
                            durationMs = event.durationMs.takeIf { it > 0 }
                                ?: (System.currentTimeMillis() - start),
                        ),
                    )
                    accumulator?.currentTest = null
                }
                is RunEvent.TestFailed -> {
                    val start = accumulator?.currentTestStart ?: System.currentTimeMillis()
                    accumulator?.tests?.add(
                        TestRecord(
                            runId = 0,
                            filePath = event.testCase.filePath,
                            functionName = event.testCase.functionName,
                            passed = false,
                            durationMs = event.durationMs.takeIf { it > 0 }
                                ?: (System.currentTimeMillis() - start),
                            errorMessage = event.error,
                        ),
                    )
                    accumulator?.currentTest = null
                }
                is RunEvent.RunComplete -> {
                    accumulator?.let { acc -> saveRun(acc, event.passed, event.failed) }
                    accumulator = null
                }
            }
        }
    }

    private suspend fun saveRun(acc: RunAccumulator, passed: Int, failed: Int) {
        val now = System.currentTimeMillis()
        val dao = db.reportingDao()

        val base = RunRecord(
            projectId = acc.projectId,
            projectDir = acc.projectDir,
            startedAt = acc.startedAt,
            finishedAt = now,
            passed = passed,
            failed = failed,
        )
        val runId = dao.insertRunRecord(base)

        val tests = acc.tests.map { it.copy(runId = runId) }
        tests.forEach { dao.insertTestRecord(it) }

        val logs = acc.logs.map { it.copy(runId = runId) }
        logs.forEach { dao.insertLogEntry(it) }

        val reportsDir = File(context.filesDir, "reports/$runId").apply { mkdirs() }
        val withId = base.copy(id = runId)
        val xmlFile = runCatching { JunitXmlWriter.write(withId, tests, reportsDir) }.getOrNull()
        val htmlFile = runCatching { HtmlReportWriter.write(withId, tests, logs, reportsDir) }.getOrNull()

        if (xmlFile != null || htmlFile != null) {
            dao.updateRunRecord(
                withId.copy(
                    xmlReportPath = xmlFile?.absolutePath,
                    htmlReportPath = htmlFile?.absolutePath,
                ),
            )
        }
    }

    private data class RunAccumulator(
        val projectId: Long,
        val projectDir: String,
        val startedAt: Long = System.currentTimeMillis(),
        val tests: MutableList<TestRecord> = mutableListOf(),
        val logs: MutableList<LogEntry> = mutableListOf(),
        var currentTestStart: Long = 0L,
        var currentTest: TestCase? = null,
    )
}
