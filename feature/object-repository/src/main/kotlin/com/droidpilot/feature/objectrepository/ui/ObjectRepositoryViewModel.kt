package com.droidpilot.feature.objectrepository.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.droidpilot.core.automation.ObjectRepository
import com.droidpilot.core.automation.RepositoryEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ObjectRepositoryUiState(
    val entries: List<RepositoryEntry> = emptyList(),
    val query: String = "",
    val selectedIds: Set<Long> = emptySet(),
    val renamingEntry: RepositoryEntry? = null,
    val renameText: String = "",
)

val ObjectRepositoryUiState.filtered: List<RepositoryEntry>
    get() = if (query.isBlank()) entries
    else entries.filter { e ->
        e.name.contains(query, ignoreCase = true) ||
        e.selectorType.contains(query, ignoreCase = true) ||
        e.screenLabel.contains(query, ignoreCase = true)
    }

val ObjectRepositoryUiState.groupedByScreen: Map<String, List<RepositoryEntry>>
    get() = filtered.groupBy { it.screenLabel }

@HiltViewModel
class ObjectRepositoryViewModel @Inject constructor(
    private val repo: ObjectRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ObjectRepositoryUiState())
    val uiState = combine(_uiState, repo.entriesFlow()) { state, entries ->
        state.copy(entries = entries)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ObjectRepositoryUiState())

    fun onQueryChange(q: String) = _uiState.update { it.copy(query = q) }

    fun toggleSelection(id: Long) = _uiState.update {
        val ids = it.selectedIds.toMutableSet()
        if (!ids.add(id)) ids.remove(id)
        it.copy(selectedIds = ids)
    }

    fun clearSelection() = _uiState.update { it.copy(selectedIds = emptySet()) }

    fun deleteSelected() = viewModelScope.launch {
        val ids = _uiState.value.selectedIds.toList()
        repo.deleteAll(ids)
        _uiState.update { it.copy(selectedIds = emptySet()) }
    }

    fun deleteEntry(id: Long) = viewModelScope.launch { repo.delete(id) }

    fun startRename(entry: RepositoryEntry) =
        _uiState.update { it.copy(renamingEntry = entry, renameText = entry.name) }

    fun onRenameTextChange(text: String) = _uiState.update { it.copy(renameText = text) }

    fun confirmRename() = viewModelScope.launch {
        val state = _uiState.value
        val entry = state.renamingEntry ?: return@launch
        if (state.renameText.isNotBlank()) repo.rename(entry.id, state.renameText)
        _uiState.update { it.copy(renamingEntry = null) }
    }

    fun cancelRename() = _uiState.update { it.copy(renamingEntry = null) }
}
