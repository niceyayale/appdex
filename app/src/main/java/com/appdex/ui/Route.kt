package com.appdex.ui

import kotlinx.serialization.Serializable

sealed interface Route {
    @Serializable
    data object Files : Route

    @Serializable
    data class Editor(val filePath: String) : Route

    @Serializable
    data class Analyzer(val apkPath: String) : Route

    @Serializable
    data object Settings : Route
}
