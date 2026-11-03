package com.droidpilot.core.automation

public data class ElementInfo(
    val text: String?,
    val description: String?,
    val resourceId: String?,
    val className: String?,
    val packageName: String?,
    val isClickable: Boolean,
    val isEnabled: Boolean,
    val isPassword: Boolean,
    val boundsLeft: Int,
    val boundsTop: Int,
    val boundsRight: Int,
    val boundsBottom: Int,
)
