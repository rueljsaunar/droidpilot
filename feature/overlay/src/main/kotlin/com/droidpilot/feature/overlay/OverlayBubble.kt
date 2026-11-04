package com.droidpilot.feature.overlay

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable

@Composable
internal fun OverlayBubble(
    isRunning: State<Boolean>,
    currentStep: State<String>,
    passCount: State<Int>,
    failCount: State<Int>,
    onMove: (Float, Float) -> Unit,
    onDismiss: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val running = isRunning.value
    val step = currentStep.value
    val passed = passCount.value
    val failed = failCount.value

    MaterialTheme {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
            modifier = Modifier.padding(4.dp),
        ) {
            if (expanded) {
                Column(
                    modifier = Modifier
                        .width(220.dp)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    // Drag handle row
                    Text(
                        text = "⠿  Drag to move",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .pointerInput(Unit) {
                                detectDragGestures { _, amount -> onMove(amount.x, amount.y) }
                            },
                    )
                    Text(
                        text = if (running && step.isNotEmpty()) "▶  $step" else "Idle",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (running) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "✓ $passed   ✗ $failed",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(onClick = { expanded = false }) { Text("Collapse") }
                        TextButton(onClick = onDismiss) { Text("Close") }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clickable { expanded = true },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "▶\n$passed/$failed",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (running) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
