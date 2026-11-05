package com.droidpilot.feature.objectrepository.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.droidpilot.core.automation.RepositoryEntry
import com.droidpilot.core.automation.toDisplayString

@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun ObjectRepositoryScreen(
    onNavigateUp: () -> Unit,
    onInsertIntoEditor: ((String) -> Unit)? = null,
    viewModel: ObjectRepositoryViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val clipboard = LocalClipboardManager.current
    val selectionMode = state.selectedIds.isNotEmpty()

    // Rename dialog
    state.renamingEntry?.let { entry ->
        AlertDialog(
            onDismissRequest = viewModel::cancelRename,
            title = { Text("Rename entry") },
            text = {
                OutlinedTextField(
                    value = state.renameText,
                    onValueChange = viewModel::onRenameTextChange,
                    label = { Text("Name") },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::confirmRename) { Text("Rename") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelRename) { Text("Cancel") }
            },
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    if (selectionMode) Text("${state.selectedIds.size} selected")
                    else Text("Object Repository")
                },
                navigationIcon = {
                    if (selectionMode) {
                        IconButton(onClick = viewModel::clearSelection) {
                            Icon(Icons.Default.Close, contentDescription = "Clear selection")
                        }
                    } else {
                        IconButton(onClick = onNavigateUp) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    if (selectionMode) {
                        IconButton(onClick = { viewModel.deleteSelected() }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete selected")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            // Search bar
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::onQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search by name, type, screen…") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
            )

            val grouped = state.groupedByScreen
            if (grouped.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "No saved selectors yet.\nUse the Inspector to save elements.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    )
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
                    grouped.forEach { (screenLabel, entries) ->
                        item {
                            Text(
                                text = screenLabel.ifBlank { "Unnamed Screen" },
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
                            )
                        }
                        items(entries, key = { it.id }) { entry ->
                            RepositoryEntryCard(
                                entry = entry,
                                selected = entry.id in state.selectedIds,
                                onToggleSelect = { viewModel.toggleSelection(entry.id) },
                                onRename = { viewModel.startRename(entry) },
                                onDelete = { viewModel.deleteEntry(entry.id) },
                                onCopySelector = {
                                    clipboard.setText(
                                        AnnotatedString(entry.selector.toDisplayString())
                                    )
                                },
                                onInsertIntoEditor = onInsertIntoEditor?.let {
                                    { it(selectorToPythonCall(entry)) }
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RepositoryEntryCard(
    entry: RepositoryEntry,
    selected: Boolean,
    onToggleSelect: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onCopySelector: () -> Unit,
    onInsertIntoEditor: (() -> Unit)?,
) {
    var expanded by remember { mutableStateOf(false) }
    val borderColor = if (selected)
        MaterialTheme.colorScheme.primary
    else
        MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(
            containerColor = if (selected)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else
                MaterialTheme.colorScheme.surface,
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(0.dp),
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Type badge
                SelectorTypeBadge(entry.selectorType)
                Spacer(Modifier.width(10.dp))
                Text(
                    entry.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(4.dp))
                IconButton(onClick = onToggleSelect, modifier = Modifier.size(32.dp)) {
                    Icon(
                        if (selected) Icons.Default.Close else Icons.Default.Edit,
                        contentDescription = if (selected) "Deselect" else "Select",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(Modifier.padding(top = 10.dp)) {
                    Text(
                        entry.selector.toDisplayString(),
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(8.dp),
                    )
                    Text(
                        entry.appPackage,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        IconButton(onClick = onCopySelector, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.ContentCopy, "Copy selector", Modifier.size(16.dp))
                        }
                        if (onInsertIntoEditor != null) {
                            IconButton(onClick = onInsertIntoEditor, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.DriveFileRenameOutline, "Insert into editor", Modifier.size(16.dp))
                            }
                        }
                        IconButton(onClick = onRename, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Edit, "Rename", Modifier.size(16.dp))
                        }
                        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Delete, "Delete", Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectorTypeBadge(type: String) {
    val (bg, fg) = when (type) {
        "ResourceId"   -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.primary
        "Text"         -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.secondary
        "Description"  -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.tertiary
        else           -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bg)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(
            type.take(10),
            style = MaterialTheme.typography.labelSmall,
            fontSize = 10.sp,
            color = fg,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

private fun selectorToPythonCall(entry: RepositoryEntry): String {
    val name = entry.name.replace("\"", "\\\"")
    return "d.repo(\"$name\")"
}
