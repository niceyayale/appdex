package com.appdex.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "appdex_settings")

enum class ThemeMode { SYSTEM, LIGHT, DARK }
enum class DensityMode { COMPACT, STANDARD, COMFORTABLE }
enum class LanguageMode { ENGLISH, CHINESE, SYSTEM }

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore get() = context.dataStore

    // ── Theme ──
    val themeMode: Flow<ThemeMode> = dataStore.data
        .map { ThemeMode.entries.getOrNull(it[KEY_THEME] ?: 0) ?: ThemeMode.SYSTEM }

    val densityMode: Flow<DensityMode> = dataStore.data
        .map { DensityMode.entries.getOrNull(it[KEY_DENSITY] ?: 1) ?: DensityMode.STANDARD }

    // ── File Manager ──
    val showHiddenFiles: Flow<Boolean> = dataStore.data
        .map { it[KEY_SHOW_HIDDEN] ?: false }

    val rememberLastPath: Flow<Boolean> = dataStore.data
        .map { it[KEY_REMEMBER_PATH] ?: true }

    val lastPath: Flow<String> = dataStore.data
        .map { it[KEY_LAST_PATH] ?: "/storage/emulated/0" }

    // ── Editor ──
    val editorFontSize: Flow<Int> = dataStore.data
        .map { it[KEY_FONT_SIZE] ?: 14 }

    val editorTabWidth: Flow<Int> = dataStore.data
        .map { it[KEY_TAB_WIDTH] ?: 4 }

    val defaultEncoding: Flow<String> = dataStore.data
        .map { it[KEY_ENCODING] ?: "UTF-8" }

    // ── Language ──
    val languageMode: Flow<LanguageMode> = dataStore.data
        .map { LanguageMode.entries.getOrNull(it[KEY_LANGUAGE] ?: 2) ?: LanguageMode.SYSTEM }

    // ── Terminal ──
    val terminalFontSize: Flow<Int> = dataStore.data
        .map { it[KEY_TERM_FONT_SIZE] ?: 13 }

    val terminalScrollback: Flow<Int> = dataStore.data
        .map { it[KEY_TERM_SCROLLBACK] ?: 1000 }

    // ── Writers ──
    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[KEY_THEME] = mode.ordinal }
    }

    suspend fun setDensityMode(mode: DensityMode) {
        dataStore.edit { it[KEY_DENSITY] = mode.ordinal }
    }

    suspend fun setShowHiddenFiles(show: Boolean) {
        dataStore.edit { it[KEY_SHOW_HIDDEN] = show }
    }

    suspend fun setLastPath(path: String) {
        dataStore.edit { it[KEY_LAST_PATH] = path }
    }

    suspend fun setRememberPath(remember: Boolean) {
        dataStore.edit { it[KEY_REMEMBER_PATH] = remember }
    }

    suspend fun setEditorFontSize(size: Int) {
        dataStore.edit { it[KEY_FONT_SIZE] = size }
    }

    suspend fun setEditorTabWidth(width: Int) {
        dataStore.edit { it[KEY_TAB_WIDTH] = width }
    }

    suspend fun setDefaultEncoding(encoding: String) {
        dataStore.edit { it[KEY_ENCODING] = encoding }
    }

    suspend fun setLanguageMode(mode: LanguageMode) {
        dataStore.edit { it[KEY_LANGUAGE] = mode.ordinal }
    }

    suspend fun setTerminalFontSize(size: Int) {
        dataStore.edit { it[KEY_TERM_FONT_SIZE] = size }
    }

    suspend fun setTerminalScrollback(lines: Int) {
        dataStore.edit { it[KEY_TERM_SCROLLBACK] = lines }
    }

    companion object {
        private val KEY_THEME = intPreferencesKey("theme_mode")
        private val KEY_DENSITY = intPreferencesKey("density_mode")
        private val KEY_SHOW_HIDDEN = booleanPreferencesKey("show_hidden")
        private val KEY_REMEMBER_PATH = booleanPreferencesKey("remember_path")
        private val KEY_LAST_PATH = stringPreferencesKey("last_path")
        private val KEY_FONT_SIZE = intPreferencesKey("font_size")
        private val KEY_TAB_WIDTH = intPreferencesKey("tab_width")
        private val KEY_ENCODING = stringPreferencesKey("encoding")
        private val KEY_LANGUAGE = intPreferencesKey("language_mode")
        private val KEY_TERM_FONT_SIZE = intPreferencesKey("term_font_size")
        private val KEY_TERM_SCROLLBACK = intPreferencesKey("term_scrollback")
    }
}
