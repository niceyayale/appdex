package com.appdex.arsceditor

import android.util.Log

import com.appdex.arch.BaseViewModel
import com.appdex.arch.MviEffect
import com.appdex.arch.MviIntent
import com.appdex.arch.MviState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

data class ArscEditorState(
    val packages: List<ArscPackageData> = emptyList(),
    val selectedPackageIndex: Int = 0,
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val fileName: String = "",
) : MviState {
    val filteredEntries: List<ArscEntryData>
        get() {
            val pkg = packages.getOrNull(selectedPackageIndex) ?: return emptyList()
            return if (searchQuery.isBlank()) {
                pkg.entries
            } else {
                pkg.entries.filter {
                    it.name.contains(searchQuery, ignoreCase = true) ||
                        it.type.contains(searchQuery, ignoreCase = true) ||
                        it.value.contains(searchQuery, ignoreCase = true) ||
                        "0x${it.resourceId.toString(16)}".contains(searchQuery, ignoreCase = true)
                }
            }
        }
}

sealed interface ArscEditorIntent : MviIntent {
    data class LoadFromApk(val apkPath: String) : ArscEditorIntent
    data class LoadFromBytes(val bytes: ByteArray, val fileName: String) : ArscEditorIntent
    data class SelectPackage(val index: Int) : ArscEditorIntent
    data class Search(val query: String) : ArscEditorIntent
}

sealed interface ArscEditorEffect : MviEffect {
    data class ShowMessage(val message: String) : ArscEditorEffect
}

@HiltViewModel
class ArscEditorViewModel @Inject constructor(
    private val repository: ArscEditorRepository,
) : BaseViewModel<ArscEditorIntent, ArscEditorState, ArscEditorEffect>(ArscEditorState()) {

    override fun handleIntent(intent: ArscEditorIntent) {
        when (intent) {
            is ArscEditorIntent.LoadFromApk -> loadFromApk(intent.apkPath)
            is ArscEditorIntent.LoadFromBytes -> loadFromBytes(intent.bytes, intent.fileName)
            is ArscEditorIntent.SelectPackage -> update { it.copy(selectedPackageIndex = intent.index) }
            is ArscEditorIntent.Search -> update { it.copy(searchQuery = intent.query) }
        }
    }

    private fun loadFromApk(apkPath: String) {
        update { it.copy(isLoading = true, error = null, fileName = apkPath.substringAfterLast("/")) }
        launchEffect {
            try {
                val result = repository.parseFromApk(apkPath)
                update { it.copy(packages = result.packages, isLoading = false) }
            } catch (e: Exception) {
                Log.w("AppDex", "Suppressed exception", e)
                update { it.copy(isLoading = false, error = e.message) }
                emitEffect(ArscEditorEffect.ShowMessage("解析失败: ${e.message}"))
            }
        }
    }

    private fun loadFromBytes(bytes: ByteArray, fileName: String) {
        update { it.copy(isLoading = true, error = null, fileName = fileName) }
        launchEffect {
            try {
                val result = repository.parse(bytes)
                update { it.copy(packages = result.packages, isLoading = false) }
            } catch (e: Exception) {
                Log.w("AppDex", "Suppressed exception", e)
                update { it.copy(isLoading = false, error = e.message) }
                emitEffect(ArscEditorEffect.ShowMessage("解析失败: ${e.message}"))
            }
        }
    }
}
