package com.droidpilot.runner

public data class RunConfig(
    val timeoutMs: Long = 30_000L,
    val maxRetries: Int = 0,
    val targetPackage: String? = null,
)
