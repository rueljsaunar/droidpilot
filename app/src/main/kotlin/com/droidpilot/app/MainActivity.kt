package com.droidpilot.app

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.droidpilot.app.navigation.AppNavHost
import com.droidpilot.app.ui.theme.DroidPilotTheme
import com.droidpilot.feature.overlay.OverlayService
import com.droidpilot.app.ui.ShizukuStatusBanner
import com.droidpilot.app.ui.ShizukuViewModel
import com.droidpilot.backend.shizuku.ShizukuRepository
import dagger.hilt.android.AndroidEntryPoint
import rikka.shizuku.Shizuku
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var shizukuRepository: ShizukuRepository

    private val permissionListener = Shizuku.OnRequestPermissionResultListener { _: Int, grantResult: Int ->
        shizukuRepository.onPermissionResult(grantResult == PackageManager.PERMISSION_GRANTED)
    }

    private var openRecorderOnStart = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        openRecorderOnStart = intent?.getBooleanExtra(OverlayService.EXTRA_OPEN_RECORDER, false) == true
        Shizuku.addRequestPermissionResultListener(permissionListener)
        enableEdgeToEdge()
        setContent {
            DroidPilotTheme {
                val navController = rememberNavController()
                val shizukuVm: ShizukuViewModel = viewModel()
                val shizukuState by shizukuVm.state.collectAsState()
                Column(modifier = Modifier.fillMaxSize()) {
                    ShizukuStatusBanner(
                        state = shizukuState,
                        onRequestPermission = shizukuVm::requestPermission,
                    )
                    AppNavHost(
                        navController = navController,
                        openRecorderOnStart = openRecorderOnStart,
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        Shizuku.removeRequestPermissionResultListener(permissionListener)
    }
}
