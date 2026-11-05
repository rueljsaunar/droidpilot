package com.droidpilot.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.droidpilot.app.ui.theme.DpGreen
import com.droidpilot.app.ui.theme.DpGreenDim
import com.droidpilot.app.ui.theme.DpMuted
import com.droidpilot.app.ui.theme.DpRed
import com.droidpilot.app.ui.theme.DpRedDim
import com.droidpilot.app.ui.theme.DpSurface2
import com.droidpilot.app.ui.theme.DpYellow
import com.droidpilot.app.ui.theme.DpYellowDim
import com.droidpilot.app.ui.theme.DpBorder

enum class RunStatus { PASS, FAILED, RUNNING, NO_RUN }

@Composable
fun StatusBadge(status: RunStatus, modifier: Modifier = Modifier) {
    val (label, fg, bg, border) = when (status) {
        RunStatus.PASS    -> Quad("PASS",    DpGreen,  DpGreenDim, DpGreen)
        RunStatus.FAILED  -> Quad("FAILED",  DpRed,    DpRedDim,   DpRed)
        RunStatus.RUNNING -> Quad("RUNNING", DpYellow, DpYellowDim, DpYellow)
        RunStatus.NO_RUN  -> Quad("NO RUN",  DpMuted,  DpSurface2,  DpBorder)
    }
    val dot = fg
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .background(bg, RoundedCornerShape(20.dp))
            .border(1.dp, border, RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(dot)
        )
        androidx.compose.foundation.layout.Spacer(Modifier.padding(horizontal = 3.dp))
        Text(
            text = label,
            color = fg,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.sp,
        )
    }
}

private data class Quad(val label: String, val fg: Color, val bg: Color, val border: Color)
