package com.appdex.security

import android.util.Log

import androidx.lifecycle.viewModelScope
import com.appdex.arch.BaseViewModel
import com.appdex.arch.MviIntent
import com.appdex.arch.MviState
import com.appdex.arch.MviEffect
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface SecurityScannerIntent : MviIntent {
    data class SetApkPath(val path: String) : SecurityScannerIntent
    data object Scan : SecurityScannerIntent
    data object Reset : SecurityScannerIntent
}

data class SecurityScannerState(
    val apkPath: String = "",
    val apkName: String = "",
    val isScanning: Boolean = false,
    val scanResult: SecurityScanResult? = null,
    val error: String? = null,
) : MviState

sealed interface SecurityScannerEffect : MviEffect {
    data class ShowError(val message: String) : SecurityScannerEffect
    data class ScanComplete(val result: SecurityScanResult) : SecurityScannerEffect
}

@HiltViewModel
class SecurityScannerViewModel @Inject constructor(
    private val scannerRepository: SecurityScannerRepository,
) : BaseViewModel<SecurityScannerIntent, SecurityScannerState, SecurityScannerEffect>(
    initialState = SecurityScannerState()
) {
    override fun handleIntent(intent: SecurityScannerIntent) {
        when (intent) {
            is SecurityScannerIntent.SetApkPath -> update {
                it.copy(apkPath = intent.path, apkName = java.io.File(intent.path).name)
            }
            is SecurityScannerIntent.Scan -> scan()
            is SecurityScannerIntent.Reset -> update { SecurityScannerState() }
        }
    }

    private fun scan() {
        update { it.copy(isScanning = true, error = null) }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val result = scannerRepository.scan(currentState.apkPath)
                update { it.copy(isScanning = false, scanResult = result) }
                emitEffect(SecurityScannerEffect.ScanComplete(result))
            } catch (e: Exception) {
                Log.w("AppDex", "Suppressed exception", e)
                val msg = e.message ?: "扫描失败"
                update { it.copy(isScanning = false, error = msg) }
                emitEffect(SecurityScannerEffect.ShowError(msg))
            }
        }
    }
}
