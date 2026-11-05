package com.droidpilot.feature.inspector

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.droidpilot.core.automation.ObjectRepository
import com.droidpilot.core.automation.RepositoryEntry
import com.droidpilot.core.automation.Selector
import com.droidpilot.core.automation.UiInspector
import com.droidpilot.core.automation.UiNode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class InspectorUiState(
    val root: UiNode? = null,
    val selectedNode: UiNode? = null,
    val savingNode: UiNode? = null,
    val saveName: String = "",
    val saveScreenLabel: String = "",
    val savedMessage: String? = null,
)

@HiltViewModel
class InspectorViewModel @Inject constructor(
    private val inspector: UiInspector,
    private val objectRepository: ObjectRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(InspectorUiState())
    val state = _state.asStateFlow()

    init {
        refresh()
        viewModelScope.launch {
            inspector.treeChanges.collect { refresh() }
        }
    }

    fun refresh() {
        _state.update { it.copy(root = inspector.snapshot(), selectedNode = null) }
    }

    fun selectNode(node: UiNode) {
        _state.update { it.copy(selectedNode = if (it.selectedNode == node) null else node) }
    }

    fun startSave(node: UiNode) {
        _state.update {
            it.copy(
                savingNode = node,
                saveName = node.label,
                saveScreenLabel = node.packageName ?: "",
            )
        }
    }

    fun onSaveNameChange(name: String) = _state.update { it.copy(saveName = name) }
    fun onSaveScreenLabelChange(label: String) = _state.update { it.copy(saveScreenLabel = label) }

    fun confirmSave() = viewModelScope.launch {
        val state = _state.value
        val node = state.savingNode ?: return@launch
        val bestSelector = node.selectorCandidates.firstOrNull() ?: return@launch
        objectRepository.save(
            RepositoryEntry(
                name = state.saveName.trim(),
                selector = bestSelector,
                selectorType = bestSelector.typeName(),
                appPackage = node.packageName ?: "",
                screenLabel = state.saveScreenLabel.trim(),
            )
        )
        _state.update { it.copy(savingNode = null, savedMessage = "Saved \"${state.saveName}\"") }
    }

    fun cancelSave() = _state.update { it.copy(savingNode = null) }

    fun clearSavedMessage() = _state.update { it.copy(savedMessage = null) }
}

private fun Selector.typeName(): String = when (this) {
    is Selector.ResourceId   -> "ResourceId"
    is Selector.Text         -> "Text"
    is Selector.TextContains -> "TextContains"
    is Selector.TextMatches  -> "TextMatches"
    is Selector.Description  -> "Description"
    is Selector.ClassName    -> "ClassName"
    is Selector.Package      -> "Package"
    is Selector.Clickable    -> "Clickable"
    is Selector.Enabled      -> "Enabled"
    is Selector.Index        -> "Index"
    is Selector.Instance     -> "Instance"
    is Selector.XPath        -> "XPath"
    is Selector.Child        -> "Child"
    is Selector.Sibling      -> "Sibling"
    is Selector.Compound     -> "Compound"
}
