package com.appdex.nav

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import com.appdex.AppXMainViewModel
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
import com.appdex.settings.AiProvidersScreen
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
import com.appdex.ui.WorkspaceScreen
import com.appdex.ui.ReportScreen
import com.appdex.ui.AiScreen
import com.appdex.ui.CommandPalette
import com.appdex.ui.components.AppXBottomNav
import com.appdex.ui.components.AppXNavItem
import com.appdex.ui.components.NavAiIcon
import com.appdex.ui.components.NavTaskIcon
import com.appdex.ui.components.NavSettingsIcon
import com.appdex.ui.theme.AppXTheme
import kotlinx.coroutines.flow.collectLatest

@androidx.annotation.OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Suppress("RestrictedApi")
@Composable
fun AppXApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    // ── Custom NavHostController with deep link processing disabled ──
    // Compose Navigation 2.8.x auto-generates deep link patterns for @Serializable
    // routes. The NavController.handleDeepLink() method processes the activity's
    // intent on lifecycle events (ON_CREATE, ON_RESUME) and navigates to any
    // matching route with a "deepLinkIntent" marker. This causes a navigation
    // loop (HexEditor → DexBrowser → ElfViewer → AxmlEditor) after file picker
    // callbacks trigger activity lifecycle events.
    //
    // Fix: Override handleDeepLink() to always return false, completely disabling
    // deep link processing. We manually register the same navigators that
    // rememberNavController() would add (ComposeNavGraphNavigator, ComposeNavigator,
    // DialogNavigator) so that composable<Route.X> { ... } works correctly.
    val navController = remember(context) {
        object : androidx.navigation.NavHostController(context) {
            override fun handleDeepLink(intent: android.content.Intent?): Boolean {
                // Disable all deep link processing to prevent navigation issues
                return false
            }
        }.apply {
            val provider = navigatorProvider
            // ComposeNavGraphNavigator is internal in navigation-compose, so we
            // create it via reflection. It extends NavGraphNavigator and provides
            // Compose-specific lifecycle handling for nested navigation graphs.
            try {
                val clazz = Class.forName("androidx.navigation.compose.ComposeNavGraphNavigator")
                val constructor = clazz.getConstructor(androidx.navigation.NavigatorProvider::class.java)
                val navigator = constructor.newInstance(provider) as androidx.navigation.Navigator<*>
                provider.addNavigator(navigator)
            } catch (e: Exception) {
                android.util.Log.w("AppX", "Could not register ComposeNavGraphNavigator, using default", e)
            }
            provider.addNavigator(
                androidx.navigation.compose.ComposeNavigator()
            )
            provider.addNavigator(
                androidx.navigation.compose.DialogNavigator()
            )
        }
    }
    val mainViewModel: AppXMainViewModel = hiltViewModel()

    // ── Navigation debug listener ──
    androidx.compose.runtime.DisposableEffect(navController) {
        val listener = androidx.navigation.NavController.OnDestinationChangedListener { controller, destination, arguments ->
            android.util.Log.d("AppX", "NAV_EVENT: route=${destination.route}")
        }
        navController.addOnDestinationChangedListener(listener)
        onDispose { navController.removeOnDestinationChangedListener(listener) }
    }

    // ── 3-Tab Navigation: AI / Workspace / Settings ──
    val navItems = listOf(
        AppXNavItem("AI", NavAiIcon),
        AppXNavItem("工作区", NavTaskIcon),
        AppXNavItem("设置", NavSettingsIcon)
    )

    var mediaRequest by remember { mutableStateOf<MediaOpenRequest?>(null) }
    var showCommandPalette by remember { mutableStateOf(false) }
    val pendingNavigation = remember { mutableStateOf<Route?>(null) }

    // ── Handle APK opened from external source (file manager, etc.) ──
    val activity = context as? com.appdex.AppXActivity
    val pendingApkUri = activity?.pendingApkUri

    // Debug: log the activity's intent to track deep link issue
    LaunchedEffect(Unit) {
        val intent = (context as? android.app.Activity)?.intent
        android.util.Log.d("AppX", "Activity intent on launch: action=${intent?.action}, data=${intent?.data}, flags=${intent?.flags}")
    }

    // ── Deep link prevention ──
    // Clear the activity intent on every recomposition and on resume to prevent
    // Compose Navigation from processing stale deep links.
    androidx.compose.runtime.SideEffect {
        val act = context as? android.app.Activity
        val currentIntent = act?.intent
        if (currentIntent?.data != null ||
            currentIntent?.action == android.content.Intent.ACTION_VIEW) {
            act?.intent = android.content.Intent(android.content.Intent.ACTION_MAIN).apply {
                setPackage(act.packageName)
                addCategory(android.content.Intent.CATEGORY_LAUNCHER)
            }
        }
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                val act = context as? android.app.Activity
                val currentIntent = act?.intent
                if (currentIntent?.data != null ||
                    currentIntent?.action == android.content.Intent.ACTION_VIEW) {
                    act?.intent = android.content.Intent(android.content.Intent.ACTION_MAIN).apply {
                        setPackage(act.packageName)
                        addCategory(android.content.Intent.CATEGORY_LAUNCHER)
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(pendingApkUri?.value) {
        val uri = pendingApkUri?.value
        if (uri != null) {
            android.util.Log.d("AppX", "Opening APK from external intent: $uri")
            mainViewModel.openApk(uri)
            navController.navigate(Route.Workspace) {
                popUpTo(Route.Ai) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
            // Clear the pending URI
            activity?.pendingApkUri?.value = null
        }
    }

    LaunchedEffect(Unit) {
        MediaNavigationBus.events.collectLatest { request ->
            mediaRequest = request
        }
    }

    // Process pending navigation (delayed from ActivityResult callback to avoid deep link conflict)
    LaunchedEffect(pendingNavigation.value) {
        val route = pendingNavigation.value
        if (route != null) {
            android.util.Log.d("AppX", "Processing pending navigation to: $route")
            navController.navigate(route) {
                popUpTo(Route.Ai) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
            pendingNavigation.value = null
        }
    }

    // Collect states from main ViewModel
    val sessions by mainViewModel.sessions.collectAsStateWithLifecycle(initialValue = emptyList())
    val isAnalyzing by mainViewModel.isAnalyzing.collectAsStateWithLifecycle(initialValue = false)
    val aiMessages by mainViewModel.aiMessages.collectAsStateWithLifecycle(initialValue = emptyList())
    val isAiResponding by mainViewModel.isAiResponding.collectAsStateWithLifecycle(initialValue = false)
    val streamingContent by mainViewModel.streamingContent.collectAsStateWithLifecycle(initialValue = "")
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
    val workflowProgress by mainViewModel.workflowProgress.collectAsStateWithLifecycle(initialValue = 0f)
    val workflowStepLabel by mainViewModel.currentWorkflowStep.collectAsStateWithLifecycle(initialValue = "")

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRouteName = currentDestination?.route?.let { routeName ->
        when {
            routeName.contains("Ai") && !routeName.contains("Analyzer") -> "AI"
            routeName.contains("Workspace") || routeName.contains("Task") || routeName.contains("Analyzer") -> "工作区"
            routeName.contains("Settings") -> "设置"
            else -> "AI"
        }
    } ?: "AI"

    val showBottomBar = currentDestination?.route?.let { route ->
        route.contains("Ai") && !route.contains("Analyzer") ||
        route.contains("Workspace") || route.contains("Task") ||
        route.contains("Settings")
    } ?: false

    // ── Deep link redirect REMOVED ──
    // The previous redirect logic was causing a navigation loop: legitimate
    // navigations to sub-routes (DexBrowser, AxmlEditor, etc.) were being
    // detected as "deep links" and redirected back to Workspace, preventing
    // users from ever reaching tool pages.
    //
    // The custom NavHostController already overrides handleDeepLink() to
    // return false, which fully disables deep link processing. No additional
    // redirect logic is needed.

    // ── RC5: Workspace OS — collect workspace state and navigation commands ──
    val workspaceState by mainViewModel.workspaceState.collectAsStateWithLifecycle(
        initialValue = com.appdex.data.workspace.WorkspaceObject()
    )
    val workspaceInsights by mainViewModel.workspaceInsights.collectAsStateWithLifecycle(
        initialValue = emptyList()
    )

    // ── RC5: Handle workspace navigation commands (Phase H) ──
    // WorkspaceController emits NavigateTo(tool) when OpenTool event fires.
    // We translate WorkspaceTool → Route and navigate.
    LaunchedEffect(mainViewModel) {
        mainViewModel.workspaceNavCommands.collect { command ->
            if (command != null) {
                when (command) {
                    is com.appdex.data.workspace.NavigationCommand.NavigateTo -> {
                        val route = when (command.tool) {
                            com.appdex.data.workspace.WorkspaceTool.AI -> Route.Ai
                            com.appdex.data.workspace.WorkspaceTool.WORKSPACE -> Route.Workspace
                            com.appdex.data.workspace.WorkspaceTool.SETTINGS -> Route.Settings
                            com.appdex.data.workspace.WorkspaceTool.MANIFEST -> Route.AxmlEditor(apkPath = currentSession?.apkFilePath)
                            com.appdex.data.workspace.WorkspaceTool.DEX -> Route.DexBrowser(apkPath = currentSession?.apkFilePath)
                            com.appdex.data.workspace.WorkspaceTool.EDITOR -> Route.Editor()
                            com.appdex.data.workspace.WorkspaceTool.HEX -> Route.HexEditor(filePath = currentSession?.apkFilePath ?: "")
                            com.appdex.data.workspace.WorkspaceTool.ELF -> Route.ElfViewer()
                            com.appdex.data.workspace.WorkspaceTool.SQLITE -> Route.SqliteViewer()
                            com.appdex.data.workspace.WorkspaceTool.RESOURCES -> Route.ArscViewer(apkPath = currentSession?.apkFilePath)
                            com.appdex.data.workspace.WorkspaceTool.SECURITY -> Route.ApkSecurity(apkPath = currentSession?.apkFilePath)
                            com.appdex.data.workspace.WorkspaceTool.SIGNING -> Route.ApkSigning(apkPath = currentSession?.apkFilePath)
                            com.appdex.data.workspace.WorkspaceTool.REPACK -> Route.ApkRepack(apkPath = currentSession?.apkFilePath)
                            com.appdex.data.workspace.WorkspaceTool.REPORT -> Route.Report
                            com.appdex.data.workspace.WorkspaceTool.FILES -> Route.Files()
                            com.appdex.data.workspace.WorkspaceTool.TERMINAL -> Route.Terminal
                            com.appdex.data.workspace.WorkspaceTool.TOOLS -> Route.Tools
                            com.appdex.data.workspace.WorkspaceTool.REMOTE -> Route.Remote
                            com.appdex.data.workspace.WorkspaceTool.DIFF -> Route.ApkDiff
                            com.appdex.data.workspace.WorkspaceTool.SIZE -> Route.ApkSizeAnalyzer(apkPath = currentSession?.apkFilePath)
                            com.appdex.data.workspace.WorkspaceTool.NONE -> null
                        }
                        if (route != null) {
                            navController.navigate(route) {
                                launchSingleTop = true
                            }
                        }
                    }
                    is com.appdex.data.workspace.NavigationCommand.NavigateBack -> {
                        val route = when (command.toTool) {
                            com.appdex.data.workspace.WorkspaceTool.WORKSPACE -> Route.Workspace
                            com.appdex.data.workspace.WorkspaceTool.AI -> Route.Ai
                            else -> null
                        }
                        if (route != null) {
                            navController.navigate(route) {
                                popUpTo(Route.Ai) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        } else {
                            navController.popBackStack()
                        }
                    }
                }
                mainViewModel.workspaceController.clearNavigationCommand()
            }
        }
    }

    CompositionLocalProvider(
        com.appdex.ui.components.LocalCurrentApkName provides currentSession?.displayName,
        com.appdex.ui.components.LocalWorkspaceController provides mainViewModel.workspaceController,
        com.appdex.ui.components.LocalWorkspaceEventBus provides mainViewModel.workspaceEventBus,
        com.appdex.ui.components.LocalWorkspaceReporter provides object : com.appdex.ui.components.WorkspaceReporter {
            override fun report(panel: String?, selection: String?, action: String?, timelineType: String?, timelineTitle: String?, timelineDetail: String?) {
                mainViewModel.sessionManager.reportWorkspaceAction(
                    panel = panel,
                    selection = selection,
                    action = action,
                    timelineType = timelineType,
                    timelineTitle = timelineTitle,
                    timelineDetail = timelineDetail
                )
            }
        }
    ) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppXTheme.colors.background)
            .statusBarsPadding()
    ) {
        // ── RC5: Workspace Breadcrumb Bar (Phase E) ──
        com.appdex.ui.components.WorkspaceBreadcrumbBar(
            controller = mainViewModel.workspaceController,
            onNavigateToTool = { tool ->
                mainViewModel.workspaceEventBus.emit(com.appdex.data.workspace.WorkspaceEvent.OpenTool(tool))
            }
        )
        Box(modifier = Modifier.weight(1f)) {
            Row(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.weight(1f)) {
                    NavHost(
                navController = navController,
                startDestination = Route.Ai,
                modifier = Modifier.fillMaxSize()
            ) {
                // ═══════════════════════════════════════════════════════
                // Tab 1: AI — Primary Entry Point
                // ═══════════════════════════════════════════════════════
                composable<Route.Ai> {
                    AiScreen(
                        messages = aiMessages,
                        isAiResponding = isAiResponding,
                        streamingContent = streamingContent,
                        aiConfig = aiConfig,
                        sessions = sessions,
                        currentSession = currentSession,
                        suggestedQuestions = suggestedQuestions,
                        onSendMessage = { text -> mainViewModel.sendAiMessage(text) },
                        onStopAi = { mainViewModel.stopAi() },
                        onRegenerate = { mainViewModel.regenerateLastMessage() },
                        onAnalyzeApk = { uri ->
                            android.util.Log.d("AppX", "onAnalyzeApk called with uri=$uri")
                            mainViewModel.openApk(uri)
                            // Delay navigation to avoid deep link conflict from ActivityResult callback
                            pendingNavigation.value = Route.Workspace
                        },
                        onOpenSession = { sessionId ->
                            mainViewModel.selectSession(sessionId)
                            navController.navigate(Route.Workspace) {
                                popUpTo(Route.Ai) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
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
                                "hex" -> navController.navigate(
                                    Route.HexEditor(filePath = session?.apkFilePath ?: "")
                                )
                                "diff" -> navController.navigate(Route.ApkDiff)
                                "remote" -> navController.navigate(Route.Remote)
                                "sqlite" -> navController.navigate(Route.SqliteViewer())
                                "elf" -> navController.navigate(Route.ElfViewer())
                                "editor" -> navController.navigate(Route.Editor())
                                "terminal" -> navController.navigate(Route.Terminal)
                                "files" -> navController.navigate(Route.Files())
                                "report" -> navController.navigate(Route.Report)
                                else -> {}
                            }
                        },
                        onNavigateToSettings = {
                            navController.navigate(Route.Settings) {
                                popUpTo(Route.Ai) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onNavigateToWorkspace = {
                            navController.navigate(Route.Workspace) {
                                popUpTo(Route.Ai) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onOpenCommandPalette = { showCommandPalette = true }
                    )
                }

                // ═══════════════════════════════════════════════════════
                // Tab 2: Workspace — APK Analysis Dashboard
                // ═══════════════════════════════════════════════════════
                composable<Route.Workspace> {
                    android.util.Log.d("AppX", ">>> Workspace composable RENDERED")
                    WorkspaceScreen(
                        sessions = sessions,
                        currentSession = currentSession,
                        isAnalyzing = isAnalyzing,
                        workflowProgress = workflowProgress,
                        workflowStepLabel = workflowStepLabel,
                        displayMode = displayMode,
                        onOpenApk = { uri -> mainViewModel.openApk(uri) },
                        onSelectSession = { sessionId -> mainViewModel.selectSession(sessionId) },
                        onDeleteSession = { sessionId -> mainViewModel.deleteSession(sessionId) },
                        onNavigate = { route ->
                            when (route) {
                                "终端" -> navController.navigate(Route.Terminal)
                                "编辑器" -> navController.navigate(Route.Editor())
                                "文件" -> navController.navigate(Route.Files())
                                "工具" -> navController.navigate(Route.Tools)
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
                        onOpenDetail = { navController.navigate(Route.ApkDetail) },
                        onOpenReport = { navController.navigate(Route.Report) },
                        onOpenFiles = { navController.navigate(Route.Files()) },
                        onOpenTools = { navController.navigate(Route.Tools) },
                        onOpenAxmlEditor = { apkPath ->
                            android.util.Log.d("AppX", "Workspace onOpenAxmlEditor called with apkPath=$apkPath")
                            navController.navigate(Route.AxmlEditor(apkPath = apkPath))
                        },
                        onOpenArscViewer = { apkPath ->
                            navController.navigate(Route.ArscViewer(apkPath = apkPath))
                        },
                        onNavigateToAi = {
                            navController.navigate(Route.Ai) {
                                popUpTo(Route.Workspace) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onAskAi = { query ->
                            mainViewModel.sendAiMessage(query)
                            navController.navigate(Route.Ai) {
                                popUpTo(Route.Workspace) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }

                // ═══════════════════════════════════════════════════════
                // Tab 3: Settings
                // ═══════════════════════════════════════════════════════
                composable<Route.Settings> {
                    SettingsScreen(
                        onNavigateToAiProviders = {
                            navController.navigate(Route.AiProviders)
                        }
                    )
                }

                // ═══════════════════════════════════════════════════════
                // AI Providers Management
                // ═══════════════════════════════════════════════════════
                composable<Route.AiProviders> {
                    AiProvidersScreen(onBack = { navController.popBackStack() })
                }

                // ═══════════════════════════════════════════════════════
                // Sub-routes
                // ═══════════════════════════════════════════════════════
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

                composable<Route.Tools> {
                    val currentApkPath = currentSession?.apkFilePath
                    ToolsScreen(
                        onBack = { navController.popBackStack() },
                        onNavigate = { route ->
                            when (route) {
                                "终端" -> navController.navigate(Route.Terminal)
                                "编辑器" -> navController.navigate(Route.Editor())
                                "远程管理" -> navController.navigate(Route.Remote)
                                "分析" -> navController.navigate(Route.Workspace)
                            }
                        },
                        onOpenDiff = { navController.navigate(Route.ApkDiff) },
                        onOpenSecurity = { navController.navigate(Route.ApkSecurity(apkPath = currentApkPath)) },
                        onOpenSizeAnalyzer = { navController.navigate(Route.ApkSizeAnalyzer(apkPath = currentApkPath)) },
                        onOpenDexBrowser = { navController.navigate(Route.DexBrowser(apkPath = currentApkPath)) },
                        onOpenSigning = { navController.navigate(Route.ApkSigning(apkPath = currentApkPath)) },
                        onOpenAxmlEditor = { navController.navigate(Route.AxmlEditor(apkPath = currentApkPath)) },
                        onOpenArscViewer = { navController.navigate(Route.ArscViewer(apkPath = currentApkPath)) },
                        onOpenSqliteViewer = { navController.navigate(Route.SqliteViewer()) },
                        onOpenElfViewer = { navController.navigate(Route.ElfViewer()) },
                        onOpenHexEditor = { navController.navigate(Route.HexEditor(filePath = currentApkPath ?: "")) },
                        displayMode = displayMode
                    )
                }

                composable<Route.DexBrowser> { backStackEntry ->
                    val route = backStackEntry.toRoute<Route.DexBrowser>()
                    // Auto-fallback to current session's APK path for global coherence
                    val effectivePath = route.apkPath ?: currentSession?.apkFilePath ?: ""
                    LaunchedEffect(Unit) {
                        mainViewModel.workspaceEventBus.emit(com.appdex.data.workspace.WorkspaceEvent.OpenTool(com.appdex.data.workspace.WorkspaceTool.DEX))
                        mainViewModel.sessionManager.updateNavigationContext("DEX Browser", "DexBrowser")
                        mainViewModel.sessionManager.reportWorkspaceAction(
                            panel = "DEX Browser",
                            action = "打开 DEX 浏览器",
                            timelineType = "NAVIGATE",
                            timelineTitle = "查看 DEX",
                            timelineDetail = effectivePath.substringAfterLast('/')
                        )
                    }
                    DexBrowserScreen(apkPath = effectivePath, searchQuery = route.searchQuery, onBack = { navController.popBackStack() })
                }

                composable<Route.HexEditor> { backStackEntry ->
                    val route = backStackEntry.toRoute<Route.HexEditor>()
                    // Auto-fallback to current session's APK for global coherence
                    val effectivePath = route.filePath.ifEmpty { currentSession?.apkFilePath ?: "" }
                    LaunchedEffect(Unit) {
                        mainViewModel.workspaceEventBus.emit(com.appdex.data.workspace.WorkspaceEvent.OpenTool(com.appdex.data.workspace.WorkspaceTool.HEX))
                        mainViewModel.sessionManager.updateNavigationContext("HEX Editor", "HexEditor")
                        mainViewModel.sessionManager.reportWorkspaceAction(
                            panel = "HEX Editor",
                            action = "打开 HEX 编辑器",
                            timelineType = "NAVIGATE",
                            timelineTitle = "查看 HEX",
                            timelineDetail = effectivePath.substringAfterLast('/')
                        )
                    }
                    HexEditorScreen(filePath = effectivePath, initialOffset = route.offset, onBack = { navController.popBackStack() })
                }

                composable<Route.ApkSigning> { backStackEntry ->
                    val route = backStackEntry.toRoute<Route.ApkSigning>()
                    val effectivePath = route.apkPath ?: currentSession?.apkFilePath
                    LaunchedEffect(Unit) {
                        mainViewModel.workspaceEventBus.emit(com.appdex.data.workspace.WorkspaceEvent.OpenTool(com.appdex.data.workspace.WorkspaceTool.SIGNING))
                        mainViewModel.sessionManager.updateNavigationContext("Signing", "ApkSigning")
                        mainViewModel.sessionManager.reportWorkspaceAction(
                            panel = "Signing",
                            action = "打开签名工具",
                            timelineType = "NAVIGATE",
                            timelineTitle = "查看签名"
                        )
                    }
                    SigningScreen(apkPath = effectivePath ?: "", onBack = { navController.popBackStack() })
                }

                composable<Route.ApkRepack> { backStackEntry ->
                    val route = backStackEntry.toRoute<Route.ApkRepack>()
                    val effectivePath = route.apkPath ?: currentSession?.apkFilePath
                    LaunchedEffect(Unit) {
                        mainViewModel.workspaceEventBus.emit(com.appdex.data.workspace.WorkspaceEvent.OpenTool(com.appdex.data.workspace.WorkspaceTool.REPACK))
                        mainViewModel.sessionManager.updateNavigationContext("Repack", "ApkRepack")
                        mainViewModel.sessionManager.reportWorkspaceAction(
                            panel = "Repack",
                            action = "打开重打包工具",
                            timelineType = "NAVIGATE",
                            timelineTitle = "查看重打包"
                        )
                    }
                    RepackagingScreen(apkPath = effectivePath ?: "", onBack = { navController.popBackStack() })
                }

                composable<Route.ApkDiff> {
                    ApkDiffScreen(onBack = { navController.popBackStack() })
                }

                composable<Route.ApkSecurity> { backStackEntry ->
                    val route = backStackEntry.toRoute<Route.ApkSecurity>()
                    val effectivePath = route.apkPath ?: currentSession?.apkFilePath
                    LaunchedEffect(Unit) {
                        mainViewModel.workspaceEventBus.emit(com.appdex.data.workspace.WorkspaceEvent.OpenTool(com.appdex.data.workspace.WorkspaceTool.SECURITY))
                        mainViewModel.sessionManager.updateNavigationContext("Security", "ApkSecurity")
                        mainViewModel.sessionManager.reportWorkspaceAction(
                            panel = "Security",
                            action = "打开安全扫描",
                            timelineType = "NAVIGATE",
                            timelineTitle = "查看安全扫描"
                        )
                    }
                    SecurityScannerScreen(apkPath = effectivePath, onBack = { navController.popBackStack() })
                }

                composable<Route.ApkSizeAnalyzer> { backStackEntry ->
                    val route = backStackEntry.toRoute<Route.ApkSizeAnalyzer>()
                    val effectivePath = route.apkPath ?: currentSession?.apkFilePath
                    SizeAnalyzerScreen(apkPath = effectivePath, onBack = { navController.popBackStack() })
                }

                composable<Route.AxmlEditor> { backStackEntry ->
                    val route = backStackEntry.toRoute<Route.AxmlEditor>()
                    val effectivePath = route.apkPath ?: currentSession?.apkFilePath
                    LaunchedEffect(Unit) {
                        mainViewModel.workspaceEventBus.emit(com.appdex.data.workspace.WorkspaceEvent.OpenTool(com.appdex.data.workspace.WorkspaceTool.MANIFEST))
                        mainViewModel.sessionManager.updateNavigationContext("Manifest", "AxmlEditor")
                        mainViewModel.sessionManager.reportWorkspaceAction(
                            panel = "Manifest",
                            action = "打开 Manifest 编辑器",
                            timelineType = "NAVIGATE",
                            timelineTitle = "查看 Manifest",
                            timelineDetail = route.entryName
                        )
                    }
                    AxmlEditorScreen(apkPath = effectivePath, entryName = route.entryName, onBack = { navController.popBackStack() })
                }

                composable<Route.ArscViewer> { backStackEntry ->
                    val route = backStackEntry.toRoute<Route.ArscViewer>()
                    val effectivePath = route.apkPath ?: currentSession?.apkFilePath
                    LaunchedEffect(Unit) {
                        mainViewModel.workspaceEventBus.emit(com.appdex.data.workspace.WorkspaceEvent.OpenTool(com.appdex.data.workspace.WorkspaceTool.RESOURCES))
                        mainViewModel.sessionManager.updateNavigationContext("Resources", "ArscViewer")
                        mainViewModel.sessionManager.reportWorkspaceAction(
                            panel = "Resources",
                            action = "打开资源查看器",
                            timelineType = "NAVIGATE",
                            timelineTitle = "查看资源",
                            timelineDetail = route.resourceId
                        )
                    }
                    ArscEditorScreen(apkPath = effectivePath, resourceId = route.resourceId, onBack = { navController.popBackStack() })
                }

                composable<Route.SqliteViewer> { backStackEntry ->
                    val route = backStackEntry.toRoute<Route.SqliteViewer>()
                    SqliteViewerScreen(dbPath = route.dbPath, onBack = { navController.popBackStack() })
                }

                composable<Route.ElfViewer> { backStackEntry ->
                    val route = backStackEntry.toRoute<Route.ElfViewer>()
                    ElfViewerScreen(filePath = route.filePath, onBack = { navController.popBackStack() })
                }

                composable<Route.Report> {
                    val session = mainViewModel.getCurrentSession()
                    LaunchedEffect(Unit) {
                        mainViewModel.workspaceEventBus.emit(com.appdex.data.workspace.WorkspaceEvent.OpenTool(com.appdex.data.workspace.WorkspaceTool.REPORT))
                        mainViewModel.sessionManager.updateNavigationContext("Report", "Report")
                        mainViewModel.sessionManager.reportWorkspaceAction(
                            panel = "Report",
                            action = "查看分析报告",
                            timelineType = "NAVIGATE",
                            timelineTitle = "查看报告"
                        )
                    }
ReportScreen(
    session = session,
    onBack = { navController.popBackStack() },
    onAskAi = { query ->
        mainViewModel.sendAiMessage(query)
        navController.navigate(Route.Ai) {
            popUpTo(Route.Ai) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    },
    onNavigateToSecurity = {
        navController.navigate(Route.ApkSecurity(apkPath = session?.apkFilePath))
    },
    onNavigateToDex = {
        navController.navigate(Route.DexBrowser(apkPath = session?.apkFilePath))
    },
    onNavigateToManifest = {
        navController.navigate(Route.AxmlEditor(apkPath = session?.apkFilePath))
    },
    onNavigateToSigning = {
        navController.navigate(Route.ApkSigning(apkPath = session?.apkFilePath))
    },
    onNavigateToReport = {
        navController.navigate(Route.Report)
    }
)
                }

                // ── Legacy route redirects ──
                composable<Route.Home> {
                    navController.navigate(Route.Ai) {
                        popUpTo(Route.Home) { inclusive = true }
                    }
                }
                composable<Route.Task> {
                    navController.navigate(Route.Workspace) {
                        popUpTo(Route.Task) { inclusive = true }
                    }
                }
            } // NavHost
                } // Box (NavHost content)

                // ── RC5: Workspace Inspector Sidebar (Phase D) ──
                // Show inspector when user is in a tool (not on main tabs)
                if (!showBottomBar && currentSession != null) {
                    com.appdex.ui.components.WorkspaceInspector(
                        controller = mainViewModel.workspaceController,
                        onNavigateToTool = { tool ->
                            mainViewModel.workspaceEventBus.emit(com.appdex.data.workspace.WorkspaceEvent.OpenTool(tool))
                        },
                        onClearInsight = { mainViewModel.workspaceController.clearInsights() }
                    )
                }
            } // Row
        } // Box

        if (showBottomBar) {
            AppXBottomNav(
                items = navItems,
                currentRoute = currentRouteName,
                onNavigate = { index ->
                    val route = when (index) {
                        0 -> Route.Ai
                        1 -> Route.Workspace
                        2 -> Route.Settings
                        else -> Route.Ai
                    }
                    navController.navigate(route) {
                        popUpTo(Route.Ai) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }

    // Command Palette overlay
    val currentApkPath = currentSession?.apkFilePath
    CommandPalette(
        visible = showCommandPalette,
        onDismiss = { showCommandPalette = false },
        session = currentSession,
        onNavigateAi = {
            navController.navigate(Route.Ai) {
                popUpTo(Route.Ai) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        },
        onNavigateWorkspace = {
            navController.navigate(Route.Workspace) {
                popUpTo(Route.Ai) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        },
        onNavigateSettings = {
            navController.navigate(Route.Settings) {
                popUpTo(Route.Ai) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        },
        onNavigateTerminal = { navController.navigate(Route.Terminal) },
        onNavigateEditor = { navController.navigate(Route.Editor()) },
        onNavigateFiles = {
            navController.navigate(Route.Files())
        },
        onNavigateTools = { navController.navigate(Route.Tools) },
        onAnalyzeApk = {
            navController.navigate(Route.Workspace) {
                popUpTo(Route.Ai) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        },
        onScanSecurity = { navController.navigate(Route.ApkSecurity(apkPath = currentApkPath)) },
        onOpenSigning = { navController.navigate(Route.ApkSigning(apkPath = currentApkPath)) },
        onOpenDex = { searchQuery -> navController.navigate(Route.DexBrowser(apkPath = currentApkPath, searchQuery = searchQuery)) },
        onOpenDiff = { navController.navigate(Route.ApkDiff) },
        onOpenSqlite = { navController.navigate(Route.SqliteViewer()) },
        onOpenHexEditor = { navController.navigate(Route.HexEditor(filePath = currentApkPath ?: "")) },
        onOpenRepack = { navController.navigate(Route.ApkRepack(apkPath = currentApkPath)) },
        onOpenSizeAnalyzer = { navController.navigate(Route.ApkSizeAnalyzer(apkPath = currentApkPath)) },
        onOpenAxmlEditor = { entryName -> navController.navigate(Route.AxmlEditor(apkPath = currentApkPath, entryName = entryName)) },
        onOpenArscViewer = { resourceId -> navController.navigate(Route.ArscViewer(apkPath = currentApkPath, resourceId = resourceId)) },
        onOpenElfViewer = { navController.navigate(Route.ElfViewer()) },
        onNavigateRemote = { navController.navigate(Route.Remote) },
        onAskAi = { query ->
            mainViewModel.sendAiMessage(query)
            navController.navigate(Route.Ai) {
                popUpTo(Route.Ai) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
    )

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
                navController.navigate(Route.Workspace) {
                    popUpTo(Route.Ai) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
                mediaRequest = null
            }
        }
    }
    } // CompositionLocalProvider
}
