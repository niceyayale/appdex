package com.appdex.settings

import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModel
import com.appdex.data.DensityMode
import com.appdex.data.LanguageMode
import com.appdex.data.SettingsRepository
import com.appdex.data.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.runBlocking
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settings: SettingsRepository,
    private val app: Application
) : ViewModel() {

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

    fun setThemeMode(mode: ThemeMode) = runBlocking { settings.setThemeMode(mode) }
    fun setDensityMode(mode: DensityMode) = runBlocking { settings.setDensityMode(mode) }
    fun setLanguageMode(mode: LanguageMode) = runBlocking {
        settings.setLanguageMode(mode)
        applyLanguage(mode)
    }
    fun setShowHidden(show: Boolean) = runBlocking { settings.setShowHiddenFiles(show) }
    fun setRememberPath(remember: Boolean) = runBlocking { settings.setRememberPath(remember) }
    fun setFontSize(size: Int) = runBlocking { settings.setEditorFontSize(size) }
    fun setTabWidth(width: Int) = runBlocking { settings.setEditorTabWidth(width) }
    fun setTerminalFontSize(size: Int) = runBlocking { settings.setTerminalFontSize(size) }
    fun setTerminalScrollback(lines: Int) = runBlocking { settings.setTerminalScrollback(lines) }

    fun getCacheSize(): String {
        return try {
            val cacheDir = app.cacheDir
            val size = calculateDirSize(cacheDir)
            formatSize(size)
        } catch (_: Exception) {
            "Unknown"
        }
    }

    fun clearCache() {
        try {
            app.cacheDir.deleteRecursively()
            app.cacheDir.mkdirs()
        } catch (_: Exception) {
        }
    }

    fun getAppVersion(): String {
        return try {
            val pm = app.packageManager
            val info = pm.getPackageInfo(app.packageName, 0)
            "${info.versionName} (${info.longVersionCode})"
        } catch (_: Exception) {
            "Unknown"
        }
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

    private fun formatSize(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        if (bytes < 1024 * 1024) return "%.1f KB".format(bytes / 1024.0)
        if (bytes < 1024 * 1024 * 1024) return "%.1f MB".format(bytes / (1024.0 * 1024))
        return "%.1f GB".format(bytes / (1024.0 * 1024 * 1024))
    }
}
