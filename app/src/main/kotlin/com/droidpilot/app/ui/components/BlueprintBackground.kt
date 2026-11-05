package com.droidpilot.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun BlueprintBackground(
    modifier: Modifier = Modifier,
    gridColor: Color = Color(0xFF7B9EFF).copy(alpha = 0.05f),
    cellSize: Dp = 32.dp,
    content: @Composable () -> Unit,
) {
    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cell = cellSize.toPx()
            val cols = (size.width / cell).toInt() + 1
            val rows = (size.height / cell).toInt() + 1
            for (col in 0..cols) {
                val x = col * cell
                drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 0.5f)
            }
            for (row in 0..rows) {
                val y = row * cell
                drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 0.5f)
            }
        }
        content()
    }
}
