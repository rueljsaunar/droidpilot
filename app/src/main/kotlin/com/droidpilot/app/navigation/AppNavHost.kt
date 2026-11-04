package com.droidpilot.app.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.droidpilot.feature.editor.EditorScreen
import com.droidpilot.feature.projects.ui.ProjectListScreen
import com.droidpilot.feature.projects.ui.ProjectSettingsScreen
import com.droidpilot.feature.projects.ui.RunHistoryScreen

sealed class Route(val path: String) {
    object Projects : Route("projects")
    object Editor : Route("editor/{projectId}/{encodedDirPath}") {
        fun build(id: Long, dir: String) = "editor/$id/${Uri.encode(dir)}"
    }
    object Settings : Route("settings/{projectId}") {
        fun build(id: Long) = "settings/$id"
    }
    object History : Route("history/{projectId}") {
        fun build(id: Long) = "history/$id"
    }
}

@Composable
fun AppNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Route.Projects.path) {
        composable(Route.Projects.path) {
            ProjectListScreen(
                onOpenProject = { id, dir ->
                    navController.navigate(Route.Editor.build(id, dir))
                },
                onOpenSettings = { id ->
                    navController.navigate(Route.Settings.build(id))
                },
                onViewHistory = { id ->
                    navController.navigate(Route.History.build(id))
                },
            )
        }
        composable(
            Route.Editor.path,
            arguments = listOf(
                navArgument("projectId") { type = NavType.LongType },
                navArgument("encodedDirPath") { type = NavType.StringType },
            ),
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getLong("projectId") ?: return@composable
            val encodedDir = backStackEntry.arguments?.getString("encodedDirPath") ?: return@composable
            EditorScreen(
                projectId = id,
                encodedDirPath = encodedDir,
                onBack = { navController.navigateUp() },
            )
        }
        composable(
            Route.Settings.path,
            arguments = listOf(
                navArgument("projectId") { type = NavType.LongType },
            ),
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getLong("projectId") ?: return@composable
            ProjectSettingsScreen(
                projectId = id,
                onNavigateUp = { navController.navigateUp() },
            )
        }
        composable(
            Route.History.path,
            arguments = listOf(
                navArgument("projectId") { type = NavType.LongType },
            ),
        ) {
            RunHistoryScreen(onNavigateUp = { navController.navigateUp() })
        }
    }
}
