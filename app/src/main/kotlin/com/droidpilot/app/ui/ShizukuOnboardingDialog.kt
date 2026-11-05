package com.droidpilot.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private val STEPS = listOf(
    "1" to "Open Settings → Developer Options and enable Wireless Debugging.",
    "2" to "Install Shizuku from the Play Store (search for “Shizuku by Rikka”).",
    "3" to "Open Shizuku and tap “Start via Wireless Debugging”. Follow the pairing prompts.",
    "4" to "Return to DroidPilot and tap “Grant” in the banner above.",
    "5" to "Approve the Shizuku permission dialog when it appears.",
)

@Composable
fun ShizukuOnboardingDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set up Shizuku") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "Shizuku grants DroidPilot shell-level access for app management, " +
                        "screen capture, and logcat — without requiring root.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                STEPS.forEach { (num, text) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = num,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 1.dp),
                        )
                        Text(text = text, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Got it") }
        },
    )
}
