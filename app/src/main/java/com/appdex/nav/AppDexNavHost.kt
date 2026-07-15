package com.appdex.nav

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.appdex.AppDexMainViewModel
import com.appdex.analyzer.ApkAnalyzerScreen
import com.appdex.analyzer.ApkAnalyzerViewModel
import com.appdex.analyzer.ApkAnalyzerIntent
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
import com.appdex.dex.DexBrowserScreen
import com.appdex.hex.HexEditorScreen
import com.appdex.signing.SigningScreen
import com.appdex.repack.RepackagingScreen
import com.appdex.diff.ApkDiffScreen
import com.appdex.security.SecurityScannerScreen
import com.appdex.size.SizeAnalyzerScreen
import com.appdex.axmleditor.AxmlEditorScreen
import com.appdex.arsceditor.ArscEditorScreen
import com.appdex.sqliteviewer.SqliteViewerScreen
import com.appdex.elfviewer.ElfViewerScreen
import com.appdex.ui.Route
import com.appdex.ui.HomeScreen
import com.appdex.ui.TaskScreen
import com.appdex.ui.AiScreen
import com.appdex.ui.components.AppDexBottomNav
import com.appdex.ui.components.AppDexNavItem
import com.appdex.ui.components.NavHomeIcon
import com.appdex.ui.components.NavTaskIcon
import com.appdex.ui.components.NavFolderIcon
import com.appdex.ui.components.NavToolsIcon
import com.appdex.ui.components.NavAiIcon
import com.appdex.ui.components.NavSettingsIcon
import com.appdex.ui.theme.AppDexTheme
import kotlinx.coroutines.flow.collectLatest

