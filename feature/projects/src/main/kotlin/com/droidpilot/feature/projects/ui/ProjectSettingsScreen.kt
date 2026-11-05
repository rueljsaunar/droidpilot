package com.droidpilot.feature.projects.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.droidpilot.feature.projects.data.ProjectSettingsEmbedded

@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun ProjectSettingsScreen(
    projectId: Long,
    onNavigateUp: () -> Unit,
    viewModel: ProjectSettingsViewModel = hiltViewModel(),
) {
    val project by viewModel.project.collectAsState()
    val isSaved by viewModel.isSaved.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(projectId) { viewModel.load(projectId) }
    LaunchedEffect(isSaved) {
        if (isSaved) {
            snackbarHostState.showSnackbar("Settings saved")
            viewModel.clearSaved()
        }
    }

    val settings = project?.settings ?: ProjectSettingsEmbedded()
    var targetPackage by remember(settings) { mutableStateOf(settings.targetPackage) }
    var targetActivity by remember(settings) { mutableStateOf(settings.targetActivity) }
    var timeoutMs by remember(settings) { mutableStateOf(settings.timeoutMs.toString()) }
    var maxRetries by remember(settings) { mutableStateOf(settings.maxRetries.toString()) }
    var launchBefore by remember(settings) { mutableStateOf(settings.launchAppBeforeRun) }
    var clearData by remember(settings) { mutableStateOf(settings.clearDataBeforeRun) }
    var showPicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Project Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = {
                        viewModel.saveSettings(
                            projectId,
                            ProjectSettingsEmbedded(
                                targetPackage = targetPackage,
                                targetActivity = targetActivity,
                                timeoutMs = timeoutMs.toLongOrNull() ?: 30_000L,
                                maxRetries = maxRetries.toIntOrNull() ?: 1,
                                launchAppBeforeRun = launchBefore,
                                clearDataBeforeRun = clearData,
                            ),
                        )
                    }) { Text("Save") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            SectionLabel("Target App")
            OutlinedTextField(
                value = targetPackage,
                onValueChange = { targetPackage = it },
                label = { Text("Package name") },
                placeholder = { Text("com.example.app") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = targetActivity,
                onValueChange = { targetActivity = it },
                label = { Text("Launch activity (optional)") },
                placeholder = { Text(".MainActivity") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { showPicker = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Default.Apps, contentDescription = null)
                Text("Browse installed apps", modifier = Modifier.padding(start = 8.dp))
            }

            Spacer(Modifier.height(24.dp))
            SectionLabel("Run Defaults")
            OutlinedTextField(
                value = timeoutMs,
                onValueChange = { timeoutMs = it.filter { c -> c.isDigit() } },
                label = { Text("Timeout (ms)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = maxRetries,
                onValueChange = { maxRetries = it.filter { c -> c.isDigit() } },
                label = { Text("Max retries") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(24.dp))
            SectionLabel("Before Run")
            SwitchRow(
                label = "Launch app before run",
                checked = launchBefore,
                onCheckedChange = { launchBefore = it },
            )
            SwitchRow(
                label = "Clear app data before run",
                checked = clearData,
                onCheckedChange = { clearData = it },
            )
        }
    }

    if (showPicker) {
        InstalledAppPickerSheet(
            onAppSelected = { pkg, activity ->
                targetPackage = pkg
                targetActivity = activity
                showPicker = false
            },
            onDismiss = { showPicker = false },
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 8.dp),
    )
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
