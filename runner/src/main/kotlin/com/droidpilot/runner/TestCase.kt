package com.droidpilot.runner

public data class TestCase(
    val filePath: String,
    val functionName: String,
    val hasSetup: Boolean = false,
    val hasTeardown: Boolean = false,
)
