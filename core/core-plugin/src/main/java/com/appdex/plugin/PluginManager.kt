package com.appdex.plugin

import java.util.concurrent.ConcurrentHashMap

/**
 * Central registry for managing AppX plugins.
 * Plugins are registered at app startup via [register] and can be
 * enabled/disabled at runtime.
 *
 * This is a compile-time plugin system — all plugins are bundled in the APK.
 * No dynamic DEX loading, ensuring security and stability.
 */
object PluginManager {

    private val plugins = ConcurrentHashMap<String, PluginEntry>()

    /** Register a plugin instance */
    fun register(plugin: AppXPlugin) {
        plugins[plugin.id] = PluginEntry(plugin)
    }

    /** Unregister a plugin by ID */
    fun unregister(id: String) {
        plugins.remove(id)
    }

    /** Get a plugin entry by ID */
    fun get(id: String): PluginEntry? = plugins[id]

    /** Get all registered plugins */
    fun getAll(): List<PluginEntry> = plugins.values.toList().sortedBy { it.plugin.name }

    /** Get all enabled plugins */
    fun getEnabled(): List<PluginEntry> = plugins.values.filter { it.enabled }.sortedBy { it.plugin.name }

    /** Get plugins by category */
    fun getByCategory(category: PluginCategory): List<PluginEntry> =
        plugins.values.filter { it.plugin.category == category }.sortedBy { it.plugin.name }

    /** Enable or disable a plugin */
    fun setEnabled(id: String, enabled: Boolean) {
        plugins[id]?.enabled = enabled
    }

    /** Check if a plugin is enabled */
    fun isEnabled(id: String): Boolean = plugins[id]?.enabled ?: false

    /** Get all metadata without loading content */
    fun getAllMetadata(): List<PluginMetadata> = getAll().map { it.metadata }

    /** Total plugin count */
    val count: Int get() = plugins.size
}
