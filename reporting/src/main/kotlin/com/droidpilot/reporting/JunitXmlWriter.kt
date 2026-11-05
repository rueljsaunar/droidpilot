package com.droidpilot.reporting

import java.io.File
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

public object JunitXmlWriter {

    private val ISO_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
        .withZone(ZoneOffset.UTC)

    public fun write(runRecord: RunRecord, tests: List<TestRecord>, dir: File): File {
        val total = tests.size
        val failures = tests.count { !it.passed }
        val totalSec = (runRecord.finishedAt - runRecord.startedAt) / 1000.0

        val byFile = tests.groupBy { File(it.filePath).nameWithoutExtension }

        val sb = StringBuilder()
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        sb.append("<testsuites name=\"DroidPilot\"")
        sb.append(" tests=\"$total\"")
        sb.append(" failures=\"$failures\"")
        sb.append(" time=\"%.3f\"".format(totalSec))
        sb.append(">\n")

        for ((className, classTests) in byFile) {
            val classFail = classTests.count { !it.passed }
            val classSec = classTests.sumOf { it.durationMs } / 1000.0
            val ts = ISO_FMT.format(Instant.ofEpochMilli(runRecord.startedAt))
            sb.append("  <testsuite name=\"$className\"")
            sb.append(" tests=\"${classTests.size}\"")
            sb.append(" failures=\"$classFail\"")
            sb.append(" time=\"%.3f\"".format(classSec))
            sb.append(" timestamp=\"$ts\"")
            sb.append(">\n")
            for (t in classTests) {
                val testSec = t.durationMs / 1000.0
                sb.append("    <testcase name=\"${t.functionName.xmlEscape()}\"")
                sb.append(" classname=\"$className\"")
                sb.append(" time=\"%.3f\"".format(testSec))
                sb.append(">")
                if (!t.passed) {
                    val msg = t.errorMessage.orEmpty().xmlEscape()
                    sb.append("\n      <failure message=\"$msg\">$msg</failure>")
                    sb.append("\n    </testcase>")
                } else {
                    sb.append("</testcase>")
                }
                sb.append("\n")
            }
            sb.append("  </testsuite>\n")
        }
        sb.append("</testsuites>\n")

        val file = File(dir, "report.xml")
        file.writeText(sb.toString(), Charsets.UTF_8)
        return file
    }

    private fun String.xmlEscape(): String = this
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")
}
