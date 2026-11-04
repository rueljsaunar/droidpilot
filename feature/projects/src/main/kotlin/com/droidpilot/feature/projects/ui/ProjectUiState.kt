package com.droidpilot.feature.projects.ui

import com.droidpilot.feature.projects.data.ProjectEntity

public data class ProjectUiState(
    val projects: List<ProjectEntity> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)
