package com.droidpilot.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.droidpilot.app.navigation.AppNavHost
import com.droidpilot.app.ui.theme.DroidPilotTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DroidPilotTheme {
                AppNavHost()
            }
        }
    }
}
