package com.appdex.data.workspace

import android.util.Log
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

// ═══════════════════════════════════════════════════════════════
// WorkspaceEventBus — The Central Nervous System
// ═══════════════════════════════════════════════════════════════
// Tools NEVER call each other directly.
// All communication flows through events.
// Any tool can emit. Any tool can observe.
// ═══════════════════════════════════════════════════════════════

/**
 * All workspace events. Tools emit these; the WorkspaceController
 * (and any interested tool) observes them.
 *
 * Principle: events are intentions, not commands.
 * The WorkspaceController decides what to do with them.
 */
sealed class WorkspaceEvent {
    // ── Selection Events (Phase C) ──
    data class SelectClass(val className: String, val dexFile: String? = null) : WorkspaceEvent()
    data class SelectMethod(val className: String, val methodName: String, val descriptor: String? = null) : WorkspaceEvent()
    data class SelectXmlNode(val tagName: String, val name: String, val parent: String? = null) : WorkspaceEvent()
    data class SelectPermission(val name: String) : WorkspaceEvent()
    data class SelectComponent(val name: String, val type: ComponentType, val exported: Boolean = false) : WorkspaceEvent()
    data class SelectResource(val resourceId: String, val type: String? = null, val value: String? = null) : WorkspaceEvent()
    data class SelectFile(val path: String, val displayName: String? = null) : WorkspaceEvent()
    data class SelectOffset(val address: Long, val file: String? = null) : WorkspaceEvent()
    data class SelectFinding(val findingId: String, val title: String) : WorkspaceEvent()
    data class SelectField(val className: String, val fieldName: String, val type: String? = null) : WorkspaceEvent()
    data class SelectString(val value: kotlin.String, val className: kotlin.String? = null) : WorkspaceEvent()
    data object ClearSelection : WorkspaceEvent()

    // ── Navigation Events (Phase H) ──
    data class OpenTool(val tool: WorkspaceTool, val selection: WorkspaceSelection? = null) : WorkspaceEvent()
    data object CloseTool : WorkspaceEvent()
    data class NavigateBack(val toTool: WorkspaceTool? = null) : WorkspaceEvent()

    // ── Lifecycle Events ──
    data class ApkLoaded(val filePath: String, val packageName: String) : WorkspaceEvent()
    data class AnalysisCompleted(val score: Int, val findingCount: Int) : WorkspaceEvent()
    data class SecurityUpdated(val score: Int, val findingCount: Int) : WorkspaceEvent()
    data class RepackCompleted(val outputPath: String) : WorkspaceEvent()
    data class SignCompleted(val outputPath: String) : WorkspaceEvent()
    data class EditCompleted(val filePath: String, val description: String) : WorkspaceEvent()

    // ── Workspace State Events ──
    data object WorkspaceChanged : WorkspaceEvent()
    data class SearchRequested(val query: String, val type: SearchType = SearchType.ALL) : WorkspaceEvent()
    data class PinItem(val label: String, val selection: WorkspaceSelection, val tool: WorkspaceTool) : WorkspaceEvent()
    data object UnpinLast : WorkspaceEvent()

    // ── AI Events (Phase F) ──
    data class AIReply(val content: String, val actionCards: List<String> = emptyList()) : WorkspaceEvent()
    data class AIInsight(val title: String, val detail: String, val tool: WorkspaceTool) : WorkspaceEvent()
    data object RequestAIContext : WorkspaceEvent()

    // ── Inspector Events (Phase D) ──
    data class Inspect(val target: InspectionTarget) : WorkspaceEvent()
}

/**
 * The central event bus. Singleton, injected everywhere.
 *
 * Usage:
 *   class SomeViewModel @Inject constructor(bus: WorkspaceEventBus) {
 *       fun onClassClicked(name: String) {
 *           bus.emit(WorkspaceEvent.SelectClass(name))
 *       }
 *   }
 *
 *   class AnotherViewModel @Inject constructor(bus: WorkspaceEventBus) {
 *       bus.events.collect { event ->
 *           if (event is WorkspaceEvent.SelectClass) { ... auto-navigate ... }
 *       }
 *   }
 */
@Singleton
class WorkspaceEventBus @Inject constructor() {

    private val _events = MutableSharedFlow<WorkspaceEvent>(
        replay = 0,           // Don't replay — live stream
        extraBufferCapacity = 64
    )
    val events: SharedFlow<WorkspaceEvent> = _events.asSharedFlow()

    /**
     * Emit an event. Safe to call from any thread.
     */
    fun emit(event: WorkspaceEvent) {
        Log.d("WorkspaceEventBus", "Event: ${event::class.simpleName}")
        _events.tryEmit(event)
    }

    /**
     * Convenience: select something and optionally open a tool.
     */
    fun select(selection: WorkspaceSelection, openTool: WorkspaceTool? = null) {
        val event = when (selection) {
            is WorkspaceSelection.Class -> WorkspaceEvent.SelectClass(selection.className, selection.dexFile)
            is WorkspaceSelection.Method -> WorkspaceEvent.SelectMethod(selection.className, selection.methodName, selection.descriptor)
            is WorkspaceSelection.Field -> WorkspaceEvent.SelectField(selection.className, selection.fieldName, selection.type)
            is WorkspaceSelection.StringValue -> WorkspaceEvent.SelectString(selection.value, selection.className)
            is WorkspaceSelection.XmlNode -> WorkspaceEvent.SelectXmlNode(selection.tagName, selection.name, selection.parent)
            is WorkspaceSelection.Permission -> WorkspaceEvent.SelectPermission(selection.name)
            is WorkspaceSelection.Component -> WorkspaceEvent.SelectComponent(selection.name, selection.type, selection.exported)
            is WorkspaceSelection.Resource -> WorkspaceEvent.SelectResource(selection.resourceId, selection.type, selection.value)
            is WorkspaceSelection.File -> WorkspaceEvent.SelectFile(selection.path, selection.displayName)
            is WorkspaceSelection.Offset -> WorkspaceEvent.SelectOffset(selection.address, selection.file)
            is WorkspaceSelection.Finding -> WorkspaceEvent.SelectFinding(selection.findingId, selection.title)
            is WorkspaceSelection.None -> WorkspaceEvent.ClearSelection
        }
        emit(event)
        if (openTool != null) {
            emit(WorkspaceEvent.OpenTool(openTool, selection))
        }
    }
}