@Composable
fun AppDexApp() {
    val navController = rememberNavController()
    val mainViewModel: AppDexMainViewModel = hiltViewModel()

    val navItems = listOf(
        AppDexNavItem("主页", NavHomeIcon),
        AppDexNavItem("任务", NavTaskIcon),
        AppDexNavItem("文件", NavFolderIcon),
        AppDexNavItem("工具", NavToolsIcon),
        AppDexNavItem("AI", NavAiIcon),
        AppDexNavItem("设置", NavSettingsIcon)
    )

    var mediaRequest by remember { mutableStateOf<MediaOpenRequest?>(null) }

    LaunchedEffect(Unit) {
        MediaNavigationBus.events.collectLatest { request ->
            mediaRequest = request
        }
    }

    // Collect states from main ViewModel
    val sessions by mainViewModel.sessions.collectAsStateWithLifecycle(initialValue = emptyList())
    val isAnalyzing by mainViewModel.isAnalyzing.collectAsStateWithLifecycle(initialValue = false)
    val aiMessages by mainViewModel.aiMessages.collectAsStateWithLifecycle(initialValue = emptyList())
    val isAiResponding by mainViewModel.isAiResponding.collectAsStateWithLifecycle(initialValue = false)
    val aiConfig by mainViewModel.aiConfig.collectAsStateWithLifecycle(
        initialValue = com.appdex.data.ai.AiConfig()
    )
    val displayMode by mainViewModel.displayMode.collectAsStateWithLifecycle(
        initialValue = com.appdex.data.session.ToolDisplayMode.NORMAL
    )
    val suggestedQuestions by mainViewModel.suggestedQuestions.collectAsStateWithLifecycle(
        initialValue = emptyList()
    )

    val currentSession by mainViewModel.currentSession.collectAsStateWithLifecycle(initialValue = null)

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRouteName = currentDestination?.route?.let { routeName ->
        when {
            routeName.contains("Home") -> "主页"
            routeName.contains("Task") || routeName.contains("Analyzer") -> "任务"
            routeName.contains("Files") -> "文件"
            routeName.contains("Tools") -> "工具"
            routeName.contains("Ai") && !routeName.contains("Analyzer") -> "AI"
            routeName.contains("Settings") -> "设置"
            else -> "主页"
        }
    } ?: "主页"

    val showBottomBar = currentDestination?.route?.let { route ->
        route.contains("Home") || route.contains("Task") || route.contains("Analyzer") ||
        route.contains("Files") || route.contains("Tools") ||
        (route.contains("Ai") && !route.contains("Analyzer")) || route.contains("Settings")
    } ?: false

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppDexTheme.colors.background)
            .statusBarsPadding()
    ) {
        Box(modifier = Modifier.weight(1f)) {
            NavHost(
                navController = navController,
                startDestination = Route.Home,
                modifier = Modifier.fillMaxSize()
            ) {
                // ── Home Tab ──
                composable<Route.Home> {
                    val quickActions by mainViewModel.quickActions.collectAsStateWithLifecycle(
                        initialValue = emptyList()
                    )
                    HomeScreen(
                        sessions = sessions,
                        suggestedQuestions = suggestedQuestions,
                        quickActions = quickActions,
                        onAnalyzeApk = { uri ->
                            mainViewModel.openApk(uri)
                            navController.navigate(Route.Task) {
                                popUpTo(Route.Home) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onScanInstalled = {
                            navController.navigate(Route.Task) {
                                popUpTo(Route.Home) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onOpenSession = { sessionId ->
                            mainViewModel.selectSession(sessionId)
                            navController.navigate(Route.Task)
                        },
                        onNavigateToAi = {
                            navController.navigate(Route.Ai) {
                                popUpTo(Route.Home) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onAskQuestion = { question ->
                            navController.navigate(Route.Ai) {
                                popUpTo(Route.Home) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                            mainViewModel.sendAiMessage(question)
                        },
                        onQuickAction = { action ->
                            when (action.action) {
                                "analyze" -> {
                                    // Trigger file picker via the hero button
                                }
                                "scan_installed" -> {
                                    navController.navigate(Route.Task) {
                                        popUpTo(Route.Home) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                                "ai", "chat" -> {
                                    navController.navigate(Route.Ai) {
                                        popUpTo(Route.Home) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                                "files", "folder" -> {
                                    navController.navigate(Route.Files) {
                                        popUpTo(Route.Home) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                                "report", "scan" -> {
                                    navController.navigate(Route.Task) {
                                        popUpTo(Route.Home) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                                "edit" -> {
                                    navController.navigate(Route.Tools) {
                                        popUpTo(Route.Home) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                                "signing", "key" -> {
                                    navController.navigate(Route.ApkSigning()) {
                                        popUpTo(Route.Home) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                                else -> {
                                    navController.navigate(Route.Task) {
                                        popUpTo(Route.Home) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            }
                        },
                        onNavigateToTask = {
                            navController.navigate(Route.Task) {
                                popUpTo(Route.Home) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }

                // ── Task Tab ──
                composable<Route.Task> {
                    val workflowProgress by mainViewModel.workflowProgress.collectAsStateWithLifecycle(
                        initialValue = 0f
                    )
                    val workflowStepLabel by mainViewModel.currentWorkflowStep.collectAsStateWithLifecycle(
                        initialValue = ""
                    )
                    TaskScreen(
                        sessions = sessions,
                        currentSession = currentSession,
                        isAnalyzing = isAnalyzing,
                        workflowProgress = workflowProgress,
                        workflowStepLabel = workflowStepLabel,
                        onOpenApk = { uri ->
                            mainViewModel.openApk(uri)
                        },
                        onSelectSession = { sessionId ->
                            mainViewModel.selectSession(sessionId)
                        },
                        onDeleteSession = { sessionId ->
                            mainViewModel.deleteSession(sessionId)
                        },
                        onNavigate = { route ->
                            when (route) {
                                "终端" -> navController.navigate(Route.Terminal)
                                "编辑器" -> navController.navigate(Route.Editor())
                            }
                        },
                        onOpenDexBrowser = { apkPath ->
                            navController.navigate(Route.DexBrowser(apkPath = apkPath))
                        },
                        onOpenSigning = { apkPath ->
                            navController.navigate(Route.ApkSigning(apkPath = apkPath))
                        },
                        onOpenRepack = { apkPath ->
                            navController.navigate(Route.ApkRepack(apkPath = apkPath))
                        },
                        onOpenSecurity = { apkPath ->
                            navController.navigate(Route.ApkSecurity(apkPath = apkPath))
                        },
                        onOpenDetail = {
                            navController.navigate(Route.ApkDetail)
                        }
                    )
                }

                // ── Files Tab ──
                composable<Route.Files> {
                    FileManagerScreen(
                        onOpenTextFile = { path ->
                            navController.navigate(Route.Editor(filePath = path))
                        },
                        onOpenHexFile = { path ->
                            navController.navigate(Route.HexEditor(filePath = path))
                        },
                        onNavigateToTab = { tab ->
                            when (tab) {
                                "终端" -> navController.navigate(Route.Terminal)
                                "工具" -> navController.navigate(Route.Tools)
                            }
                        }
                    )
                }

                // ── Tools Tab ──
                composable<Route.Tools> {
                    ToolsScreen(
                        onBack = { navController.popBackStack() },
                        onNavigate = { route ->
                            when (route) {
                                "终端" -> navController.navigate(Route.Terminal)
                                "编辑器" -> navController.navigate(Route.Editor())
                                "远程管理" -> navController.navigate(Route.Remote)
                                "分析" -> navController.navigate(Route.Task)
                            }
                        },
                        onOpenDiff = { navController.navigate(Route.ApkDiff) },
                        onOpenSecurity = { navController.navigate(Route.ApkSecurity()) },
                        onOpenSizeAnalyzer = { navController.navigate(Route.ApkSizeAnalyzer()) },
                        onOpenDexBrowser = { navController.navigate(Route.DexBrowser()) },
                        onOpenSigning = { navController.navigate(Route.ApkSigning()) },
                        onOpenAxmlEditor = { navController.navigate(Route.AxmlEditor()) },
                        onOpenArscViewer = { navController.navigate(Route.ArscViewer()) },
                        onOpenSqliteViewer = { navController.navigate(Route.SqliteViewer()) },
                        onOpenElfViewer = { navController.navigate(Route.ElfViewer()) },
                        displayMode = displayMode
                    )
                }

                // ── AI Tab ──
                composable<Route.Ai> {
                    AiScreen(
                        messages = aiMessages,
                        isAiResponding = isAiResponding,
                        aiConfig = aiConfig,
                        suggestedQuestions = suggestedQuestions,
                        onSendMessage = { text -> mainViewModel.sendAiMessage(text) },
                        onActionClick = { actionCard ->
                            val session = mainViewModel.getCurrentSession()
                            when (actionCard.route) {
                                "permissions", "security" -> navController.navigate(
                                    Route.ApkSecurity(apkPath = session?.apkFilePath)
                                )
                                "dex" -> navController.navigate(
                                    Route.DexBrowser(apkPath = session?.apkFilePath)
                                )
                                "signing" -> navController.navigate(
                                    Route.ApkSigning(apkPath = session?.apkFilePath)
                                )
                                "repack" -> navController.navigate(
                                    Route.ApkRepack(apkPath = session?.apkFilePath)
                                )
                                "size" -> navController.navigate(
                                    Route.ApkSizeAnalyzer(apkPath = session?.apkFilePath)
                                )
                                "axml" -> navController.navigate(
                                    Route.AxmlEditor(apkPath = session?.apkFilePath)
                                )
                                "arsc" -> navController.navigate(
                                    Route.ArscViewer(apkPath = session?.apkFilePath)
                                )
                                "sqlite" -> navController.navigate(Route.SqliteViewer())
                                "elf" -> navController.navigate(Route.ElfViewer())
                                "editor" -> navController.navigate(Route.Editor())
                                "terminal" -> navController.navigate(Route.Terminal)
                                "files" -> navController.navigate(Route.Files)
                                else -> {}
                            }
                        },
                        onNavigateToSettings = {
                            navController.navigate(Route.Settings) {
                                popUpTo(Route.Ai) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }

                // ── Settings Tab ──
                composable<Route.Settings> {
                    SettingsScreen()
                }

                // ── Sub-routes ──
                composable<Route.Editor> { backStackEntry ->
                    val route = backStackEntry.toRoute<Route.Editor>()
                    EditorScreen(
                        filePath = route.filePath,
                        onBack = { navController.popBackStack() }
                    )
                }

                composable<Route.Analyzer> {
                    val viewModel: ApkAnalyzerViewModel = hiltViewModel()
                    ApkAnalyzerScreen(
                        onOpenDetail = { navController.navigate(Route.ApkDetail) },
                        viewModel = viewModel
                    )
                }

                composable<Route.ApkDetail> { backStackEntry ->
                    val parentEntry = remember(backStackEntry) {
                        navController.getBackStackEntry(Route.Analyzer)
                    }
                    val viewModel: ApkAnalyzerViewModel = hiltViewModel(parentEntry)
                    com.appdex.analyzer.ApkDetailScreen(
                        onBack = { navController.popBackStack() },
                        onNavigate = { route ->
                            when (route) {
                                "终端" -> navController.navigate(Route.Terminal)
                                "编辑器" -> navController.navigate(Route.Editor())
                            }
                        },
                        onOpenDexBrowser = {
                            viewModel.state.value.apkFilePath?.let { path ->
                                navController.navigate(Route.DexBrowser(apkPath = path))
                            }
                        },
                        onOpenSigning = {
                            viewModel.state.value.apkFilePath?.let { path ->
                                navController.navigate(Route.ApkSigning(apkPath = path))
                            }
                        },
                        onOpenRepack = {
                            viewModel.state.value.apkFilePath?.let { path ->
                                navController.navigate(Route.ApkRepack(apkPath = path))
                            }
                        },
                        viewModel = viewModel
                    )
                }

                composable<Route.Terminal> {
                    TerminalScreen(onBack = { navController.popBackStack() })
                }

                composable<Route.Remote> {
                    RemoteScreen(onBack = { navController.popBackStack() })
                }

                composable<Route.DexBrowser> { backStackEntry ->
                    val route = backStackEntry.toRoute<Route.DexBrowser>()
                    DexBrowserScreen(apkPath = route.apkPath ?: "", onBack = { navController.popBackStack() })
                }

                composable<Route.HexEditor> { backStackEntry ->
                    val route = backStackEntry.toRoute<Route.HexEditor>()
                    HexEditorScreen(filePath = route.filePath, onBack = { navController.popBackStack() })
                }

                composable<Route.ApkSigning> { backStackEntry ->
                    val route = backStackEntry.toRoute<Route.ApkSigning>()
                    SigningScreen(apkPath = route.apkPath ?: "", onBack = { navController.popBackStack() })
                }

                composable<Route.ApkRepack> { backStackEntry ->
                    val route = backStackEntry.toRoute<Route.ApkRepack>()
                    RepackagingScreen(apkPath = route.apkPath ?: "", onBack = { navController.popBackStack() })
                }

                composable<Route.ApkDiff> {
                    ApkDiffScreen(onBack = { navController.popBackStack() })
                }

                composable<Route.ApkSecurity> { backStackEntry ->
                    val route = backStackEntry.toRoute<Route.ApkSecurity>()
                    SecurityScannerScreen(apkPath = route.apkPath, onBack = { navController.popBackStack() })
                }

                composable<Route.ApkSizeAnalyzer> { backStackEntry ->
                    val route = backStackEntry.toRoute<Route.ApkSizeAnalyzer>()
                    SizeAnalyzerScreen(apkPath = route.apkPath, onBack = { navController.popBackStack() })
                }

                composable<Route.AxmlEditor> { backStackEntry ->
                    val route = backStackEntry.toRoute<Route.AxmlEditor>()
                    AxmlEditorScreen(apkPath = route.apkPath, entryName = route.entryName, onBack = { navController.popBackStack() })
                }

                composable<Route.ArscViewer> { backStackEntry ->
                    val route = backStackEntry.toRoute<Route.ArscViewer>()
                    ArscEditorScreen(apkPath = route.apkPath, onBack = { navController.popBackStack() })
                }

                composable<Route.SqliteViewer> { backStackEntry ->
                    val route = backStackEntry.toRoute<Route.SqliteViewer>()
                    SqliteViewerScreen(dbPath = route.dbPath, onBack = { navController.popBackStack() })
                }

                composable<Route.ElfViewer> { backStackEntry ->
                    val route = backStackEntry.toRoute<Route.ElfViewer>()
                    ElfViewerScreen(filePath = route.filePath, onBack = { navController.popBackStack() })
                }
            }
        }

        if (showBottomBar) {
            AppDexBottomNav(
                items = navItems,
                currentRoute = currentRouteName,
                onNavigate = { index ->
                    val route = when (index) {
                        0 -> Route.Home
                        1 -> Route.Task
                        2 -> Route.Files
                        3 -> Route.Tools
                        4 -> Route.Ai
                        5 -> Route.Settings
                        else -> Route.Home
                    }
                    navController.navigate(route) {
                        popUpTo(Route.Home) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }

    // Handle media requests
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
                VideoPlayerScreen(videoPath = request.path, onDismiss = { mediaRequest = null })
            }
            is MediaOpenRequest.Apk -> {
                mainViewModel.openApkPath(request.path)
                navController.navigate(Route.Task) {
                    popUpTo(Route.Home) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
                mediaRequest = null
            }
        }
    }
}
