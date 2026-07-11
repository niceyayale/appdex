package com.appdex.common

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Lightweight event bus for cross-module media navigation.
 * Used to open image/audio/video files from file manager without direct module dependencies.
 */
object MediaNavigationBus {

    private val _events = MutableSharedFlow<MediaOpenRequest>(extraBufferCapacity = 1)
    val events: SharedFlow<MediaOpenRequest> = _events.asSharedFlow()

    fun openImage(path: String, allPaths: List<String> = listOf(path)) {
        _events.tryEmit(MediaOpenRequest.Image(path, allPaths))
    }

    fun openAudio(paths: List<String>, index: Int = 0) {
        _events.tryEmit(MediaOpenRequest.Audio(paths, index))
    }

    fun openVideo(path: String) {
        _events.tryEmit(MediaOpenRequest.Video(path))
    }

    fun openApk(path: String) {
        _events.tryEmit(MediaOpenRequest.Apk(path))
    }
}

sealed interface MediaOpenRequest {
    val path: String

    data class Image(
        override val path: String,
        val allPaths: List<String>
    ) : MediaOpenRequest

    data class Audio(
        override val path: String,
        val allPaths: List<String>,
        val index: Int
    ) : MediaOpenRequest {
        constructor(paths: List<String>, index: Int) : this(paths.getOrNull(index) ?: "", paths, index)
    }

    data class Video(
        override val path: String
    ) : MediaOpenRequest

    data class Apk(
        override val path: String
    ) : MediaOpenRequest
}
