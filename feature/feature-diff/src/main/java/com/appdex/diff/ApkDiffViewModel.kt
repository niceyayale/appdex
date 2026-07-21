package com.appdex.diff

import android.util.Log

import androidx.lifecycle.viewModelScope
import com.appdex.arch.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ApkDiffViewModel @Inject constructor(
    private val diffRepository: ApkDiffRepository,
) : BaseViewModel<ApkDiffIntent, ApkDiffState, ApkDiffEffect>(
    initialState = ApkDiffState()
) {
    override fun handleIntent(intent: ApkDiffIntent) {
        when (intent) {
            is ApkDiffIntent.SetOldApk -> update { it.copy(oldApkPath = intent.path) }
            is ApkDiffIntent.SetNewApk -> update { it.copy(newApkPath = intent.path) }
            is ApkDiffIntent.RunDiff -> runDiff()
            is ApkDiffIntent.ClearError -> update { it.copy(error = null) }
            is ApkDiffIntent.Reset -> update { ApkDiffState() }
        }
    }

    private fun runDiff() {
        val state = currentState
        if (state.oldApkPath.isEmpty() || state.newApkPath.isEmpty()) return

        update { it.copy(isDiffing = true, error = null) }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val result = diffRepository.diff(state.oldApkPath, state.newApkPath)
                update { it.copy(isDiffing = false, diffResult = result) }
                emitEffect(ApkDiffEffect.DiffComplete(result))
            } catch (e: Exception) {
                Log.w("AppX", "Suppressed exception", e)
                val msg = e.message ?: "对比失败"
                update { it.copy(isDiffing = false, error = msg) }
                emitEffect(ApkDiffEffect.ShowError(msg))
            }
        }
    }
}
