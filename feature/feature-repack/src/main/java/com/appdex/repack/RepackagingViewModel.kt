package com.appdex.repack

import android.util.Log

import androidx.lifecycle.viewModelScope
import com.appdex.arch.BaseViewModel
import com.appdex.signing.SigningSchemeConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class RepackagingViewModel @Inject constructor(
    private val repackagingRepository: RepackagingRepository,
) : BaseViewModel<RepackagingIntent, RepackagingState, RepackagingEffect>(
    initialState = RepackagingState()
) {

    override fun handleIntent(intent: RepackagingIntent) {
        when (intent) {
            is RepackagingIntent.SetInputApk -> {
                update {
                    it.copy(
                        inputApkPath = intent.path,
                        inputApkName = File(intent.path).name,
                        step = RepackStep.SELECT_DEX,
                    )
                }
                handleIntent(RepackagingIntent.LoadDexFiles)
            }
            is RepackagingIntent.LoadDexFiles -> loadDexFiles()
            is RepackagingIntent.SelectDexFile -> toggleDexSelection(intent.dexName)
            is RepackagingIntent.SetSmaliContent -> update {
                val newReplacements = it.smaliReplacements.toMutableMap()
                newReplacements[intent.dexName] = intent.smaliContents
                it.copy(smaliReplacements = newReplacements)
            }
            is RepackagingIntent.SetKeystoreInfo -> update {
                it.copy(
                    keystorePath = intent.keystorePath,
                    keystorePassword = intent.keystorePassword,
                    keyAlias = intent.keyAlias,
                    keyPassword = intent.keyPassword,
                    step = if (it.selectedDexFiles.isNotEmpty()) RepackStep.ENTER_KEYSTORE else it.step,
                )
            }
            is RepackagingIntent.RepackAndSign -> repackAndSign(intent.outputPath)
            is RepackagingIntent.RepackOnly -> repackOnly(intent.outputPath)
            is RepackagingIntent.ClearError -> update { it.copy(error = null) }
            is RepackagingIntent.Reset -> update { RepackagingState() }
        }
    }

    private fun loadDexFiles() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val dexFiles = repackagingRepository.listDexFiles(currentState.inputApkPath)
                update { it.copy(dexFiles = dexFiles, error = null) }
            } catch (e: Exception) {
                Log.w("AppDex", "Suppressed exception", e)
                val msg = e.message ?: "加载 DEX 文件列表失败"
                update { it.copy(error = msg) }
                emitEffect(RepackagingEffect.ShowError(msg))
            }
        }
    }

    private fun toggleDexSelection(dexName: String) {
        update {
            val newSelection = it.selectedDexFiles.toMutableSet()
            if (newSelection.contains(dexName)) {
                newSelection.remove(dexName)
            } else {
                newSelection.add(dexName)
            }
            it.copy(selectedDexFiles = newSelection)
        }
    }

    private fun repackAndSign(outputPath: String) {
        update { it.copy(isProcessing = true, step = RepackStep.REPACKING, error = null) }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 为每个选中的 DEX 文件准备空的 Smali 替换
                // (实际使用时，UI 会传递编辑后的 Smali 内容)
                val smaliReplacements = currentState.smaliReplacements
                val result = repackagingRepository.repackAndSign(
                    inputApkPath = currentState.inputApkPath,
                    outputApkPath = outputPath,
                    smaliReplacements = smaliReplacements,
                    keystorePath = currentState.keystorePath,
                    keystorePassword = currentState.keystorePassword,
                    keyAlias = currentState.keyAlias,
                    keyPassword = currentState.keyPassword,
                    schemeConfig = SigningSchemeConfig(),
                )
                update {
                    it.copy(
                        isProcessing = false,
                        repackResult = result,
                        step = RepackStep.COMPLETE,
                    )
                }
                emitEffect(RepackagingEffect.RepackComplete(result))
            } catch (e: Exception) {
                Log.w("AppDex", "Suppressed exception", e)
                val msg = e.message ?: "回编译失败"
                update { it.copy(isProcessing = false, error = msg, step = RepackStep.ENTER_KEYSTORE) }
                emitEffect(RepackagingEffect.ShowError(msg))
            }
        }
    }

    private fun repackOnly(outputPath: String) {
        update { it.copy(isProcessing = true, step = RepackStep.REPACKING, error = null) }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 将 Smali 内容编译为 DEX
                val dexReplacements = mutableMapOf<String, ByteArray>()
                for ((dexName, smaliContents) in currentState.smaliReplacements) {
                    val result = repackagingRepository.compileSmaliToDex(smaliContents)
                    if (result.isFailure) {
                        val msg = result.exceptionOrNull()?.message ?: "DEX 编译失败"
                        update { it.copy(isProcessing = false, error = msg, step = RepackStep.ENTER_KEYSTORE) }
                        emitEffect(RepackagingEffect.ShowError(msg))
                        return@launch
                    }
                    dexReplacements[dexName] = result.getOrThrow()
                }
                val result = repackagingRepository.repackApk(
                    inputApkPath = currentState.inputApkPath,
                    outputApkPath = outputPath,
                    dexReplacements = dexReplacements,
                )
                update {
                    it.copy(
                        isProcessing = false,
                        repackResult = result,
                        step = RepackStep.COMPLETE,
                    )
                }
                emitEffect(RepackagingEffect.RepackComplete(result))
            } catch (e: Exception) {
                Log.w("AppDex", "Suppressed exception", e)
                val msg = e.message ?: "回编译失败"
                update { it.copy(isProcessing = false, error = msg, step = RepackStep.ENTER_KEYSTORE) }
                emitEffect(RepackagingEffect.ShowError(msg))
            }
        }
    }
}
