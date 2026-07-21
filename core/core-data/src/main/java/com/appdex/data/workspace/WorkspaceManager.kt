package com.appdex.data.workspace

import android.util.Log
import com.appdex.data.session.SessionManager
import com.appdex.data.session.SessionStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

// ═══════════════════════════════════════════════════════════════
// WorkspaceManager — The Central Manager
// ═══════════════════════════════════════════════════════════════
// Wraps SessionManager. Adds workspace-level state management.
// Follows Engine Protection principle: wrap, don't rewrite.
// ═══════════════════════════════════════════════════════════════

@Singleton
class WorkspaceManager @Inject constructor(
    private val sessionManager: SessionManager
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // ── Workspace State ──
    private val _workspaces = MutableStateFlow<List<Workspace>>(emptyList())
    val workspaces: StateFlow<List<Workspace>> = _workspaces.asStateFlow()

    private val _currentWorkspace = MutableStateFlow<Workspace?>(null)
    val currentWorkspace: StateFlow<Workspace?> = _currentWorkspace.asStateFlow()

    // ── Delegated session access (backward compatibility) ──
    val sessions get() = sessionManager.sessions
    val currentSession get() = sessionManager.currentSession
    val displayMode get() = sessionManager.displayMode

    // ── Workspace lifecycle ──

    /**
     * Create a new empty workspace.
     */
    fun createWorkspace(): Workspace {
        val session = sessionManager.createSession()
        val workspace = Workspace(
            id = session.id,  // Use same ID for simplicity
            session = session,
            state = WorkspaceState.EMPTY
        )
        _workspaces.update { it + workspace }
        _currentWorkspace.value = workspace
        addTimelineEvent(workspace.id, TimelineEventType.ANALYSIS, "工作区已创建")
        return workspace
    }

    /**
     * Select/open an existing workspace.
     */
    fun openWorkspace(workspaceId: String) {
        val workspace = _workspaces.value.find { it.id == workspaceId }
        if (workspace != null) {
            sessionManager.setCurrentSession(workspace.session.id)
            _currentWorkspace.value = workspace.copy(lastOpenedAt = System.currentTimeMillis())
        } else {
            // Try to restore from session
            val session = sessionManager.getCurrentSession()
            if (session != null) {
                val ws = Workspace(id = session.id, session = session)
                _currentWorkspace.value = ws
            }
        }
    }

    /**
     * Delete a workspace.
     */
    fun deleteWorkspace(workspaceId: String) {
        _workspaces.update { it.filter { ws -> ws.id != workspaceId } }
        sessionManager.deleteSession(workspaceId)
        if (_currentWorkspace.value?.id == workspaceId) {
            _currentWorkspace.value = _workspaces.value.firstOrNull()
        }
    }

    /**
     * Get current workspace.
     */
    fun getCurrentWorkspace(): Workspace? = _currentWorkspace.value

    /**
     * Update current workspace.
     */
    fun updateCurrentWorkspace(updater: (Workspace) -> Workspace) {
        val current = _currentWorkspace.value ?: return
        val updated = updater(current)
        _workspaces.update { workspaces ->
            workspaces.map { if (it.id == updated.id) updated else it }
        }
        _currentWorkspace.value = updated
    }

    /**
     * Update workspace state.
     */
    fun setState(state: WorkspaceState) {
        updateCurrentWorkspace { it.copy(state = state) }
    }

    // ── Context Management ──

    /**
     * Set the active panel in the workspace.
     */
    fun setActivePanel(panel: WorkspacePanel) {
        updateCurrentWorkspace { it.copy(context = it.context.copy(activePanel = panel)) }
    }

    /**
     * Set the active file being viewed/edited.
     */
    fun setActiveFile(path: String?, content: String? = null) {
        updateCurrentWorkspace { ws ->
            ws.copy(context = ws.context.copy(
                activeFilePath = path,
                activeFileContent = content
            ))
        }
    }

    /**
     * Set the current goal.
     */
    fun setGoal(goal: Goal?) {
        updateCurrentWorkspace { ws ->
            ws.copy(
                context = ws.context.copy(activeGoal = goal),
                goals = if (goal != null) ws.goals + goal else ws.goals
            )
        }
        if (goal != null) {
            _currentWorkspace.value?.let { ws ->
                addTimelineEvent(ws.id, TimelineEventType.GOAL_SET, "目标: ${goal.title}")
            }
        }
    }

    /**
     * Record a recent action for AI context.
     */
    fun recordAction(action: String) {
        updateCurrentWorkspace { ws ->
            ws.copy(context = ws.context.copy(
                recentActions = (ws.context.recentActions + action).takeLast(10)
            ))
        }
    }

    // ── Timeline ──

    /**
     * Add a timeline event.
     */
    fun addTimelineEvent(workspaceId: String, type: TimelineEventType, title: String, description: String? = null) {
        val event = TimelineEvent(
            type = type,
            title = title,
            description = description,
            sessionId = workspaceId
        )
        _workspaces.update { workspaces ->
            workspaces.map { ws ->
                if (ws.id == workspaceId) {
                    ws.copy(timeline = ws.timeline + event)
                } else ws
            }
        }
        if (_currentWorkspace.value?.id == workspaceId) {
            _currentWorkspace.value = _currentWorkspace.value?.copy(
                timeline = (_currentWorkspace.value?.timeline ?: emptyList()) + event
            )
        }
    }

    // ── Tags & Notes ──

    fun addTag(tag: String) {
        updateCurrentWorkspace { ws ->
            ws.copy(tags = (ws.tags + tag).distinct())
        }
    }

    fun setNotes(notes: String) {
        updateCurrentWorkspace { it.copy(notes = notes) }
    }

    // ── Sync with SessionManager ──

    /**
     * Called when session is updated. Syncs the workspace with the latest session data.
     */
    fun syncWithSession() {
        val session = sessionManager.getCurrentSession() ?: return
        val ws = _currentWorkspace.value
        if (ws != null) {
            val newState = when (session.status) {
                SessionStatus.IDLE -> WorkspaceState.EMPTY
                SessionStatus.LOADING, SessionStatus.ANALYZING, SessionStatus.SUMMARIZING -> WorkspaceState.ANALYZING
                SessionStatus.READY -> WorkspaceState.READY
                SessionStatus.MODIFIED -> WorkspaceState.MODIFYING
                SessionStatus.REPACKED -> WorkspaceState.REBUILDING
                SessionStatus.SIGNED -> WorkspaceState.SIGNING
                SessionStatus.INSTALLED -> WorkspaceState.INSTALLING
            }
            _currentWorkspace.value = ws.copy(session = session, state = newState)
        }
    }

    // ── Display Mode ──

    fun setDisplayMode(mode: com.appdex.data.session.ToolDisplayMode) {
        sessionManager.setDisplayMode(mode)
    }

    // ── Restore from persisted sessions ──

    /**
     * Restore workspaces from persisted sessions.
     */
    fun restoreFromSessions() {
        scope.launch {
            try {
                val sessions = sessionManager.sessions.value
                if (sessions.isNotEmpty()) {
                    val workspaces = sessions.map { session ->
                        Workspace(
                            id = session.id,
                            session = session,
                            state = when (session.status) {
                                SessionStatus.READY -> WorkspaceState.READY
                                SessionStatus.IDLE -> WorkspaceState.EMPTY
                                else -> WorkspaceState.READY
                            }
                        )
                    }
                    _workspaces.value = workspaces
                    _currentWorkspace.value = workspaces.lastOrNull()
                    Log.i("WorkspaceManager", "Restored ${workspaces.size} workspaces")
                }
            } catch (e: Exception) {
                Log.w("WorkspaceManager", "Failed to restore workspaces", e)
            }
        }
    }
}
