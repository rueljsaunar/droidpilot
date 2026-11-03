package com.droidpilot.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.droidpilot.app.ui.HomeScreen

sealed class Route(val path: String) {
    object Home : Route("home")
}

@Composable
fun AppNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Route.Home.path) {
        composable(Route.Home.path) { HomeScreen() }
    }
}
