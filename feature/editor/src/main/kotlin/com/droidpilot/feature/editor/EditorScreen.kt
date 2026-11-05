package com.droidpilot.feature.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun EditorScreen(
    projectId: Long,
    encodedDirPath: String,
    onBack: () -> Unit,
    viewModel: EditorViewModel = hiltViewModel(),
) {
    val tabs by viewModel.tabs.collectAsState()
    val activeTabIndex by viewModel.activeTabIndex.collectAsState()
    val activeContent by viewModel.activeContent.collectAsState()
    val files by viewModel.fileTree.collectAsState()
    val runOutput by viewModel.runOutput.collectAsState()
    val isRunning by viewModel.isRunning.collectAsState()
    val projectDir = viewModel.projectDirPath.ifBlank { "/" }

    LaunchedEffect(Unit) { viewModel.loadDirectory() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        File(projectDir).name,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isRunning) {
                        IconButton(onClick = { /* stop not yet supported — Python exec is synchronous */ }) {
                            Icon(
                                Icons.Default.Stop,
                                contentDescription = "Stop",
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    } else {
                        IconButton(
                            onClick = { viewModel.runCurrentFile() },
                            enabled = tabs.isNotEmpty(),
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Run")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Tab row
            if (tabs.isNotEmpty()) {
                PrimaryScrollableTabRow(
                    selectedTabIndex = activeTabIndex.coerceIn(0, tabs.lastIndex),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    tabs.forEachIndexed { index, tab ->
                        Tab(
                            selected = index == activeTabIndex,
                            onClick = { viewModel.selectTab(index) },
                            text = {
                                Row {
                                    Text(
                                        text = if (tab.isDirty) "• ${File(tab.path).name}"
                                        else File(tab.path).name,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    IconButton(
                                        onClick = { viewModel.closeTab(index) },
                                        modifier = Modifier.padding(start = 4.dp),
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Close tab")
                                    }
                                }
                            },
                        )
                    }
                }
            }

            // Editor area
            Row(modifier = Modifier.weight(1f)) {
                // File tree sidebar
                FileTreePanel(
                    files = files,
                    activeFilePath = tabs.getOrNull(activeTabIndex)?.path,
                    onFileClick = { viewModel.openFile(it) },
                    onNewFile = { viewModel.createNewFile(it) },
                    modifier = Modifier.width(180.dp).fillMaxHeight(),
                )
                VerticalDivider()

                // Editor
                if (tabs.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        Text(
                            "Open a file from the sidebar",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                } else {
                    SoraEditorView(
                        content = activeContent,
                        onContentChanged = { viewModel.onContentChanged(it) },
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                }
            }

            // Run console
            if (runOutput.isNotEmpty() || isRunning) {
                RunConsole(
                    output = runOutput,
                    isRunning = isRunning,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // Status bar
            StatusBar(
                activeFilePath = tabs.getOrNull(activeTabIndex)?.path,
                isDirty = tabs.getOrNull(activeTabIndex)?.isDirty == true,
            )
        }
    }
}

@Composable
private fun StatusBar(activeFilePath: String?, isDirty: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        Text(
            "Ln 1  Col 1",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            "│",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outlineVariant,
        )
        Text(
            "Python",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            "│",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outlineVariant,
        )
        Text(
            if (isDirty) "• Unsaved" else "✓ Saved",
            style = MaterialTheme.typography.labelSmall,
            color = if (isDirty) MaterialTheme.colorScheme.tertiary
                    else MaterialTheme.colorScheme.secondary,
        )
    }
}
