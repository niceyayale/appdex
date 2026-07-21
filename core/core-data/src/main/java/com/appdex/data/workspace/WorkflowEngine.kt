package com.appdex.data.workspace

import android.util.Log
import com.appdex.data.session.SessionManager
import com.appdex.data.session.SessionStatus
import com.appdex.data.session.StepStatus
import com.appdex.data.session.WorkflowStep
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

// ═══════════════════════════════════════════════════════════════
// AppX Workflow Engine — Graph-based workflow execution
// ═══════════════════════════════════════════════════════════════
// "There are no isolated screens. Everything becomes workflows.
//  Each workflow is a graph. Node → Node → Node → Node."
// ═══════════════════════════════════════════════════════════════

/**
 * A workflow node — represents a single step in a workflow graph.
 */
data class WorkflowNode(
    val id: String,
    val name: String,
    val friendlyName: String,
    val description: String,
    val category: NodeCategory,
    val dependencies: List<String> = emptyList(),  // IDs of nodes that must complete first
    val estimatedDurationMs: Long = 1000
)

enum class NodeCategory {
    PARSE,          // Parse APK
    ANALYZE,        // Analyze manifest, permissions
    SCAN,           // Scan SDK, resources, code
    SECURITY,       // Security check
    SUMMARY,        // AI summary
    MODIFY,         // Create patch
    BUILD,          // Rebuild APK
    SIGN,           // Sign APK
    INSTALL,        // Install APK
    EXPORT          // Export report
}

/**
 * Runtime state of a workflow node.
 */
data class NodeState(
    val node: WorkflowNode,
    val status: NodeStatus = NodeStatus.PENDING,
    val progress: Float = 0f,
    val message: String = "",
    val result: String? = null,
    val startTime: Long = 0,
    val endTime: Long = 0
) {
    val isCompleted: Boolean get() = status == NodeStatus.DONE
    val isRunning: Boolean get() = status == NodeStatus.RUNNING
    val durationMs: Long get() = if (startTime > 0 && endTime > 0) endTime - startTime else 0
}

enum class NodeStatus { PENDING, RUNNING, DONE, SKIPPED, ERROR }

/**
 * A workflow definition — a graph of nodes.
 */
data class Workflow(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val nodes: List<WorkflowNode>,
    val goal: Goal? = null
) {
    /**
     * Get the execution order using topological sort.
     */
    fun executionOrder(): List<WorkflowNode> {
        val result = mutableListOf<WorkflowNode>()
        val visited = mutableSetOf<String>()
        val visiting = mutableSetOf<String>()

        fun visit(node: WorkflowNode) {
            if (node.id in visited) return
            if (node.id in visiting) return  // Cycle detected, skip
            visiting.add(node.id)
            node.dependencies.forEach { depId ->
                nodes.find { it.id == depId }?.let { visit(it) }
            }
            visiting.remove(node.id)
            visited.add(node.id)
            result.add(node)
        }

        nodes.forEach { visit(it) }
        return result
    }
}

/**
 * Workflow execution state.
 */
data class WorkflowExecutionState(
    val workflow: Workflow,
    val nodeStates: Map<String, NodeState> = emptyMap(),
    val isRunning: Boolean = false,
    val isComplete: Boolean = false,
    val currentNodeId: String? = null,
    val error: String? = null
) {
    val progress: Float
        get() {
            if (nodeStates.isEmpty()) return 0f
            val doneCount = nodeStates.values.count { it.isCompleted }
            return doneCount.toFloat() / nodeStates.size
        }

    val currentNode: NodeState?
        get() = currentNodeId?.let { nodeStates[it] }
}

// ═══════════════════════════════════════════════════════════════
// Workflow Templates
// ═══════════════════════════════════════════════════════════════

object WorkflowTemplates {
    /**
     * Full analysis workflow — the default pipeline.
     */
    val fullAnalysis = Workflow(
        name = "完整分析",
        nodes = listOf(
            WorkflowNode("parse", "解析 APK", "读取应用文件", "解析 APK 文件结构", NodeCategory.PARSE),
            WorkflowNode("manifest", "分析 Manifest", "检查配置", "分析 AndroidManifest.xml", NodeCategory.ANALYZE, listOf("parse")),
            WorkflowNode("sdk", "扫描 SDK", "检测第三方 SDK", "检测广告和追踪 SDK", NodeCategory.SCAN, listOf("parse")),
            WorkflowNode("resources", "扫描资源", "检查资源文件", "扫描资源文件结构", NodeCategory.SCAN, listOf("parse")),
            WorkflowNode("code", "扫描代码", "分析代码结构", "分析 DEX 代码结构", NodeCategory.SCAN, listOf("parse")),
            WorkflowNode("security", "安全检查", "安全扫描", "检查危险权限和安全风险", NodeCategory.SECURITY, listOf("manifest", "sdk")),
            WorkflowNode("summary", "AI 总结", "生成报告", "AI 生成分析报告", NodeCategory.SUMMARY, listOf("security", "resources", "code"))
        )
    )

    /**
     * Security-only workflow.
     */
    val securityScan = Workflow(
        name = "安全扫描",
        nodes = listOf(
            WorkflowNode("parse", "解析 APK", "读取应用文件", "解析 APK 文件结构", NodeCategory.PARSE),
            WorkflowNode("manifest", "分析 Manifest", "检查配置", "分析权限和组件", NodeCategory.ANALYZE, listOf("parse")),
            WorkflowNode("security", "安全检查", "安全扫描", "检查安全风险", NodeCategory.SECURITY, listOf("manifest")),
            WorkflowNode("summary", "安全报告", "生成报告", "生成安全评估报告", NodeCategory.SUMMARY, listOf("security"))
        )
    )

