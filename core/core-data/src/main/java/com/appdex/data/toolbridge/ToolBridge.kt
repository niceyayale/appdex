package com.appdex.data.toolbridge

import com.appdex.apk.ApkInfo
import com.appdex.data.session.AnalysisSession
import com.appdex.data.session.FindingSeverity
import com.appdex.data.session.AnalysisFinding

/**
 * ToolBridge - AI 与工具之间的桥梁
 *
 * 职责:
 * - 构建 AI 系统提示词（Copilot 模式）
 * - 将 APK 分析数据转换为 AI 可理解的上下文（Token 裁剪）
 * - 从 APK 信息自动生成安全发现
 * - 解析 AI 回复中的 Action Card
 * - 提供 AI 推荐问题
 */
object ToolBridge {

    private val DANGEROUS_PERMISSIONS = setOf(
        "android.permission.READ_SMS", "android.permission.SEND_SMS",
        "android.permission.RECEIVE_SMS", "android.permission.RECEIVE_MMS",
        "android.permission.READ_CONTACTS", "android.permission.WRITE_CONTACTS",
        "android.permission.READ_CALL_LOG", "android.permission.WRITE_CALL_LOG",
        "android.permission.CAMERA", "android.permission.RECORD_AUDIO",
        "android.permission.ACCESS_FINE_LOCATION", "android.permission.ACCESS_COARSE_LOCATION",
        "android.permission.ACCESS_BACKGROUND_LOCATION",
        "android.permission.READ_EXTERNAL_STORAGE", "android.permission.WRITE_EXTERNAL_STORAGE",
        "android.permission.MANAGE_EXTERNAL_STORAGE",
        "android.permission.SYSTEM_ALERT_WINDOW", "android.permission.REQUEST_INSTALL_PACKAGES",
        "android.permission.READ_PHONE_STATE", "android.permission.CALL_PHONE",
        "android.permission.BIND_ACCESSIBILITY_SERVICE",
        "android.permission.WRITE_SETTINGS", "android.permission.REBOOT",
        "android.permission.INSTALL_PACKAGES", "android.permission.DELETE_PACKAGES"
    )

    // ── AI System Prompt — Copilot 模式 ──

    fun buildSystemPrompt(session: AnalysisSession?): String = """
        你是 AppDex AI Copilot，一个专业的 Android APK 分析助手。

        ## 你的角色
        你不是聊天机器人。你是用户的 APK 分析伙伴。
        用户可能完全不懂逆向工程。你的工作是用自然语言帮助他们理解 APK。

        ## 你的能力
        1. 用通俗易懂的语言解释 APK 分析结果
        2. 给出安全风险评估和实用建议
        3. 根据用户需求推荐下一步操作
        4. 在回答中附带 Action Card 让用户直接进入对应工具

        ## 重要规则
        - 你可以：解释、定位、分析、指出修改位置、生成补丁建议
        - 你不能：帮助破解、去广告、绕过授权、修改支付逻辑、生成破解版 APK
        - 回答要简洁明了，适合非专业用户理解
        - 如果分析数据不可用，引导用户先选择 APK 进行分析
        - 用类比和通俗解释代替专业术语

        ## 回答格式
        - 正常回复用普通文本
        - 推荐工具时，在回复末尾添加 Action Card:
          [ACTION:标题|描述|图标类型|路由]
        - 图标类型: security, code, key, folder, terminal, edit, compare, database, memory, scan, hex
        - 路由: permissions, dex, signing, repack, security, size, axml, arsc, sqlite, elf, editor, terminal, files

        ## 用户意图识别
        当用户说"看看有没有广告"时，分析 SDK 和代码中的广告组件
        当用户说"看看有没有病毒"时，进行安全扫描和权限分析
        当用户说"为什么申请这么多权限"时，详细解释每个危险权限
        当用户说"我想改名字"时，引导到资源编辑器
        当用户说"我想改图标"时，引导到文件管理器定位图标资源
        当用户说"我想查看代码"时，引导到代码浏览器
    """.trimIndent()

    // ── Context Builder — Token 控制 + 深度上下文 ──

