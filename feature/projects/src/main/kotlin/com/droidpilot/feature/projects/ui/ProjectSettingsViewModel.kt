package com.droidpilot.feature.projects.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.droidpilot.feature.projects.data.ProjectEntity
import com.droidpilot.feature.projects.data.ProjectRepository
import com.droidpilot.feature.projects.data.ProjectSettingsEmbedded
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
public class ProjectSettingsViewModel @Inject constructor(
    private val repo: ProjectRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {

    private val _project = MutableStateFlow<ProjectEntity?>(null)
    public val project: StateFlow<ProjectEntity?> = _project.asStateFlow()

    private val _isSaved = MutableStateFlow(false)
    public val isSaved: StateFlow<Boolean> = _isSaved.asStateFlow()

    public fun load(projectId: Long) {
        viewModelScope.launch(ioDispatcher) {
            _project.value = repo.getById(projectId)
        }
    }

    public fun saveSettings(projectId: Long, settings: ProjectSettingsEmbedded) {
        viewModelScope.launch(ioDispatcher) {
            repo.updateSettings(projectId, settings)
            _project.value = _project.value?.copy(settings = settings)
            _isSaved.value = true
        }
    }

    public fun clearSaved() { _isSaved.value = false }
}
