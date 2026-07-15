package com.appdex.elfviewer

import android.util.Log

import com.appdex.arch.BaseViewModel
import com.appdex.arch.MviEffect
import com.appdex.arch.MviIntent
import com.appdex.arch.MviState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

data class ElfViewerState(
    val elfData: ElfData? = null,
    val selectedTab: ElfTab = ElfTab.HEADER,
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val fileName: String = "",
) : MviState {
    val filteredSymbols: List<ElfSymbol>
        get() {
            val syms = elfData?.symbols ?: return emptyList()
            return if (searchQuery.isBlank()) syms
            else syms.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                    it.type.contains(searchQuery, ignoreCase = true) ||
                    it.bind.contains(searchQuery, ignoreCase = true)
            }
        }
}

enum class ElfTab(val title: String) {
    HEADER("文件头"),
    SECTIONS("节区"),
    SEGMENTS("段"),
    SYMBOLS("符号表"),
    DYNAMIC("动态链接"),
}

sealed interface ElfViewerIntent : MviIntent {
    data class LoadFile(val filePath: String) : ElfViewerIntent
    data class SelectTab(val tab: ElfTab) : ElfViewerIntent
    data class Search(val query: String) : ElfViewerIntent
}

sealed interface ElfViewerEffect : MviEffect {
    data class ShowMessage(val message: String) : ElfViewerEffect
}

@HiltViewModel
class ElfViewerViewModel @Inject constructor(
    private val repository: ElfViewerRepository,
) : BaseViewModel<ElfViewerIntent, ElfViewerState, ElfViewerEffect>(ElfViewerState()) {

    override fun handleIntent(intent: ElfViewerIntent) {
        when (intent) {
            is ElfViewerIntent.LoadFile -> loadFile(intent.filePath)
            is ElfViewerIntent.SelectTab -> update { it.copy(selectedTab = intent.tab) }
            is ElfViewerIntent.Search -> update { it.copy(searchQuery = intent.query) }
        }
    }

    private fun loadFile(filePath: String) {
        update { it.copy(isLoading = true, error = null, fileName = filePath.substringAfterLast("/")) }
        launchEffect {
            try {
                val elfData = repository.parse(filePath)
                update { it.copy(elfData = elfData, isLoading = false) }
            } catch (e: Exception) {
                Log.w("AppDex", "Suppressed exception", e)
                update { it.copy(isLoading = false, error = e.message) }
                emitEffect(ElfViewerEffect.ShowMessage("解析失败: ${e.message}"))
            }
        }
    }
}
