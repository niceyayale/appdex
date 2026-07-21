package com.appdex.settings

import android.app.Application
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appdex.data.ai.AiConfig
import com.appdex.data.ai.AiConfigRepository
import com.appdex.data.ai.AiProviderEntity
import com.appdex.data.ai.AiProviderType
import com.appdex.data.ai.AiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AiProvidersViewModel @Inject constructor(
    private val aiConfigRepo: AiConfigRepository,
    private val aiService: AiService,
    private val app: Application
) : ViewModel() {

    val savedProviders = aiConfigRepo.savedProviders
    val activeProviderId = aiConfigRepo.activeProviderId

    // ── UI State ──
    sealed class UiState {
        object List : UiState()
        data class Edit(val provider: AiProviderEntity?) : UiState() // null = creating new
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.List)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    // ── Edit form state ──
    data class EditForm(
        val id: String = "",
        val name: String = "",
        val providerType: AiProviderType = AiProviderType.OPENAI,
        val apiKey: String = "",
        val baseUrl: String = "",
        val modelName: String = "",
        val isExisting: Boolean = false
    )

    private val _editForm = MutableStateFlow(EditForm())
    val editForm: StateFlow<EditForm> = _editForm.asStateFlow()

    // ── Connection Test ──
    sealed class TestResult {
        object Idle : TestResult()
        object Testing : TestResult()
        data class Success(val message: String) : TestResult()
        data class Failure(val message: String) : TestResult()
    }

    private val _testResult = MutableStateFlow<TestResult>(TestResult.Idle)
    val testResult: StateFlow<TestResult> = _testResult.asStateFlow()

    // ── Toast messages ──
    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    fun showToast(message: String) {
        _toastMessage.value = message
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    init {
        // Migrate legacy config on first load
        viewModelScope.launch {
            aiConfigRepo.migrateFromLegacyIfNeeded()
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // Navigation
    // ═══════════════════════════════════════════════════════════════

    fun showCreateForm() {
        _editForm.value = EditForm(isExisting = false)
        _testResult.value = TestResult.Idle
        _uiState.value = UiState.Edit(null)
    }

    fun showEditForm(provider: AiProviderEntity) {
        _editForm.value = EditForm(
            id = provider.id,
            name = provider.name,
            providerType = provider.providerType,
            apiKey = provider.apiKey,
            baseUrl = provider.baseUrl,
            modelName = provider.modelName,
            isExisting = true
        )
        _testResult.value = TestResult.Idle
        _uiState.value = UiState.Edit(provider)
    }

    fun backToList() {
        _uiState.value = UiState.List
        _testResult.value = TestResult.Idle
    }

    // ═══════════════════════════════════════════════════════════════
    // Form editing
    // ═══════════════════════════════════════════════════════════════

    fun updateName(name: String) {
        _editForm.value = _editForm.value.copy(name = name)
    }

    fun updateProviderType(type: AiProviderType) {
        _editForm.value = _editForm.value.copy(providerType = type)
    }

    fun updateApiKey(key: String) {
        _editForm.value = _editForm.value.copy(apiKey = key)
    }

    fun updateBaseUrl(url: String) {
        _editForm.value = _editForm.value.copy(baseUrl = url)
    }

    fun updateModel(model: String) {
        _editForm.value = _editForm.value.copy(modelName = model)
    }

    // ═══════════════════════════════════════════════════════════════
    // Save / Delete
    // ═══════════════════════════════════════════════════════════════

    fun saveProvider() {
        val form = _editForm.value
        if (form.name.isBlank()) {
            _toastMessage.value = "请填写提供商名称"
            return
        }

        viewModelScope.launch {
            val provider = AiProviderEntity(
                id = if (form.isExisting) form.id else AiProviderEntity.generateId(),
                name = form.name.trim(),
                providerType = form.providerType,
                apiKey = form.apiKey.trim(),
                baseUrl = form.baseUrl.trim(),
                modelName = form.modelName.trim()
            )

            if (form.isExisting) {
                aiConfigRepo.updateProvider(provider)
                _toastMessage.value = "提供商已更新"
            } else {
                aiConfigRepo.addProvider(provider)
                _toastMessage.value = "提供商已添加"
            }
            backToList()
        }
    }

    fun deleteProvider(providerId: String) {
        viewModelScope.launch {
            aiConfigRepo.deleteProvider(providerId)
            _toastMessage.value = "提供商已删除"
            if (_uiState.value is UiState.Edit) {
                backToList()
            }
        }
    }

    fun setActiveProvider(providerId: String) {
        viewModelScope.launch {
            aiConfigRepo.setActiveProvider(providerId)
            _toastMessage.value = "已切换为当前 AI 提供商"
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // Connection Test
    // ═══════════════════════════════════════════════════════════════

    fun testConnection() {
        val form = _editForm.value
        val config = AiConfig(
            providerType = form.providerType,
            apiKey = form.apiKey,
            baseUrl = form.baseUrl,
            modelName = form.modelName
        )

        if (!config.isConfigured()) {
            _testResult.value = TestResult.Failure("请先填写必要的配置字段")
            return
        }

        viewModelScope.launch {
            _testResult.value = TestResult.Testing
            try {
                val response = aiService.testConnection(config)
                if (response.success) {
                    _testResult.value = TestResult.Success("连接成功")
                } else {
                    _testResult.value = TestResult.Failure(response.error ?: "连接失败")
                }
            } catch (e: Exception) {
                Log.w("AppX", "AI connection test failed", e)
                _testResult.value = TestResult.Failure(e.message ?: "连接失败")
            }
        }
    }

    fun resetTestResult() {
        _testResult.value = TestResult.Idle
    }
}
