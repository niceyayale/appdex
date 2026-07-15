package com.appdex.terminal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appdex.data.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class TerminalViewModel @Inject constructor(
    settings: SettingsRepository
) : ViewModel() {
    val fontSize = settings.terminalFontSize
        .stateIn(viewModelScope, SharingStarted.Eagerly, 13)
    val scrollback = settings.terminalScrollback
        .stateIn(viewModelScope, SharingStarted.Eagerly, 1000)
}
