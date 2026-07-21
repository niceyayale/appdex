package com.appdex.data.session

/**
 * 唯一安全评分计算器 — RiskScore Pipeline 的核心。
 *
 * 架构:
 *   APK Analysis Result → SecurityFinding → RiskScoreCalculator → Session.securityScore
 *
 * 所有页面（Workspace / Report / AI Summary / Security Screen）
 * 必须读取同一个 Session.securityScore，禁止页面自己计算评分。
 */
object RiskScoreCalculator {

    // ── 权重定义（唯一来源）──
    const val WEIGHT_CRITICAL = 25
    const val WEIGHT_HIGH = 15
    const val WEIGHT_MEDIUM = 8
    const val WEIGHT_LOW = 3

    /**
     * 根据 [FindingSeverity] 列表计算安全评分 (0-100)。
     * 用于 ToolBridge.generateFindings() 和任何使用 AnalysisFinding 的地方。
     */
    fun calculate(findings: List<AnalysisFinding>): Int {
        val critical = findings.count { it.severity == FindingSeverity.CRITICAL }
        val high = findings.count { it.severity == FindingSeverity.HIGH }
        val medium = findings.count { it.severity == FindingSeverity.MEDIUM }
        val low = findings.count { it.severity == FindingSeverity.LOW }
        return calculate(critical, high, medium, low)
    }

    /**
     * 根据原始严重级别计数计算安全评分 (0-100)。
     * 用于 SecurityScannerRepository 等不使用 AnalysisFinding 的地方。
     */
    fun calculate(critical: Int, high: Int, medium: Int, low: Int): Int {
        return maxOf(0, 100 - critical * WEIGHT_CRITICAL - high * WEIGHT_HIGH - medium * WEIGHT_MEDIUM - low * WEIGHT_LOW)
    }

    /**
     * 获取风险等级文本。
     */
    fun getRiskLevel(score: Int): String = when {
        score >= 80 -> "低风险"
        score >= 60 -> "中风险"
        score >= 40 -> "高风险"
        else -> "极高风险"
    }

    /**
     * 获取风险颜色标签。
     */
    fun getRiskColor(score: Int): String = when {
        score >= 80 -> "green"
        score >= 60 -> "amber"
        score >= 40 -> "orange"
        else -> "red"
    }
}
