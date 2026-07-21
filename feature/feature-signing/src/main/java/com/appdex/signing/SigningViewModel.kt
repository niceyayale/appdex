package com.appdex.signing

import android.util.Log

import androidx.lifecycle.viewModelScope
import com.appdex.arch.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class SigningViewModel @Inject constructor(
    private val signingRepository: SigningRepository,
    private val workspaceEventBus: com.appdex.data.workspace.WorkspaceEventBus,
) : BaseViewModel<SigningIntent, SigningState, SigningEffect>(
    initialState = SigningState()
) {

    override fun handleIntent(intent: SigningIntent) {
        when (intent) {
            is SigningIntent.SetInputApk -> {
                update {
                    it.copy(
                        inputApkPath = intent.path,
                        inputApkName = File(intent.path).name,
                        step = SigningStep.SELECT_KEYSTORE,
                    )
                }
            }
            is SigningIntent.LoadKeystore -> loadKeystore(intent.path, intent.password)
            is SigningIntent.ListKeystoreEntries -> listEntries(intent.path, intent.password)
            is SigningIntent.SelectEntry -> update {
                it.copy(selectedAlias = intent.alias, step = SigningStep.SIGN_OPTIONS)
            }
            is SigningIntent.SetKeyPassword -> update { it.copy(keyPassword = intent.password) }
            is SigningIntent.ToggleScheme -> update {
                it.copy(schemeConfig = SigningSchemeConfig(intent.v1, intent.v2, intent.v3))
            }
            is SigningIntent.Sign -> sign(intent.outputPath)
            is SigningIntent.CreateKeystore -> createKeystore(intent)
            is SigningIntent.ClearError -> update { it.copy(error = null) }
            is SigningIntent.Reset -> update { SigningState() }
        }
    }

    private fun loadKeystore(path: String, password: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val entries = signingRepository.listKeystoreEntries(path, password)
                update {
                    it.copy(
                        keystorePath = path,
                        keystorePassword = password,
                        keystoreEntries = entries,
                        step = SigningStep.ENTER_CREDENTIALS,
                        error = null,
                    )
                }
                if (entries.size == 1) {
                    update { it.copy(selectedAlias = entries[0].alias, step = SigningStep.SIGN_OPTIONS) }
                }
            } catch (e: Exception) {
                Log.w("AppX", "Suppressed exception", e)
                val msg = e.message ?: "加载 Keystore 失败"
                update { it.copy(error = msg) }
                emitEffect(SigningEffect.ShowError(msg))
            }
        }
    }

    private fun listEntries(path: String, password: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val entries = signingRepository.listKeystoreEntries(path, password)
                update { it.copy(keystoreEntries = entries, error = null) }
            } catch (e: Exception) {
                Log.w("AppX", "Suppressed exception", e)
                val msg = e.message ?: "列出条目失败"
                update { it.copy(error = msg) }
                emitEffect(SigningEffect.ShowError(msg))
            }
        }
    }

    private fun sign(outputPath: String) {
        update { it.copy(isSigning = true, step = SigningStep.SIGNING, error = null) }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val result = signingRepository.signApk(
                    inputApkPath = currentState.inputApkPath,
                    outputApkPath = outputPath,
                    keystorePath = currentState.keystorePath,
                    keystorePassword = currentState.keystorePassword,
                    keyAlias = currentState.selectedAlias,
                    keyPassword = currentState.keyPassword.ifEmpty { currentState.keystorePassword },
                    schemeConfig = currentState.schemeConfig,
                )
                update {
                    it.copy(
                        isSigning = false,
                        signingResult = result,
                        step = SigningStep.COMPLETE,
                    )
                }
                emitEffect(SigningEffect.SigningComplete(result))
                // Phase 4: Emit SignCompleted event to Workspace OS
                workspaceEventBus.emit(com.appdex.data.workspace.WorkspaceEvent.SignCompleted(outputPath))
            } catch (e: Exception) {
                Log.w("AppX", "Suppressed exception", e)
                val msg = e.message ?: "签名失败"
                update { it.copy(isSigning = false, error = msg, step = SigningStep.SIGN_OPTIONS) }
                emitEffect(SigningEffect.ShowError(msg))
            }
        }
    }

    private fun createKeystore(intent: SigningIntent.CreateKeystore) {
        update { it.copy(isCreatingKeystore = true, error = null) }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val entryInfo = signingRepository.createKeystore(
                    keystorePath = intent.path,
                    keystorePassword = intent.keystorePassword,
                    keyAlias = intent.alias,
                    keyPassword = intent.keyPassword,
                    subject = intent.subject,
                )
                update {
                    it.copy(
                        isCreatingKeystore = false,
                        keystorePath = intent.path,
                        keystorePassword = intent.keystorePassword,
                        keyPassword = intent.keyPassword,
                        keystoreEntries = listOf(entryInfo),
                        selectedAlias = intent.alias,
                        step = SigningStep.SIGN_OPTIONS,
                        error = null,
                    )
                }
                emitEffect(SigningEffect.ShowToast("Keystore 创建成功"))
            } catch (e: Exception) {
                Log.w("AppX", "Suppressed exception", e)
                val msg = e.message ?: "创建 Keystore 失败"
                update { it.copy(isCreatingKeystore = false, error = msg) }
                emitEffect(SigningEffect.ShowError(msg))
            }
        }
    }
}
