package com.appdex.settings

import androidx.lifecycle.ViewModel
import com.appdex.data.DensityMode
import com.appdex.data.SettingsRepository
import com.appdex.data.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settings: SettingsRepository
) : ViewModel() {

    val themeMode = settings.themeMode
    val densityMode = settings.densityMode
    val showHidden = settings.showHiddenFiles
    val rememberLastPath = settings.rememberLastPath
    val lastPath = settings.lastPath
    val editorFontSize = settings.editorFontSize
    val editorTabWidth = settings.editorTabWidth
    val defaultEncoding = settings.defaultEncoding

    fun setThemeMode(mode: ThemeMode) = kotlinx.coroutines.runBlocking {
        settings.setThemeMode(mode)
    }

    fun setDensityMode(mode: DensityMode) = kotlinx.coroutines.runBlocking {
        settings.setDensityMode(mode)
    }

    fun setShowHidden(show: Boolean) = kotlinx.coroutines.runBlocking {
        settings.setShowHiddenFiles(show)
    }

    fun setRememberPath(remember: Boolean) = kotlinx.coroutines.runBlocking {
        settings.setRememberPath(remember)
    }

    fun setFontSize(size: Int) = kotlinx.coroutines.runBlocking {
        settings.setEditorFontSize(size)
    }

    fun setTabWidth(width: Int) = kotlinx.coroutines.runBlocking {
        settings.setEditorTabWidth(width)
    }
}