    fun buildContext(session: AnalysisSession?, maxTokens: Int = 3000): String {
        if (session?.apkInfo == null) {
            return "当前没有已分析的 APK。请引导用户选择 APK 文件。"
        }

        val info = session.apkInfo
        val sb = StringBuilder()

        sb.appendLine("=== APK 分析数据 ===")
        sb.appendLine("包名: ${info.manifest.packageName.ifEmpty { "未知" }}")
        sb.appendLine("版本: ${info.manifest.versionName.ifEmpty { "未知" }} (${info.manifest.versionCode})")
        sb.appendLine("文件大小: ${info.fileSize} bytes")
        if (info.manifest.minSdk > 0) sb.appendLine("最低系统版本: Android ${info.manifest.minSdk}")
        if (info.manifest.targetSdk > 0) sb.appendLine("目标系统版本: Android ${info.manifest.targetSdk}")

        // 权限
        val perms = info.manifest.permissions
        sb.appendLine("权限总数: ${perms.size}")
        val dangerousPerms = perms.filter { it in DANGEROUS_PERMISSIONS }
        if (dangerousPerms.isNotEmpty()) {
            sb.appendLine("危险权限 (${dangerousPerms.size}):")
            dangerousPerms.take(15).forEach { sb.appendLine("  - $it") }
            if (dangerousPerms.size > 15) sb.appendLine("  ... 还有 ${dangerousPerms.size - 15} 个")
        }
        val otherPerms = perms.filter { it !in DANGEROUS_PERMISSIONS }
        if (otherPerms.isNotEmpty()) {
            sb.appendLine("普通权限: ${otherPerms.take(10).joinToString(", ")}")
            if (otherPerms.size > 10) sb.appendLine(" (共 ${otherPerms.size} 个)")
        }

        // 组件详情 — 增强上下文
        sb.appendLine("页面(Activity)数: ${info.manifest.activities.size}")
        if (info.manifest.activities.isNotEmpty()) {
            sb.appendLine("  主要页面: ${info.manifest.activities.take(8).joinToString(", ")}")
        }
        sb.appendLine("后台服务(Service)数: ${info.manifest.services.size}")
        if (info.manifest.services.isNotEmpty()) {
            sb.appendLine("  服务列表: ${info.manifest.services.take(5).joinToString(", ")}")
        }
        sb.appendLine("广播接收器(Receiver): ${info.manifest.receivers.size}")
        sb.appendLine("内容提供者(Provider): ${info.manifest.providers.size}")

        // MetaData — SDK 检测线索
        if (info.manifest.metaData.isNotEmpty()) {
            sb.appendLine("MetaData (${info.manifest.metaData.size}):")
            info.manifest.metaData.entries.take(15).forEach { (k, v) ->
                sb.appendLine("  $k = ${v.take(60)}")
            }
        }

        // 签名
        if (info.signatures.isNotEmpty()) {
            sb.appendLine("签名: ${info.signatures.size} 个")
            info.signatures.take(2).forEach { sig ->
                sb.appendLine("  v${sig.version}: ${sig.algorithm}, 主体: ${sig.certificateSubject.take(80)}")
                sb.appendLine("  SHA-256: ${sig.sha256.take(32)}...")
            }
        } else {
            sb.appendLine("签名: 无")
        }

        // 文件统计 + 资源树
        val dexFiles = info.entries.filter { it.name.endsWith(".dex") }
        val soFiles = info.entries.filter { it.name.endsWith(".so") }
        val dbFiles = info.entries.filter { it.name.endsWith(".db") || it.name.endsWith(".sqlite") }
        val xmlFiles = info.entries.filter { it.name.endsWith(".xml") }
        val imgFiles = info.entries.filter { it.name.endsWith(".png") || it.name.endsWith(".jpg") || it.name.endsWith(".webp") }
        sb.appendLine("文件总数: ${info.entries.size}")
        sb.appendLine("代码文件(DEX): ${dexFiles.size}")
        sb.appendLine("原生库(SO): ${soFiles.size}")
        sb.appendLine("数据库(DB): ${dbFiles.size}")
        sb.appendLine("XML文件: ${xmlFiles.size}")
        sb.appendLine("图片资源: ${imgFiles.size}")

        // SO 库名称 — 检测 SDK 和架构
        if (soFiles.isNotEmpty()) {
            val soNames = soFiles.map { it.name.substringAfterLast('/') }
            sb.appendLine("原生库名称: ${soNames.take(10).joinToString(", ")}")
            // 检测常见 SDK
            val detectedSDKs = mutableSetOf<String>()
            if (soNames.any { it.contains("libapp", true) }) detectedSDKs.add("Flutter")
            if (soNames.any { it.contains("libreactnative", true) || it.contains("libhermes", true) }) detectedSDKs.add("React Native")
            if (soNames.any { it.contains("libil2cpp", true) }) detectedSDKs.add("Unity (IL2CPP)")
            if (soNames.any { it.contains("libunity", true) }) detectedSDKS_add(detectedSDKs, "Unity")
            if (soNames.any { it.contains("libUE4", true) || it.contains("libUnreal", true) }) detectedSDKs.add("Unreal Engine")
            if (soNames.any { it.contains("libweex", true) }) detectedSDKs.add("Weex")
            if (detectedSDKs.isNotEmpty()) {
                sb.appendLine("检测到框架: ${detectedSDKs.joinToString(", ")}")
            }
        }

        // 大文件 Top 5 — 体积分析线索
        val largeFiles = info.entries.filter { !it.isDirectory }.sortedByDescending { it.size }.take(5)
        if (largeFiles.isNotEmpty()) {
            sb.appendLine("最大文件:")
            largeFiles.forEach { e ->
                sb.appendLine("  ${e.name} (${e.size / 1024}KB)")
            }
        }

        // 安全评分
        sb.appendLine("安全评分: ${session.securityScore}/100")

        // 已有发现
        if (session.findings.isNotEmpty()) {
            sb.appendLine("已发现问题: ${session.findings.size} 项")
            session.findings.take(5).forEach { f ->
                sb.appendLine("  [${f.severity}] ${f.title}: ${f.description}")
            }
        }

        return sb.toString()
    }

