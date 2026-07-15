package com.appdex.settings

import android.util.Log
import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appdex.data.DensityMode
import com.appdex.data.LanguageMode
import com.appdex.data.SettingsRepository
import com.appdex.data.ThemeMode
import com.appdex.data.ai.AiConfig
import com.appdex.data.ai.AiConfigRepository
import com.appdex.data.ai.AiProviderType
import com.appdex.data.session.ToolDisplayMode
import com.appdex.data.session.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settings: SettingsRepository,
    private val aiConfigRepo: AiConfigRepository,
    private val sessionManager: SessionManager,
    private val app: Application
) : ViewModel() {

    // ── Existing settings ──
    val themeMode = settings.themeMode
    val densityMode = settings.densityMode
    val languageMode = settings.languageMode
    val showHidden = settings.showHiddenFiles
    val rememberLastPath = settings.rememberLastPath
    val lastPath = settings.lastPath
    val editorFontSize = settings.editorFontSize
    val editorTabWidth = settings.editorTabWidth
    val defaultEncoding = settings.defaultEncoding
    val terminalFontSize = settings.terminalFontSize
    val terminalScrollback = settings.terminalScrollback

    // ── AI settings ──
    val aiConfig = aiConfigRepo.config
    val isAiEnabled = aiConfigRepo.isAiEnabled

    // ── Display mode ──
    val displayMode = sessionManager.displayMode

    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { settings.setThemeMode(mode) }
    fun setDensityMode(mode: DensityMode) = viewModelScope.launch { settings.setDensityMode(mode) }
    fun setLanguageMode(mode: LanguageMode) = viewModelScope.launch {
        settings.setLanguageMode(mode)
        applyLanguage(mode)
    }
    fun setShowHidden(show: Boolean) = viewModelScope.launch { settings.setShowHiddenFiles(show) }
    fun setRememberPath(remember: Boolean) = viewModelScope.launch { settings.setRememberPath(remember) }
    fun setFontSize(size: Int) = viewModelScope.launch { settings.setEditorFontSize(size) }
    fun setTabWidth(width: Int) = viewModelScope.launch { settings.setEditorTabWidth(width) }
    fun setTerminalFontSize(size: Int) = viewModelScope.launch { settings.setTerminalFontSize(size) }
    fun setTerminalScrollback(lines: Int) = viewModelScope.launch { settings.setTerminalScrollback(lines) }

    // ── AI methods ──
    fun setAiProvider(type: AiProviderType) = viewModelScope.launch {
        aiConfigRepo.setProvider(type)
    }
    fun setAiApiKey(key: String) = viewModelScope.launch {
        aiConfigRepo.setApiKey(key)
    }
    fun setAiBaseUrl(url: String) = viewModelScope.launch {
        aiConfigRepo.setBaseUrl(url)
    }
    fun setAiModel(model: String) = viewModelScope.launch {
        aiConfigRepo.setModel(model)
    }
    fun setAiTemperature(temp: Float) = viewModelScope.launch {
        aiConfigRepo.setTemperature(temp)
    }
    fun setAiMaxTokens(tokens: Int) = viewModelScope.launch {
        aiConfigRepo.setMaxTokens(tokens)
    }
    fun updateAiConfig(config: AiConfig) = viewModelScope.launch {
        aiConfigRepo.updateConfig(config)
    }

    // ── Display mode ──
    fun setDisplayMode(mode: ToolDisplayMode) {
        sessionManager.setDisplayMode(mode)
    }

    fun getCacheSize(): String {
        return try {
            val cacheDir = app.cacheDir
            val size = calculateDirSize(cacheDir)
            com.appdex.common.FormatUtil.formatFileSize(size)
        } catch (e: Exception) { Log.w("AppDex", "Suppressed exception", e); "Unknown" }
    }

    fun clearCache() {
        viewModelScope.launch {
            try {
                app.cacheDir.deleteRecursively()
                app.cacheDir.mkdirs()
            } catch (e: Exception) { Log.w("AppDex", "Suppressed exception", e) }
        }
    }

    fun getAppVersion(): String {
        return try {
            val pm = app.packageManager
            val info = pm.getPackageInfo(app.packageName, 0)
            "${info.versionName} (${info.longVersionCode})"
        } catch (e: Exception) { Log.w("AppDex", "Suppressed exception", e); "Unknown" }
    }

    private fun applyLanguage(mode: LanguageMode) {
        val locale = when (mode) {
            LanguageMode.ENGLISH -> Locale.ENGLISH
            LanguageMode.CHINESE -> Locale.SIMPLIFIED_CHINESE
            LanguageMode.SYSTEM -> Locale.getDefault()
        }
        val config = android.content.res.Configuration(app.resources.configuration)
        config.setLocale(locale)
        @Suppress("DEPRECATION")
        app.resources.updateConfiguration(config, app.resources.displayMetrics)
    }

    private fun calculateDirSize(dir: java.io.File): Long {
        if (!dir.exists()) return 0
        var size = 0L
        dir.walkTopDown().forEach { f -> if (f.isFile) size += f.length() }
        return size
    }
}
