package com.droidpilot.feature.projects.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.droidpilot.feature.projects.data.ProjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
public class ProjectListViewModel @Inject constructor(
    private val repo: ProjectRepository,
) : ViewModel() {

    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO

    public val projects: StateFlow<List<com.droidpilot.feature.projects.data.ProjectEntity>> =
        repo.observeAll().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _error = MutableStateFlow<String?>(null)
    public val error: StateFlow<String?> = _error.asStateFlow()

    public fun createProject(name: String, dirPath: String) {
        viewModelScope.launch(ioDispatcher) {
            runCatching { repo.create(name, dirPath) }
                .onFailure { e -> _error.update { e.message ?: e.toString() } }
        }
    }

    public fun renameProject(id: Long, newName: String) {
        viewModelScope.launch(ioDispatcher) {
            runCatching { repo.rename(id, newName) }
                .onFailure { e -> _error.update { e.message } }
        }
    }

    public fun deleteProject(id: Long) {
        viewModelScope.launch(ioDispatcher) {
            runCatching { repo.delete(id) }
                .onFailure { e -> _error.update { e.message } }
        }
    }

    public fun clearError() { _error.update { null } }
}
