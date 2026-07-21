package com.appdex.arch

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ChannelResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

abstract class BaseViewModel<I : MviIntent, S : MviState, E : MviEffect>(
    initialState: S,
    protected val savedStateHandle: SavedStateHandle? = null
) : ViewModel() {

    private val _state = MutableStateFlow(
        savedStateHandle?.get<@Suppress("UNCHECKED_CAST") S>(STATE_KEY) ?: initialState
    )
    val state: StateFlow<S> = _state.asStateFlow()

    private val _effects = Channel<E>(Channel.BUFFERED)
    val effects: Flow<E> = _effects.receiveAsFlow()

    protected val currentState: S get() = _state.value

    protected fun update(reducer: (S) -> S) {
        _state.update(reducer)
        try {
            savedStateHandle?.set(STATE_KEY, _state.value)
        } catch (_: Exception) {
            // State not serializable — use saveState() for individual fields instead
        }
    }

    protected fun emitEffect(effect: E): ChannelResult<Unit> {
        return _effects.trySend(effect)
    }

    protected fun launchEffect(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }

    /**
     * Persist a single key-value pair into SavedStateHandle for process-death survival.
     */
    protected fun <T> saveState(key: String, value: T) {
        savedStateHandle?.set(key, value)
    }

    /**
     * Retrieve a previously saved value from SavedStateHandle.
     */
    protected fun <T> restoreState(key: String, default: T): T {
        return savedStateHandle?.get<T>(key) ?: default
    }

    abstract fun handleIntent(intent: I)

    companion object {
        private const val STATE_KEY = "base_view_model_state"
    }
}