    private fun detectedSDKS_add(set: MutableSet<String>, s: String) { set.add(s) }

    // ── Auto-Generate Findings ──

    fun generateFindings(info: ApkInfo): Pair<List<AnalysisFinding>, Int> {
        val findings = mutableListOf<AnalysisFinding>()
        var score = 100

        // 1. 危险权限
        val dangerousPerms = info.manifest.permissions.filter { it in DANGEROUS_PERMISSIONS }
        if (dangerousPerms.isNotEmpty()) {
            val severity = when {
                dangerousPerms.size >= 5 -> FindingSeverity.HIGH
                dangerousPerms.size >= 3 -> FindingSeverity.MEDIUM
                else -> FindingSeverity.LOW
            }
            findings.add(AnalysisFinding(
                severity = severity,
                category = "权限",
                title = "发现 ${dangerousPerms.size} 个敏感权限",
                description = "应用请求了 ${dangerousPerms.size} 个可能影响隐私或安全的权限：\n" +
                    dangerousPerms.take(5).joinToString("\n") { "• $it" } +
                    if (dangerousPerms.size > 5) "\n...等共 ${dangerousPerms.size} 个" else "",
                recommendation = "检查这些权限是否为应用核心功能所必需",
                toolAction = "permissions"
            ))
            score -= minOf(dangerousPerms.size * 4, 32)
        }

        // 2. 签名
        if (info.signatures.isEmpty()) {
            findings.add(AnalysisFinding(
                severity = FindingSeverity.CRITICAL,
                category = "签名",
                title = "应用未签名",
                description = "此 APK 没有有效的数字签名，无法正常安装",
                recommendation = "需要先对 APK 进行签名才能安装",
                toolAction = "signing"
            ))
            score -= 25
        } else {
            val hasV2 = info.signatures.any { it.version >= 2 }
            if (!hasV2) {
                findings.add(AnalysisFinding(
                    severity = FindingSeverity.MEDIUM,
                    category = "签名",
                    title = "使用旧版签名方案",
                    description = "应用仅使用 JAR 签名（V1），未使用更安全的 APK 签名方案 V2/V3",
                    recommendation = "建议使用 V2+ 签名方案以获得更好的安全保护",
                    toolAction = "signing"
                ))
                score -= 10
            }
        }

        // 3. SDK 版本
        if (info.manifest.minSdk in 1..16) {
            findings.add(AnalysisFinding(
                severity = FindingSeverity.MEDIUM,
                category = "兼容性",
                title = "支持过低的系统版本 (Android ${info.manifest.minSdk})",
                description = "应用支持最低 Android ${info.manifest.minSdk}，可能存在安全风险",
                recommendation = "低系统版本可能使用不安全的 API"
            ))
            score -= 12
        }

        // 4. 权限过多
        if (info.manifest.permissions.size > 30) {
            findings.add(AnalysisFinding(
                severity = FindingSeverity.MEDIUM,
                category = "权限",
                title = "申请了大量权限 (${info.manifest.permissions.size})",
                description = "应用请求了大量权限，可能存在过度收集用户数据的风险",
                recommendation = "审查权限列表，移除不必要的权限",
                toolAction = "permissions"
            ))
            score -= 8
        }

        // 5. 文件结构
        val dexFiles = info.entries.filter { it.name.endsWith(".dex") }
        if (dexFiles.size > 1) {
            findings.add(AnalysisFinding(
                severity = FindingSeverity.INFO,
                category = "结构",
                title = "包含多个代码文件 (${dexFiles.size})",
                description = "应用包含 ${dexFiles.size} 个代码文件，通常表示应用较大",
                toolAction = "dex"
            ))
        }

        val soFiles = info.entries.filter { it.name.endsWith(".so") }
        if (soFiles.isNotEmpty()) {
            findings.add(AnalysisFinding(
                severity = FindingSeverity.INFO,
                category = "结构",
                title = "包含原生库 (${soFiles.size})",
                description = "应用包含 ${soFiles.size} 个原生库文件",
                recommendation = "原生代码可能包含关键逻辑",
                toolAction = "elf"
            ))
        }

        val dbFiles = info.entries.filter { it.name.endsWith(".db") || it.name.endsWith(".sqlite") }
        if (dbFiles.isNotEmpty()) {
            findings.add(AnalysisFinding(
                severity = FindingSeverity.INFO,
                category = "数据",
                title = "包含数据库 (${dbFiles.size})",
                description = "应用内包含 ${dbFiles.size} 个数据库文件",
                toolAction = "sqlite"
            ))
        }

        // 6. 总结
        if (score >= 80) {
            findings.add(AnalysisFinding(
                severity = FindingSeverity.INFO,
                category = "总结",
                title = "安全评估：低风险",
                description = "此应用安全评分为 $score/100，整体风险较低",
                recommendation = "可以正常使用，但仍建议关注权限请求"
            ))
        } else if (score >= 60) {
            findings.add(AnalysisFinding(
                severity = FindingSeverity.LOW,
                category = "总结",
                title = "安全评估：中风险",
                description = "此应用安全评分为 $score/100，存在一定风险",
                recommendation = "建议仔细检查权限和签名"
            ))
        } else {
            findings.add(AnalysisFinding(
                severity = FindingSeverity.HIGH,
                category = "总结",
                title = "安全评估：高风险",
                description = "此应用安全评分为 $score/100，存在较高风险",
                recommendation = "不建议安装使用，建议详细分析"
            ))
        }

        return Pair(findings, score.coerceIn(0, 100))
    }

