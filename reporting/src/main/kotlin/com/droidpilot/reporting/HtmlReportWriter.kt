package com.droidpilot.reporting

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

public object HtmlReportWriter {

    private val DATE_FMT = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    public fun write(
        runRecord: RunRecord,
        tests: List<TestRecord>,
        logs: List<LogEntry>,
        dir: File,
    ): File {
        val total = tests.size
        val passed = tests.count { it.passed }
        val failed = tests.count { !it.passed }
        val durationSec = (runRecord.finishedAt - runRecord.startedAt) / 1000.0
        val startTime = DATE_FMT.format(Date(runRecord.startedAt))

        val testRows = tests.joinToString("\n") { t ->
            val status = if (t.passed) "pass" else "fail"
            val label = if (t.passed) "PASS" else "FAIL"
            val err = if (!t.passed) "<div class=\"err\">${t.errorMessage.orEmpty().htmlEscape()}</div>" else ""
            """      <tr class="$status">
        <td>${t.functionName.htmlEscape()}</td>
        <td>${File(t.filePath).name.htmlEscape()}</td>
        <td class="badge $status">$label</td>
        <td>${"%.3f".format(t.durationMs / 1000.0)}s</td>
      </tr>${if (err.isNotEmpty()) "\n      <tr class=\"$status\"><td colspan=\"4\">$err</td></tr>" else ""}"""
        }

        val logLines = logs.joinToString("\n") { l ->
            """<div class="log-line">${DATE_FMT.format(Date(l.timestampMs))} ${l.message.htmlEscape()}</div>"""
        }

        val html = """<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>DroidPilot Report</title>
<style>
  :root { --bg:#121212; --surface:#1e1e1e; --on-surface:#e1e1e1; --pass:#4caf50; --fail:#f44336; --muted:#888; --border:#333; }
  * { box-sizing:border-box; margin:0; padding:0; }
  body { background:var(--bg); color:var(--on-surface); font-family:'Segoe UI',system-ui,sans-serif; font-size:14px; padding:24px 16px; }
  h1 { font-size:1.5rem; margin-bottom:4px; }
  .meta { color:var(--muted); font-size:.85rem; margin-bottom:24px; }
  .summary { display:flex; gap:16px; flex-wrap:wrap; margin-bottom:24px; }
  .stat { background:var(--surface); border-radius:8px; padding:16px 20px; min-width:100px; }
  .stat-label { font-size:.75rem; color:var(--muted); text-transform:uppercase; letter-spacing:.05em; }
  .stat-value { font-size:1.75rem; font-weight:700; margin-top:4px; }
  .pass-val { color:var(--pass); }
  .fail-val { color:var(--fail); }
  h2 { font-size:1rem; margin-bottom:12px; color:var(--muted); text-transform:uppercase; letter-spacing:.05em; }
  table { width:100%; border-collapse:collapse; background:var(--surface); border-radius:8px; overflow:hidden; margin-bottom:24px; }
  th { background:#252525; padding:10px 12px; text-align:left; font-size:.8rem; text-transform:uppercase; letter-spacing:.05em; color:var(--muted); }
  td { padding:10px 12px; border-top:1px solid var(--border); vertical-align:top; }
  tr.pass td { border-left:3px solid var(--pass); }
  tr.fail td { border-left:3px solid var(--fail); }
  .badge { font-size:.75rem; font-weight:700; padding:2px 8px; border-radius:4px; }
  .badge.pass { color:var(--pass); }
  .badge.fail { color:var(--fail); }
  .err { font-family:monospace; font-size:.8rem; color:var(--fail); background:#1a0000; padding:8px; border-radius:4px; white-space:pre-wrap; word-break:break-all; }
  .logs { background:var(--surface); border-radius:8px; padding:16px; font-family:monospace; font-size:.8rem; max-height:400px; overflow-y:auto; }
  .log-line { padding:1px 0; color:var(--muted); }
  @media (prefers-color-scheme: light) {
    :root { --bg:#f5f5f5; --surface:#fff; --on-surface:#212121; --muted:#666; --border:#ddd; }
    .err { background:#fff0f0; }
  }
</style>
</head>
<body>
<h1>DroidPilot Test Report</h1>
<div class="meta">$startTime &nbsp;·&nbsp; ${"%.2f".format(durationSec)}s total</div>
<div class="summary">
  <div class="stat"><div class="stat-label">Total</div><div class="stat-value">$total</div></div>
  <div class="stat"><div class="stat-label">Passed</div><div class="stat-value pass-val">$passed</div></div>
  <div class="stat"><div class="stat-label">Failed</div><div class="stat-value fail-val">$failed</div></div>
</div>
<h2>Test Results</h2>
<table>
  <thead><tr><th>Test</th><th>File</th><th>Status</th><th>Duration</th></tr></thead>
  <tbody>
$testRows
  </tbody>
</table>
${if (logs.isNotEmpty()) "<h2>Logs</h2>\n<div class=\"logs\">\n$logLines\n</div>" else ""}
</body>
</html>
"""
        val file = File(dir, "report.html")
        file.writeText(html, Charsets.UTF_8)
        return file
    }

    private fun String.htmlEscape(): String = this
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
}
