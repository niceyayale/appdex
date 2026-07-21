package com.appdex.common

import android.util.Log

sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val message: String, val cause: Throwable? = null) : Result<Nothing>()
    data object Loading : Result<Nothing>()
}

inline fun <T> runCatchingResult(block: () -> T): Result<T> {
    return try {
        Result.Success(block())
    } catch (e: Exception) {
        Log.w("AppX", "Suppressed exception", e)
        Result.Error(e.message ?: "Unknown error", e)
    }
}
