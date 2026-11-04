package com.droidpilot.feature.editor

import androidx.lifecycle.SavedStateHandle
import java.net.URLDecoder
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.droidpilot.script.python.ScriptRunner
import java.io.File
import javax.inject.Inject

@HiltViewModel
public class EditorViewModel @Inject constructor(
    savedState: SavedStateHandle,
) : ViewModel() {

    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO

    public val projectDirPath: String =
        URLDecoder.decode(savedState["encodedDirPath"] ?: "", "UTF-8")

    private val _tabs = MutableStateFlow<List<EditorTab>>(emptyList())
    public val tabs: StateFlow<List<EditorTab>> = _tabs.asStateFlow()

    private val _activeTabIndex = MutableStateFlow(0)
    public val activeTabIndex: StateFlow<Int> = _activeTabIndex.asStateFlow()

    // path → current in-memory content
    private val contentMap = mutableMapOf<String, String>()

    public val activeContent: StateFlow<String> =
        combine(_tabs, _activeTabIndex) { tabs, idx ->
            contentMap[tabs.getOrNull(idx)?.path ?: ""] ?: ""
        }.stateIn(viewModelScope, SharingStarted.Eagerly, "")

    private val _fileTree = MutableStateFlow<List<String>>(emptyList())
    public val fileTree: StateFlow<List<String>> = _fileTree.asStateFlow()

    private val _runOutput = MutableStateFlow<List<String>>(emptyList())
    public val runOutput: StateFlow<List<String>> = _runOutput.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    public val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    // per-path debounce jobs
    private val autosaveJobs = mutableMapOf<String, Job>()

    public fun loadDirectory() {
        if (projectDirPath.isBlank()) return
        viewModelScope.launch(ioDispatcher) {
            val dir = File(projectDirPath)
            if (!dir.exists()) dir.mkdirs()
            refreshFileTree(dir)
        }
    }

    private fun refreshFileTree(dir: File) {
        _fileTree.value = dir.walkTopDown()
            .filter { it.isFile && (it.extension == "py" || it.extension == "txt") }
            .sortedWith(compareBy({ it.parent }, { it.name }))
            .map { it.absolutePath }
            .toList()
    }

    public fun openFile(path: String) {
        val existing = _tabs.value.indexOfFirst { it.path == path }
        if (existing >= 0) {
            _activeTabIndex.value = existing
            return
        }
        viewModelScope.launch(ioDispatcher) {
            val content = runCatching { File(path).readText() }.getOrDefault("")
            contentMap[path] = content
            _tabs.update { it + EditorTab(path) }
            _activeTabIndex.value = _tabs.value.lastIndex
        }
    }

    public fun createNewFile(name: String) {
        if (projectDirPath.isBlank()) return
        viewModelScope.launch(ioDispatcher) {
            val file = File(projectDirPath, if (name.endsWith(".py")) name else "$name.py")
            file.createNewFile()
            refreshFileTree(File(projectDirPath))
            openFile(file.absolutePath)
        }
    }

    public fun selectTab(index: Int) {
        if (index in _tabs.value.indices) {
            _activeTabIndex.value = index
        }
    }

    public fun closeTab(index: Int) {
        val path = _tabs.value.getOrNull(index)?.path ?: return
        // save if dirty before closing
        if (_tabs.value[index].isDirty) {
            viewModelScope.launch(ioDispatcher) {
                runCatching { File(path).writeText(contentMap[path] ?: "") }
            }
        }
        autosaveJobs.remove(path)?.cancel()
        _tabs.update { it.toMutableList().also { l -> l.removeAt(index) } }
        _activeTabIndex.value = (_activeTabIndex.value)
            .coerceAtMost((_tabs.value.size - 1).coerceAtLeast(0))
    }

    public fun onContentChanged(newText: String) {
        val idx = _activeTabIndex.value
        val path = _tabs.value.getOrNull(idx)?.path ?: return
        contentMap[path] = newText
        _tabs.update { list ->
            list.toMutableList().also { if (idx in it.indices) it[idx] = it[idx].copy(isDirty = true) }
        }
        scheduleAutosave(path)
    }

    public fun runCurrentFile() {
        val idx = _activeTabIndex.value
        val tab = _tabs.value.getOrNull(idx) ?: return
        if (_isRunning.value) return
        val content = contentMap[tab.path] ?: return
        _runOutput.value = emptyList()
        _isRunning.value = true
        viewModelScope.launch(ioDispatcher) {
            val output = runCatching { ScriptRunner.executeCapturingOutput(content) }
                .getOrElse { e -> "ERROR: ${e.message ?: e.toString()}" }
            output.lineSequence()
                .filter { it.isNotEmpty() }
                .forEach { line -> _runOutput.update { it + line } }
            _isRunning.value = false
        }
    }

    public fun appendRunOutput(line: String) { _runOutput.update { it + line } }
    public fun clearRunOutput() { _runOutput.value = emptyList() }
    public fun setRunning(running: Boolean) { _isRunning.value = running }

    private fun scheduleAutosave(path: String) {
        autosaveJobs[path]?.cancel()
        autosaveJobs[path] = viewModelScope.launch {
            delay(500L)
            withContext(ioDispatcher) {
                runCatching { File(path).writeText(contentMap[path] ?: "") }
            }
            // mark clean
            _tabs.update { list ->
                list.map { if (it.path == path) it.copy(isDirty = false) else it }
            }
        }
    }
}