    // ── AI Suggested Questions ──

    fun getSuggestedQuestions(session: AnalysisSession?): List<Pair<String, String>> {
        return if (session?.apkInfo != null) {
            listOf(
                "这个 APK 安全吗？" to "评估应用的安全风险",
                "应用用了哪些权限？" to "查看权限分析",
                "有没有广告或追踪 SDK？" to "检测第三方 SDK",
                "应用签名有效吗？" to "检查签名信息"
            )
        } else {
            listOf(
                "分析一个 APK" to "选择 APK 文件开始分析",
                "扫描已安装应用" to "从设备中选择应用",
                "AppDex 能做什么？" to "了解 AppDex 功能",
                "如何修改 APK？" to "查看修改指南"
            )
        }
    }

    // ── AI Quick Actions — 首页快捷意图 ──

    fun getQuickActions(session: AnalysisSession?): List<QuickAction> {
        return if (session?.apkInfo != null) {
            listOf(
                QuickAction("继续分析", "查看当前 APK 的分析报告", "report", "scan"),
                QuickAction("问 AI", "询问关于这个 APK 的任何问题", "ai", "chat"),
                QuickAction("修改 APK", "编辑资源、代码或配置", "edit", "edit"),
                QuickAction("重新签名", "对修改后的 APK 签名", "signing", "key")
            )
        } else {
            listOf(
                QuickAction("分析 APK", "选择 APK 文件开始自动分析", "analyze", "scan"),
                QuickAction("扫描已安装应用", "从设备中选择已安装的应用", "scan_installed", "devices"),
                QuickAction("问 AI", "询问关于 APK 逆向的任何问题", "ai", "chat"),
                QuickAction("浏览文件", "查看设备上的文件", "files", "folder")
            )
        }
    }

    // ── Action Card Parser ──

    fun parseActionCards(content: String): List<ActionCardData> {
        val cards = mutableListOf<ActionCardData>()
        val pattern = Regex("""\[ACTION:([^|]+)\|([^|]+)\|([^|]+)\|([^\]]+)\]""")
        pattern.findAll(content).forEach { match ->
            val (title, description, iconType, route) = match.destructured
            cards.add(ActionCardData(
                title = title.trim(),
                description = description.trim(),
                iconType = iconType.trim(),
                route = route.trim()
            ))
        }
        return cards
    }

    fun stripActionCards(content: String): String {
        val pattern = Regex("""\[ACTION:[^|]+\|[^|]+\|[^|]+\|[^\]]+\]""")
        return pattern.replace(content, "").trim()
    }
}

// ── Data Classes ──

data class ActionCardData(
    val title: String,
    val description: String,
    val iconType: String,
    val route: String
)

data class QuickAction(
    val title: String,
    val description: String,
    val action: String,
    val iconType: String
)
