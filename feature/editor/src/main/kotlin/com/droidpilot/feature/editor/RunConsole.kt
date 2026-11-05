package com.droidpilot.feature.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
public fun RunConsole(
    output: List<String>,
    isRunning: Boolean = false,
    modifier: Modifier = Modifier,
    onClear: (() -> Unit)? = null,
) {
    val listState = rememberLazyListState()

    val passCount = remember(output) { output.count { it.contains("PASS") || it.startsWith("[PASS]") } }
    val failCount = remember(output) { output.count { it.contains("FAIL") || it.startsWith("[FAIL]") } }
    val total = passCount + failCount
    val progress = if (total > 0) passCount.toFloat() / total else 0f

    LaunchedEffect(output.size) {
        if (output.isNotEmpty()) listState.animateScrollToItem(output.lastIndex)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(240.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        // Header: stats row
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StatChip(
                    value = passCount,
                    label = "passed",
                    color = MaterialTheme.colorScheme.secondary,
                )
                Spacer(Modifier.width(12.dp))
                StatChip(
                    value = failCount,
                    label = "failed",
                    color = MaterialTheme.colorScheme.error,
                )
                if (isRunning) {
                    Spacer(Modifier.width(12.dp))
                    StatChip(
                        value = 1,
                        label = "running",
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
                Spacer(Modifier.weight(1f))
                if (onClear != null) {
                    IconButton(
                        onClick = onClear,
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            Icons.Default.Clear,
                            contentDescription = "Clear",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Spacer(Modifier.height(6.dp))

            LinearProgressIndicator(
                progress = { if (isRunning) progress else if (total > 0) progress else 0f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = if (failCount > 0) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.secondary,
                trackColor = MaterialTheme.colorScheme.outlineVariant,
            )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)

        // Log output
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(vertical = 4.dp),
        ) {
            items(output) { line ->
                val (prefix, lineColor) = when {
                    line.contains("PASS") || line.startsWith("PASS") ->
                        "✓ " to MaterialTheme.colorScheme.secondary
                    line.contains("FAIL") || line.startsWith("FAIL") ||
                        line.startsWith("ERROR") || line.startsWith("Traceback") ->
                        "✗ " to MaterialTheme.colorScheme.error
                    line.startsWith("[") && line.length > 10 ->
                        "▶ " to MaterialTheme.colorScheme.onSurfaceVariant
                    else ->
                        "  " to MaterialTheme.colorScheme.onSurfaceVariant
                }
                Text(
                    text = "$prefix$line",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    color = lineColor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 10.dp, vertical = 1.dp),
                )
            }
        }
    }
}

@Composable
private fun StatChip(value: Int, label: String, color: androidx.compose.ui.graphics.Color) {
    Row(
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
            fontSize = 11.sp,
            color = color.copy(alpha = 0.7f),
            modifier = Modifier.padding(bottom = 1.dp),
        )
    }
}
