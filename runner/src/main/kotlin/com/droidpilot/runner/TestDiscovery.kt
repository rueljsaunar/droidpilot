package com.droidpilot.runner

import java.io.File

public object TestDiscovery {
    private val TEST_FUNCTION_RE = Regex("""^def (test_\w+)\s*\(""", RegexOption.MULTILINE)
    private val SETUP_RE = Regex("""^def setup\s*\(""", RegexOption.MULTILINE)
    private val TEARDOWN_RE = Regex("""^def teardown\s*\(""", RegexOption.MULTILINE)

    public fun discover(projectDir: String): List<TestCase> {
        val dir = File(projectDir)
        if (!dir.isDirectory) return emptyList()
        return dir.walkTopDown()
            .filter { it.isFile && it.name.startsWith("test_") && it.extension == "py" }
            .sortedBy { it.absolutePath }
            .flatMap { discoverInFile(it) }
            .toList()
    }

    public fun discoverInFile(file: File): List<TestCase> {
        val text = runCatching { file.readText() }.getOrElse { return emptyList() }
        val hasSetup = SETUP_RE.containsMatchIn(text)
        val hasTeardown = TEARDOWN_RE.containsMatchIn(text)
        return TEST_FUNCTION_RE.findAll(text)
            .map { TestCase(file.absolutePath, it.groupValues[1], hasSetup, hasTeardown) }
            .toList()
    }
}
