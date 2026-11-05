package com.droidpilot.feature.inspector

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.droidpilot.core.automation.Selector
import com.droidpilot.core.automation.UiNode
import com.droidpilot.core.automation.toDisplayString

@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun InspectorScreen(
    onNavigateUp: () -> Unit,
    viewModel: InspectorViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val snackbarState = remember { SnackbarHostState() }
    val clipboard = LocalClipboardManager.current

    LaunchedEffect(state.savedMessage) {
        state.savedMessage?.let {
            snackbarState.showSnackbar(it)
            viewModel.clearSavedMessage()
        }
    }

    // Save-to-repository dialog
    state.savingNode?.let { node ->
        AlertDialog(
            onDismissRequest = viewModel::cancelSave,
            title = { Text("Save to Repository") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = state.saveName,
                        onValueChange = viewModel::onSaveNameChange,
                        label = { Text("Friendly name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = state.saveScreenLabel,
                        onValueChange = viewModel::onSaveScreenLabelChange,
                        label = { Text("Screen label") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (node.selectorCandidates.isEmpty()) {
                        Text(
                            "No stable selector found for this node.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    } else {
                        Text(
                            "Selector: ${node.selectorCandidates.first().toDisplayString()}",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = viewModel::confirmSave,
                    enabled = state.saveName.isNotBlank() && node.selectorCandidates.isNotEmpty(),
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelSave) { Text("Cancel") }
            },
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("UI Inspector") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::refresh) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        snackbarHost = { SnackbarHost(snackbarState) },
    ) { innerPadding ->
        if (state.root == null) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Accessibility service not running.\nEnable it in device Settings → Accessibility.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                )
            }
        } else {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                // Tree view
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                ) {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        items(flattenTree(state.root!!)) { item ->
                            UiNodeRow(
                                item = item,
                                selected = item.node == state.selectedNode,
                                onClick = { viewModel.selectNode(item.node) },
                            )
                        }
                    }
                }

                // Attribute panel for selected node
                state.selectedNode?.let { node ->
                    AttributePanel(
                        node = node,
                        onCopySelector = { selector ->
                            clipboard.setText(AnnotatedString(selector.toDisplayString()))
                        },
                        onSaveToRepo = { viewModel.startSave(node) },
                    )
                }
            }
        }
    }
}

private data class FlatNode(val node: UiNode, val depth: Int)

private fun flattenTree(root: UiNode, depth: Int = 0): List<FlatNode> {
    val result = mutableListOf(FlatNode(root, depth))
    root.children.forEach { result.addAll(flattenTree(it, depth + 1)) }
    return result
}

@Composable
private fun UiNodeRow(item: FlatNode, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected)
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
    else
        MaterialTheme.colorScheme.surface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (item.depth * 16).dp)
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .border(
                width = if (selected) 1.dp else 0.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else bg,
                shape = RoundedCornerShape(6.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Class name in muted style
        val shortClass = item.node.className?.substringAfterLast('.') ?: "?"
        Text(
            shortClass,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            item.node.label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            modifier = Modifier.weight(1f),
        )
        if (!item.node.isEnabled) {
            Text(
                "disabled",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
            )
        }
    }
}

@Composable
private fun AttributePanel(
    node: UiNode,
    onCopySelector: (Selector) -> Unit,
    onSaveToRepo: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Attributes",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Row {
                IconButton(onClick = onSaveToRepo, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Bookmark, "Save to Repository", Modifier.size(18.dp))
                }
                // Copy best selector
                val bestSelector = node.selectorCandidates.firstOrNull()
                if (bestSelector != null) {
                    IconButton(
                        onClick = { onCopySelector(bestSelector) },
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(Icons.Default.ContentCopy, "Copy selector", Modifier.size(18.dp))
                    }
                }
            }
        }

        AttributeRow("text", node.text)
        AttributeRow("contentDesc", node.contentDescription)
        AttributeRow("resourceId", node.resourceId)
        AttributeRow("className", node.className)
        AttributeRow("package", node.packageName)
        AttributeRow("bounds", node.bounds.run { "[$left,$top,$right,$bottom]" })
        AttributeRow("clickable", node.isClickable.toString())
        AttributeRow("enabled", node.isEnabled.toString())
        AttributeRow("checked", node.isChecked.toString())

        if (node.selectorCandidates.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Text(
                "Selector suggestions",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
            )
            node.selectorCandidates.forEachIndexed { i, sel ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        sel.toDisplayString(),
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    if (i == 0) {
                        Text(
                            "best",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(start = 4.dp),
                        )
                    }
                    IconButton(
                        onClick = { onCopySelector(sel) },
                        modifier = Modifier.size(28.dp),
                    ) {
                        Icon(Icons.Default.ContentCopy, null, Modifier.size(14.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun AttributeRow(key: String, value: String?) {
    if (value == null) return
    Row(Modifier.fillMaxWidth()) {
        Text(
            "$key:",
            style = MaterialTheme.typography.labelSmall,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(100.dp),
        )
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
    }
}
