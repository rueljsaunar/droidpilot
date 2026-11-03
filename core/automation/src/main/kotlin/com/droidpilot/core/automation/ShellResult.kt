package com.droidpilot.core.automation

public data class ShellResult(
    val stdout: String,
    val stderr: String,
    val exitCode: Int,
) {
    public val isSuccess: Boolean get() = exitCode == 0
}
