package com.appdex.ui

import kotlinx.serialization.Serializable

sealed interface Route {
    @Serializable
    data object Files : Route

    @Serializable
    data object Editor : Route

    @Serializable
    data object Analyzer : Route

    @Serializable
    data object Settings : Route
}
