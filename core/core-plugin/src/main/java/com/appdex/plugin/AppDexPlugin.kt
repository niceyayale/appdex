package com.appdex.plugin

import androidx.compose.runtime.Composable

/**
 * Core plugin interface for AppX plugin system.
 * Plugins are lightweight utility modules that can be registered at runtime.
 * They are NOT loaded via DEX — they are compile-time registered components
 * that extend AppX functionality without modifying the core app.
 */
interface AppXPlugin {

    /** Unique identifier for this plugin */
    val id: String

    /** Display name shown in the plugin list */
    val name: String

    /** Short description of what the plugin does */
    val description: String

    /** Plugin author */
    val author: String

    /** Plugin version string */
    val version: String

    /** Category for grouping */
    val category: PluginCategory

    /** The Compose content to render when the plugin is opened */
    @Composable
    fun Content()
}

enum class PluginCategory(val label: String) {
    FORMAT("Format"),
    CONVERT("Convert"),
    ANALYZE("Analyze"),
    UTILITY("Utility"),
    FUN("Fun")
}

/**
 * Metadata for a plugin, used for listing without loading the content.
 */
data class PluginMetadata(
    val id: String,
    val name: String,
    val description: String,
    val author: String,
    val version: String,
    val category: PluginCategory,
    val enabled: Boolean = true
)

/**
 * Wraps a plugin instance with its metadata and enabled state.
 */
data class PluginEntry(
    val plugin: AppXPlugin,
    var enabled: Boolean = true
) {
    val metadata: PluginMetadata
        get() = PluginMetadata(
            id = plugin.id,
            name = plugin.name,
            description = plugin.description,
            author = plugin.author,
            version = plugin.version,
            category = plugin.category,
            enabled = enabled
        )
}
