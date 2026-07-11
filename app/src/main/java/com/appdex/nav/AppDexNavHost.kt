package com.appdex.nav

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.appdex.analyzer.ApkAnalyzerScreen
import com.appdex.editor.EditorScreen
import com.appdex.files.FileManagerScreen
import com.appdex.settings.SettingsScreen
import com.appdex.ui.Route

@Composable
fun AppDexNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Route.Files
    ) {
        composable<Route.Files> {
            FileManagerScreen()
        }
        composable<Route.Editor> {
            EditorScreen()
        }
        composable<Route.Analyzer> {
            ApkAnalyzerScreen()
        }
        composable<Route.Settings> {
            SettingsScreen()
        }
    }
}
