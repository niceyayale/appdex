package com.appdex.size

import android.util.Log

import androidx.lifecycle.viewModelScope
import com.appdex.arch.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

sealed interface SizeAnalyzerIntent : MviIntent {
    data class SetApkPath(val path: String) : SizeAnalyzerIntent
    data object Analyze : SizeAnalyzerIntent
    data object Reset : SizeAnalyzerIntent
}

data class SizeAnalyzerState(
    val apkPath: String = "",
    val apkName: String = "",
    val isAnalyzing: Boolean = false,
    val result: SizeAnalysisResult? = null,
    val error: String? = null,
) : MviState

sealed interface SizeAnalyzerEffect : MviEffect {
    data class ShowError(val message: String) : SizeAnalyzerEffect
    data class AnalyzeComplete(val result: SizeAnalysisResult) : SizeAnalyzerEffect
}

@HiltViewModel
class SizeAnalyzerViewModel @Inject constructor(
    private val repository: SizeAnalyzerRepository,
) : BaseViewModel<SizeAnalyzerIntent, SizeAnalyzerState, SizeAnalyzerEffect>(
    initialState = SizeAnalyzerState()
) {
    override fun handleIntent(intent: SizeAnalyzerIntent) {
        when (intent) {
            is SizeAnalyzerIntent.SetApkPath -> update {
                it.copy(apkPath = intent.path, apkName = File(intent.path).name)
            }
            is SizeAnalyzerIntent.Analyze -> analyze()
            is SizeAnalyzerIntent.Reset -> update { SizeAnalyzerState() }
        }
    }

    private fun analyze() {
        update { it.copy(isAnalyzing = true, error = null) }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val result = repository.analyze(currentState.apkPath)
                update { it.copy(isAnalyzing = false, result = result) }
                emitEffect(SizeAnalyzerEffect.AnalyzeComplete(result))
            } catch (e: Exception) {
                Log.w("AppDex", "Suppressed exception", e)
                update { it.copy(isAnalyzing = false, error = e.message ?: "分析失败") }
                emitEffect(SizeAnalyzerEffect.ShowError(e.message ?: "分析失败"))
            }
        }
    }
}