    /**
     * Modification workflow.
     */
    val modification = Workflow(
        name = "修改 APK",
        nodes = listOf(
            WorkflowNode("parse", "解析 APK", "读取应用文件", "解析 APK 文件结构", NodeCategory.PARSE),
            WorkflowNode("modify", "修改文件", "编辑资源或代码", "修改 APK 内的文件", NodeCategory.MODIFY, listOf("parse")),
            WorkflowNode("build", "重新打包", "构建 APK", "将修改后的文件重新打包", NodeCategory.BUILD, listOf("modify")),
            WorkflowNode("sign", "签名", "签名 APK", "对 APK 进行签名", NodeCategory.SIGN, listOf("build"))
        )
    )

    /**
     * Get workflow for a goal.
     */
    fun forGoal(goal: Goal): Workflow {
        return when (goal.category) {
            com.appdex.data.workspace.GoalCategory.SECURITY -> securityScan
            com.appdex.data.workspace.GoalCategory.MODIFY -> modification
            else -> fullAnalysis
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// Workflow Engine — Executes workflows
// ═══════════════════════════════════════════════════════════════

@Singleton
class WorkflowEngine @Inject constructor(
    private val sessionManager: SessionManager
) {
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _executionState = MutableStateFlow<WorkflowExecutionState?>(null)
    val executionState: StateFlow<WorkflowExecutionState?> = _executionState.asStateFlow()

    /**
     * Start executing a workflow for a session.
     * The actual execution is handled by the ViewModel — this engine manages the state.
     */
    fun startWorkflow(workflow: Workflow, sessionId: String) {
        val nodeStates = workflow.nodes.associate { it.id to NodeState(it) }
        _executionState.value = WorkflowExecutionState(
            workflow = workflow,
            nodeStates = nodeStates,
            isRunning = true
        )
    }

    /**
     * Update a node's status.
     */
    fun updateNode(nodeId: String, status: NodeStatus, message: String = "", progress: Float = 0f) {
        val current = _executionState.value ?: return
        val nodeState = current.nodeStates[nodeId] ?: return

        val updatedState = nodeState.copy(
            status = status,
            message = message,
            progress = progress,
            startTime = if (status == NodeStatus.RUNNING) System.currentTimeMillis() else nodeState.startTime,
            endTime = if (status == NodeStatus.DONE || status == NodeStatus.ERROR) System.currentTimeMillis() else nodeState.endTime
        )

        _executionState.value = current.copy(
            nodeStates = current.nodeStates + (nodeId to updatedState),
            currentNodeId = if (status == NodeStatus.RUNNING) nodeId else current.currentNodeId
        )

        // Sync with SessionManager workflow steps
        syncWithSession(nodeId, status, message, progress)
    }

    /**
     * Complete a node.
     */
    fun completeNode(nodeId: String, result: String? = null) {
        val current = _executionState.value ?: return
        updateNode(nodeId, NodeStatus.DONE, result ?: "完成", 1f)

        // Check if all nodes are done
        val updated = _executionState.value ?: return
        val allDone = updated.nodeStates.values.all { it.isCompleted }
        if (allDone) {
            _executionState.value = updated.copy(
                isRunning = false,
                isComplete = true,
                currentNodeId = null
            )
        }
    }

    /**
     * Mark workflow as error.
     */
    fun error(message: String) {
        val current = _executionState.value ?: return
        _executionState.value = current.copy(
            isRunning = false,
            error = message
        )
    }

    /**
     * Reset workflow state.
     */
    fun reset() {
        _executionState.value = null
    }

    /**
     * Get current progress as a float (0..1).
     */
    fun getProgress(): Float = _executionState.value?.progress ?: 0f

    /**
     * Get current node label.
     */
    fun getCurrentLabel(): String {
        val state = _executionState.value ?: return ""
        val nodeId = state.currentNodeId ?: return ""
        return state.nodeStates[nodeId]?.node?.friendlyName ?: ""
    }

    /**
     * Sync workflow engine state with SessionManager's workflow steps.
     */
    private fun syncWithSession(nodeId: String, status: NodeStatus, message: String, progress: Float) {
        val state = _executionState.value ?: return
        val sessionId = sessionManager.currentSessionId.value ?: return

        // Map node IDs to WorkflowStep enum values
        val step = when (nodeId) {
            "parse" -> WorkflowStep.READ_APK
            "manifest" -> WorkflowStep.ANALYZE_PERMISSIONS
            "sdk" -> WorkflowStep.SCAN_SDK
            "resources" -> WorkflowStep.SCAN_RESOURCES
            "code" -> WorkflowStep.SCAN_CODE
            "security" -> WorkflowStep.ANALYZE_PERMISSIONS  // Map to closest step
            "summary" -> WorkflowStep.AI_SUMMARY
            else -> null
        }

        if (step != null) {
            val stepStatus = when (status) {
                NodeStatus.PENDING -> StepStatus.PENDING
                NodeStatus.RUNNING -> StepStatus.RUNNING
                NodeStatus.DONE -> StepStatus.DONE
                NodeStatus.SKIPPED -> StepStatus.DONE
                NodeStatus.ERROR -> StepStatus.ERROR
            }
            sessionManager.updateWorkflowStep(sessionId, step, stepStatus, message, progress)
        }
    }
}
