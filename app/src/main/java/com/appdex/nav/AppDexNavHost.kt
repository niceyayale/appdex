package com.appdex.nav

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.appdex.analyzer.ApkAnalyzerScreen
import com.appdex.editor.EditorScreen
import com.appdex.files.FileManagerScreen
import com.appdex.settings.SettingsScreen
import com.appdex.ui.Route

data class BottomNavItem(
    val route: Any,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

@Composable
fun AppDexApp() {
    val navController = rememberNavController()

    val items = listOf(
        BottomNavItem(Route.Files, "Files", Icons.Default.Folder),
        BottomNavItem(Route.Editor, "Editor", Icons.Default.Code),
        BottomNavItem(Route.Analyzer, "Analyzer", Icons.Default.Analytics),
        BottomNavItem(Route.Settings, "Settings", Icons.Default.Settings)
    )

    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination
            NavigationBar {
                items.forEach { item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                        selected = currentDestination?.hierarchy?.any { it.route == item.route::class.qualifiedName } == true,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(Route.Files) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Route.Files,
            modifier = Modifier.padding(padding)
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
}
