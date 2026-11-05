package com.droidpilot.feature.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable

// Hardcoded mockup palette — mirrors Theme.kt but accessible without the :app module dependency
private val OverlayBg       = Color(0xFF161B26)
private val OverlayBorder   = Color(0xFF2D3348)
private val OverlayText     = Color(0xFFDEE3F0)
private val OverlayMuted    = Color(0xFF7B87A8)
private val OverlayPrimary  = Color(0xFF7B9EFF)
private val OverlayGreen    = Color(0xFF52D68A)
private val OverlayGreenDim = Color(0xFF1A3A2A)
private val OverlayRed      = Color(0xFFFF6B6B)
private val OverlayRedDim   = Color(0xFF2A1A1A)

private val overlayColorScheme = darkColorScheme(
    primary           = OverlayPrimary,
    surface           = OverlayBg,
    onSurface         = OverlayText,
    onSurfaceVariant  = OverlayMuted,
    outline           = OverlayBorder,
)

@Composable
internal fun OverlayBubble(
    isRunning: State<Boolean>,
    currentStep: State<String>,
    passCount: State<Int>,
    failCount: State<Int>,
    isRecording: State<Boolean>,
    recordedStepCount: State<Int>,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onOpenRecorder: () -> Unit,
    onMove: (Float, Float) -> Unit,
    onDismiss: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val running = isRunning.value
    val step = currentStep.value
    val passed = passCount.value
    val failed = failCount.value
    val recording = isRecording.value
    val stepCount = recordedStepCount.value

    MaterialTheme(colorScheme = overlayColorScheme) {
        if (expanded) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = OverlayBg,
                modifier = Modifier
                    .width(220.dp)
                    .border(1.dp, OverlayBorder, RoundedCornerShape(16.dp))
                    .padding(0.dp),
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Drag handle + collapse button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .pointerInput(Unit) {
                                detectDragGestures { _, amount -> onMove(amount.x, amount.y) }
                            },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "⠿  DroidPilot",
                            style = MaterialTheme.typography.labelSmall,
                            color = OverlayMuted,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(
                            onClick = { expanded = false },
                            modifier = Modifier.size(24.dp),
                        ) {
                            Icon(
                                Icons.Default.KeyboardArrowDown,
                                contentDescription = "Collapse",
                                modifier = Modifier.size(16.dp),
                                tint = OverlayMuted,
                            )
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(24.dp),
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close",
                                modifier = Modifier.size(14.dp),
                                tint = OverlayMuted,
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    // Pass / fail counters
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        CounterChip(value = passed, label = "pass", color = OverlayGreen, bg = OverlayGreenDim)
                        CounterChip(value = failed, label = "fail", color = OverlayRed, bg = OverlayRedDim)
                    }

                    Spacer(Modifier.height(10.dp))

                    // Current step
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF0A0D14), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    ) {
                        Text(
                            text = if (running && step.isNotEmpty()) "▶ $step" else "idle",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            color = if (running) OverlayGreen else OverlayMuted,
                            maxLines = 2,
                        )
                    }

                    Spacer(Modifier.height(10.dp))

                    // Recorder controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        if (recording) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.FiberManualRecord,
                                    contentDescription = null,
                                    tint = OverlayRed,
                                    modifier = Modifier.size(10.dp),
                                )
                                Text(
                                    " $stepCount steps",
                                    fontSize = 11.sp,
                                    color = OverlayRed,
                                )
                            }
                            IconButton(onClick = onStopRecording, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Stop, contentDescription = "Stop recording", modifier = Modifier.size(16.dp), tint = OverlayRed)
                            }
                        } else {
                            Text(
                                if (stepCount > 0) "$stepCount steps recorded" else "Recorder",
                                fontSize = 11.sp,
                                color = OverlayMuted,
                            )
                            Row {
                                if (stepCount > 0) {
                                    IconButton(onClick = onOpenRecorder, modifier = Modifier.size(28.dp)) {
                                        Icon(Icons.Default.FiberManualRecord, contentDescription = "Review steps", modifier = Modifier.size(14.dp), tint = OverlayPrimary)
                                    }
                                }
                                IconButton(onClick = onStartRecording, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Default.FiberManualRecord, contentDescription = "Start recording", modifier = Modifier.size(16.dp), tint = OverlayMuted)
                                }
                            }
                        }
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(OverlayBg, RoundedCornerShape(32.dp))
                    .border(1.dp, OverlayBorder, RoundedCornerShape(32.dp))
                    .clickable { expanded = true }
                    .pointerInput(Unit) {
                        detectDragGestures { _, amount -> onMove(amount.x, amount.y) }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = passed.toString(),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = OverlayGreen,
                        lineHeight = 14.sp,
                    )
                    Text(
                        text = failed.toString(),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = OverlayRed,
                        lineHeight = 14.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun CounterChip(value: Int, label: String, color: Color, bg: Color) {
    Row(
        modifier = Modifier
            .background(bg, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(
            text = value.toString(),
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            color = color,
            lineHeight = 20.sp,
        )
        Text(
            text = label,
            fontSize = 10.sp,
            color = color.copy(alpha = 0.7f),
            modifier = Modifier.padding(bottom = 1.dp),
        )
    }
}
