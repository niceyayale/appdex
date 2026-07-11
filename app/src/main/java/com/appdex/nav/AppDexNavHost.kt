package com.appdex.nav

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.appdex.analyzer.ApkAnalyzerScreen
import com.appdex.common.MediaNavigationBus
import com.appdex.common.MediaOpenRequest
import com.appdex.editor.EditorScreen
import com.appdex.files.FileManagerScreen
import com.appdex.player.audio.AudioPlayerScreen
import com.appdex.player.image.ImageViewerScreen
import com.appdex.player.video.VideoPlayerScreen
import com.appdex.remote.RemoteScreen
import com.appdex.settings.SettingsScreen
import com.appdex.terminal.TerminalScreen
import com.appdex.tools.ToolsScreen
import com.appdex.ui.Route
import kotlinx.coroutines.flow.collectLatest

data class BottomNavItem(
    val route: Any,
    val label: String,
    val icon: ImageVector
)

@Composable
fun AppDexApp() {
    val navController = rememberNavController()

    val items = listOf(
        BottomNavItem(Route.Files, "Files", Icons.Default.Folder),
        BottomNavItem(Route.Editor, "Editor", Icons.Default.Code),
        BottomNavItem(Route.Analyzer, "APK", Icons.Default.Analytics),
        BottomNavItem(Route.Terminal, "Terminal", Icons.Default.Terminal),
        BottomNavItem(Route.Tools, "Tools", Icons.Default.Build),
        BottomNavItem(Route.Remote, "Remote", Icons.Default.Cloud),
        BottomNavItem(Route.Settings, "Settings", Icons.Default.Settings)
    )

    var mediaRequest by remember { mutableStateOf<MediaOpenRequest?>(null) }

    LaunchedEffect(Unit) {
        MediaNavigationBus.events.collectLatest { request ->
            mediaRequest = request
        }
    }

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
            composable<Route.Terminal> {
                TerminalScreen()
            }
            composable<Route.Tools> {
                ToolsScreen()
            }
            composable<Route.Remote> {
                RemoteScreen()
            }
            composable<Route.Settings> {
                SettingsScreen()
            }
        }
    }

    mediaRequest?.let { request ->
        when (request) {
            is MediaOpenRequest.Image -> {
                ImageViewerScreen(
                    imagePaths = request.allPaths,
                    initialIndex = request.allPaths.indexOf(request.path).coerceAtLeast(0),
                    onDismiss = { mediaRequest = null }
                )
            }
            is MediaOpenRequest.Audio -> {
                AudioPlayerScreen(
                    audioPaths = request.allPaths,
                    initialIndex = request.index,
                    onDismiss = { mediaRequest = null }
                )
            }
            is MediaOpenRequest.Video -> {
                VideoPlayerScreen(
                    videoPath = request.path,
                    onDismiss = { mediaRequest = null }
                )
            }
            is MediaOpenRequest.Apk -> {
                mediaRequest = null
            }
        }
    }
}
