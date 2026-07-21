package com.appdex

import android.util.Log

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.appdex.data.SettingsRepository
import com.appdex.data.ThemeMode
import com.appdex.nav.AppXApp
import com.appdex.ui.theme.AmberGold
import com.appdex.ui.theme.AmberGoldContainer
import com.appdex.ui.theme.AmberGoldDark
import com.appdex.ui.theme.AppXTheme
import com.appdex.ui.theme.AppThemeMode
import com.appdex.ui.theme.DeepSpaceOuter
import com.appdex.ui.theme.TextPrimary
import com.appdex.ui.theme.TextSecondary
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AppXActivity : ComponentActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    private var permissionGranted by mutableStateOf(false)

    companion object {
        private const val TAG = "AppX"
    }

    /** URI received from external VIEW intent (e.g. file manager opening an APK) */
    internal val pendingApkUri = mutableStateOf<Uri?>(null)

    private val manageStorageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        permissionGranted = checkStoragePermission()
    }

    override fun attachBaseContext(newBase: android.content.Context) {
        // Apply saved language preference before Activity creates its resources
        // Must read from "language_prefs" — the same file SettingsRepository writes to
        val prefs = newBase.getSharedPreferences("language_prefs", android.content.Context.MODE_PRIVATE)
        val langCode = prefs.getString("language_mode", "system") ?: "system"
        val locale = when (langCode) {
            "english" -> java.util.Locale.ENGLISH
            "chinese" -> java.util.Locale.SIMPLIFIED_CHINESE
            else -> java.util.Locale.getDefault()
        }
        val config = android.content.res.Configuration(newBase.resources.configuration)
        config.setLocale(locale)
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        permissionGranted = checkStoragePermission()
        // Handle APK opened from external source (file manager, browser, etc.)
        handleApkIntent(intent)
        setContent {
            val themeMode by settingsRepository.themeMode
                .collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)

            val appThemeMode = when (themeMode) {
                ThemeMode.SYSTEM -> AppThemeMode.SYSTEM
                ThemeMode.LIGHT -> AppThemeMode.LIGHT
                ThemeMode.DARK -> AppThemeMode.DARK
            }

            AppXTheme(themeMode = appThemeMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (permissionGranted) {
                        AppXApp()
                    } else {
                        PermissionScreen(
                            onRequestPermission = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                    try {
                                        val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                                            data = Uri.parse("package:$packageName")
                                        }
                                        manageStorageLauncher.launch(intent)
                                    } catch (e: Exception) {
                                        Log.w("AppX", "Suppressed exception", e)
                                        val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                                        manageStorageLauncher.launch(intent)
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        // Unconditionally replace the activity intent with a clean ACTION_MAIN intent
        // BEFORE super.onResume(). This prevents Compose Navigation 2.8.x from
        // re-processing deep links on every ON_RESUME, which causes a navigation
        // loop through all @Serializable routes (HexEditor → DexBrowser → ElfViewer
        // → AxmlEditor). The NavController's handleDeepLink checks the activity
        // intent during ON_RESUME; a clean ACTION_MAIN with no data/flags ensures
        // no deep link pattern matches.
        intent = Intent(Intent.ACTION_MAIN).apply {
            setPackage(packageName)
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        super.onResume()
        permissionGranted = checkStoragePermission()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleApkIntent(intent)
    }

    private fun handleApkIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW) {
            val uri = intent.data
            if (uri != null) {
                Log.d(TAG, "Received APK URI from external: $uri")
                pendingApkUri.value = uri
            }
        }
    }

    private fun checkStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            true
        }
    }
}

@androidx.compose.runtime.Composable
private fun PermissionScreen(
    onRequestPermission: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepSpaceOuter)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier.size(56.dp).background(AmberGoldContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Folder,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = AmberGold
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "AppX",
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                letterSpacing = 2.sp,
                color = AmberGold
            )
            Text(
                text = "需要存储权限",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                modifier = Modifier.padding(top = 4.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "AppX 需要存储访问权限来管理文件。\n请在下一个页面中授予「所有文件访问」权限。",
                fontSize = 12.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(32.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .background(AmberGold)
                    .clickable(onClick = onRequestPermission),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "授予权限",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = AmberGoldDark
                )
            }
        }
    }
}
