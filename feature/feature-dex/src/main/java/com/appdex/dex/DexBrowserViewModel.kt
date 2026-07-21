package com.appdex.dex

import android.util.Log

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appdex.arch.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class DexBrowserViewModel @Inject constructor(
    private val dexRepository: DexRepository,
) : BaseViewModel<DexBrowserIntent, DexBrowserState, DexBrowserEffect>(
    initialState = DexBrowserState()
) {

    override fun handleIntent(intent: DexBrowserIntent) {
        when (intent) {
            is DexBrowserIntent.LoadDexFiles -> loadDexFiles(intent.apkPath)
            is DexBrowserIntent.SelectDex -> selectDex(intent.dexName)
            is DexBrowserIntent.SearchClasses -> searchClasses(intent.query)
            is DexBrowserIntent.SelectClass -> selectClass(intent.classType)
            is DexBrowserIntent.BackToDexList -> backToDexList()
            is DexBrowserIntent.BackToClassList -> backToClassList()
            is DexBrowserIntent.ClearError -> update { it.copy(error = null) }
        }
    }

    private fun loadDexFiles(apkPath: String) {
        update { it.copy(apkPath = apkPath, isLoading = true, error = null, viewLevel = DexViewLevel.DEX_LIST) }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val dexFiles = dexRepository.listDexFiles(apkPath)
                if (dexFiles.isEmpty()) {
                    update { it.copy(isLoading = false, error = "APK 中未找到 DEX 文件") }
                    emitEffect(DexBrowserEffect.ShowError("APK 中未找到 DEX 文件"))
                } else {
                    update {
                        it.copy(
                            dexFiles = dexFiles,
                            isLoading = false,
                        )
                    }
                    // 如果只有一个 DEX 文件，自动选中
                    if (dexFiles.size == 1) {
                        selectDex(dexFiles[0].name)
                    }
                }
            } catch (e: Exception) {
                Log.w("AppX", "Suppressed exception", e)
                val msg = e.message ?: "加载 DEX 文件失败"
                update { it.copy(isLoading = false, error = msg) }
                emitEffect(DexBrowserEffect.ShowError(msg))
            }
        }
    }

    private fun selectDex(dexName: String) {
        update {
            it.copy(
                selectedDexName = dexName,
                isLoading = true,
                error = null,
                viewLevel = DexViewLevel.CLASS_LIST,
                searchQuery = "",
                allClasses = emptyList(),
                filteredClasses = emptyList(),
            )
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val classes = dexRepository.listClasses(currentState.apkPath, dexName)
                update {
                    it.copy(
                        allClasses = classes,
                        filteredClasses = classes,
                        isLoading = false,
                    )
                }
            } catch (e: Exception) {
                Log.w("AppX", "Suppressed exception", e)
                val msg = e.message ?: "加载类列表失败"
                update { it.copy(isLoading = false, error = msg) }
                emitEffect(DexBrowserEffect.ShowError(msg))
            }
        }
    }

    private fun searchClasses(query: String) {
        update { it.copy(searchQuery = query) }
        if (query.isBlank()) {
            update { it.copy(filteredClasses = it.allClasses) }
        } else {
            val filtered = currentState.allClasses.filter { cls ->
                cls.type.contains(query, ignoreCase = true) ||
                cls.simpleName.contains(query, ignoreCase = true) ||
                cls.packageName.contains(query, ignoreCase = true)
            }
            update { it.copy(filteredClasses = filtered) }
        }
    }

    private fun selectClass(classType: String) {
        update {
            it.copy(
                selectedClassType = classType,
                isLoadingSmali = true,
                error = null,
                viewLevel = DexViewLevel.SMALI_VIEWER,
                smaliCode = "",
            )
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val smali = dexRepository.toSmali(
                    currentState.apkPath,
                    currentState.selectedDexName,
                    classType,
                )
                update { it.copy(smaliCode = smali, isLoadingSmali = false) }
            } catch (e: Exception) {
                Log.w("AppX", "Suppressed exception", e)
                val msg = e.message ?: "反汇编失败"
                update { it.copy(isLoadingSmali = false, error = msg) }
                emitEffect(DexBrowserEffect.ShowError(msg))
            }
        }
    }

    private fun backToDexList() {
        update {
            it.copy(
                viewLevel = DexViewLevel.DEX_LIST,
                selectedDexName = "",
                allClasses = emptyList(),
                filteredClasses = emptyList(),
                searchQuery = "",
                smaliCode = "",
                selectedClassType = "",
            )
        }
    }

    private fun backToClassList() {
        update {
            it.copy(
                viewLevel = DexViewLevel.CLASS_LIST,
                smaliCode = "",
                selectedClassType = "",
                isLoadingSmali = false,
            )
        }
    }
}
