package com.droidpilot.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.droidpilot.backend.shizuku.ShizukuState

@Composable
fun ShizukuStatusBanner(
    state: ShizukuState,
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showOnboarding by rememberSaveable { mutableStateOf(false) }

    val visible = state != ShizukuState.Available && state != ShizukuState.Connecting

    AnimatedVisibility(
        visible = visible,
        enter = expandVertically(),
        exit = shrinkVertically(),
        modifier = modifier,
    ) {
        Surface(
            color = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Rounded.Warning, contentDescription = null)
                    Text(
                        text = when (state) {
                            ShizukuState.PermissionRequired -> "Shizuku permission required"
                            ShizukuState.PermissionDenied -> "Shizuku permission denied"
                            else -> "Shizuku not available — shell features disabled"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                when (state) {
                    ShizukuState.PermissionRequired -> TextButton(onClick = onRequestPermission) {
                        Text("Grant")
                    }
                    else -> TextButton(onClick = { showOnboarding = true }) {
                        Text("Set up")
                    }
                }
            }
        }
    }

    if (showOnboarding) {
        ShizukuOnboardingDialog(onDismiss = { showOnboarding = false })
    }
}
