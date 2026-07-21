# AppDex 2.0 AI Assistant System Design

> 完整 AI 系统设计 — 九个部分
>
> 产品定位：AppDex AI Assistant = 理解 APK + 解释分析结果 + 指导用户操作

---

# 第一部分：AI 总体架构

## 1.1 架构总览

```
┌──────────────────────────────────────────────────────────────────┐
│                        feature-ai-assistant                       │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────────┐  │
│  │ AiChatScreen │  │ AiChatViewModel│  │ ToolActionReceiver   │  │
│  │ (Compose UI) │  │ (MVI)         │  │ (处理AI跳转指令)     │  │
│  └──────┬───────┘  └──────┬───────┘  └──────────┬───────────┘  │
│         │                  │                      │               │
└─────────┼──────────────────┼──────────────────────┼──────────────┘
          │                  │                      │
          │           ┌──────┴──────────┐            │
          │           │    core-ai      │            │
          │           │                  │            │
          │     ┌─────┴──────────────────┴─────┐     │
          │     │                              │     │
          │  ┌──┴────────────┐  ┌─────────────┴──┐  │
          │  │  AiProvider    │  │  PromptEngine   │  │
          │  │  (OpenAI/      │  │  (System Prompt │  │
          │  │   Anthropic/   │  │   + Templates)  │  │
          │  │   Custom)      │  └─────────────────┘  │
          │  └──────┬─────────┘                       │
          │         │                                 │
          │  ┌──────┴────────────┐  ┌───────────────┐│
          │  │  AiClient         │  │ ContextBuilder││
          │  │  (HTTP/SSE Stream)│  │ (ApkInfo→Text)││
          │  └───────────────────┘  └──────┬────────┘│
          │                                 │         │
          │                          ┌──────┴────────┘
          │                          │
          │                   ┌──────┴──────┐
          │                   │  AiService   │ ← 高层 API
          │                   │  (ask/summary│
          │                   │   /stream)   │
          │                   └──────────────┘
          │                          │
          │                   ┌──────┴──────┐
          │                   │ ToolBridge   │ ← AI→AppDex 跳转
          │                   │ (Action Cards)│
          │                   └──────────────┘
          │                          │
└─────────┼──────────────────────────┘
          │
    ┌─────┴──────────────────────────────────┐
    │          ApkSessionManager (core-session)│
    │  StateFlow<ApkSession>                   │
    │  ├── Idle / Analyzing / Analyzed          │
    │  ├── Modified / Signed / Failed           │
    │  └── 持有 ApkInfo + SecurityScanResult     │
    └──────────────────────────────────────────┘
```

## 1.2 模块划分

### `core-ai` 模块

| 组件 | 包路径 | 职责 |
|---|---|---|
| `AiProvider` | `com.appdex.ai.provider` | Provider 抽象：OpenAI / Anthropic / Custom / Local |
| `AiConfig` | `com.appdex.ai.config` | 配置数据类 + DataStore 持久化 |
| `AiClient` | `com.appdex.ai.client` | HTTP 调用 + SSE 流式解析 |
| `PromptEngine` | `com.appdex.ai.prompt` | 系统提示词 + 任务模板 + Token 控制 |
| `ContextBuilder` | `com.appdex.ai.context` | ApkInfo → 结构化上下文文本 |
| `ToolBridge` | `com.appdex.ai.bridge` | AI 回复中的 Action Card 解析 + 跳转指令 |
| `AiService` | `com.appdex.ai` | 高层 API：`ask()` / `generateSummary()` / `testConnection()` |

### `feature-ai-assistant` 模块

| 组件 | 包路径 | 职责 |
|---|---|---|
| `AiChatScreen` | `com.appdex.ai.chat` | 对话 UI（Compose） |
| `AiChatViewModel` | `com.appdex.ai.chat` | MVI 状态管理 |
| `AiChatIntent` | `com.appdex.ai.chat` | 用户意图：发送消息、快捷问题、执行跳转 |
| `AiChatState` | `com.appdex.ai.chat` | UI 状态：消息列表、加载中、AI 配置状态 |
| `ToolActionReceiver` | `com.appdex.ai.chat` | 接收 ToolBridge 的跳转指令，调用 NavController |

## 1.3 AI Provider 详细设计

```
sealed interface AiProvider {
    val id: String
    val displayName: String
    val defaultBaseUrl: String
    val defaultModel: String
    val apiProtocol: ApiProtocol
    
    enum class ApiProtocol { OPENAI, ANTHROPIC }
    
    data object OpenAI : AiProvider {
        override val id = "openai"
        override val displayName = "OpenAI"
        override val defaultBaseUrl = "https://api.openai.com/v1"
        override val defaultModel = "gpt-4o-mini"
        override val apiProtocol = ApiProtocol.OPENAI
    }
    
    data object Anthropic : AiProvider {
        override val id = "anthropic"
        override val displayName = "Anthropic"
        override val defaultBaseUrl = "https://api.anthropic.com"
        override val defaultModel = "claude-3-5-sonnet-20241022"
        override val apiProtocol = ApiProtocol.ANTHROPIC
    }
    
    data object Local : AiProvider {
        override val id = "local"
        override val displayName = "本地模型"
        override val defaultBaseUrl = "http://localhost:11434/v1"
        override val defaultModel = "llama3"
        override val apiProtocol = ApiProtocol.OPENAI  // Ollama 兼容 OpenAI 协议
    }
    
    data class Custom(
        override val displayName: String,
        override val defaultBaseUrl: String,
        override val defaultModel: String = "",
        override val apiProtocol: ApiProtocol = ApiProtocol.OPENAI
    ) : AiProvider {
        override val id = "custom"
    }
}
```

### Provider 协议差异

| 维度 | OpenAI 协议 | Anthropic 协议 |
|---|---|---|
| 请求路径 | `{baseUrl}/chat/completions` | `{baseUrl}/v1/messages` |
| 认证头 | `Authorization: Bearer {key}` | `x-api-key: {key` + `anthropic-version: 2023-06-01` |
| 请求体 | `messages: [{role, content}]` | `messages: [{role, content}]` + `system` 顶级字段 |
| 流式 | `stream: true` → SSE `data: {choices[0].delta.content}` | `stream: true` → SSE `content_block_delta` |
| 响应解析 | `choices[0].message.content` | `content[0].text` |
| Token 限制 | `max_tokens` | `max_tokens` |
| 温度 | `temperature` (0-2) | `temperature` (0-1) |

## 1.4 AiClient 设计

```
class AiClient @Inject constructor(
    private val httpClient: OkHttpClient  // OkHttp, 30s timeout
) {
    // 同步请求
    suspend fun chat(
        config: AiConfig,
        messages: List<ChatMessage>
    ): Result<String>
    
    // 流式请求 — SSE
    fun chatStream(
        config: AiConfig,
        messages: List<ChatMessage>
    ): Flow<String>  // 逐 token 返回
    
    // 测试连接
    suspend fun testConnection(config: AiConfig): Result<String>
}

data class ChatMessage(
    val role: ChatRole,
    val content: String
)

enum class ChatRole { SYSTEM, USER, ASSISTANT }
```

### SSE 流式解析

OpenAI SSE 格式：
```
data: {"choices":[{"delta":{"content":"Hello"}}]}
data: {"choices":[{"delta":{"content":" world"}}]}
data: [DONE]
```

Anthropic SSE 格式：
```
event: content_block_delta
data: {"delta":{"text":"Hello"}}
```

AiClient 统一将两种格式解析为 `Flow<String>`，上层不感知差异。

## 1.5 PromptEngine 设计

```
class PromptEngine {
    // 系统提示词 — 定义 AI 角色
    fun buildSystemPrompt(level: AiCapabilityLevel): String
    
    // 任务模板 — 按场景选择
    fun buildTaskPrompt(
        template: PromptTemplate,
        context: String,
        userQuestion: String
    ): String
    
    // Token 估算（粗略：1 token ≈ 4 chars）
    fun estimateTokens(text: String): Int
    
    // Token 截断 — 确保不超模型限制
    fun truncateContext(context: String, maxTokens: Int): String
}
```

### 系统提示词

```
你是一个 APK 分析助手，集成在 AppDex 工具中。
你的职责是帮助用户理解 APK 的安全状况、权限用途和内部结构。

规则：
1. 用通俗易懂的语言回答，避免技术术语（除非用户明确要求技术细节）
2. 不要生成任何破解、绕过验证或修改付费功能的代码
3. 分析结果基于本地解析的数据，你无法访问网络
4. 如果用户想修改 APK，引导使用 AppDex 内置工具，不要直接修改文件
5. 对于危险操作（修改签名、修改 Manifest），必须提醒用户风险
6. 回答中使用 Action Card 格式（见下方）来建议用户跳转到具体工具

Action Card 格式（在回复末尾使用）：
[ACTION:tool_name|按钮文字]
例如：
[ACTION:arsc_editor|打开资源编辑器]
[ACTION:signing|进入签名流程]
[ACTION:dex_browser|打开 DEX 浏览器]

可用工具：
- arsc_editor: 资源编辑器（修改 resources.arsc）
- axml_editor: 清单编辑器（修改 AndroidManifest.xml）
- dex_browser: DEX 浏览器（查看类和方法）
- hex_editor: HEX 编辑器
- elf_viewer: ELF 查看器
- sqlite_viewer: SQLite 数据库查看器
- signing: APK 签名工具
- repack: APK 重打包工具
- security_scanner: 安全扫描器
- size_analyzer: 体积分析器
- diff: APK 对比工具
- file_manager: 文件管理器
- terminal: 终端
```

## 1.6 ContextBuilder 设计

```
class ContextBuilder @Inject constructor(
    private val sessionManager: ApkSessionManager
) {
    // 构建完整上下文（用于通用问答）
    fun buildFullContext(): String
    
    // 构建精简上下文（用于安全总结，控制 Token）
    fun buildSecurityContext(): String
    
    // 构建权限上下文
    fun buildPermissionContext(): String
    
    // 构建 DEX 结构上下文
    fun buildDexContext(): String
    
    // 构建文件结构上下文
    fun buildFileContext(): String
    
    // 构建签名上下文
    fun buildSignatureContext(): String
}
```

### 上下文分级策略

| 上下文级别 | 用途 | 包含数据 | 预估 Token |
|---|---|---|---|
| 精简 | 安全总结 | 基本信息 + 权限摘要 + 签名状态 + 评分 | ~300 |
| 标准 | 通用问答 | 基本信息 + 全部权限 + 组件数量 + 签名详情 + 安全发现 | ~600 |
| 详细 | 代码定位 | 标准 + DEX 类名列表（前 200）+ 文件结构摘要 | ~1500 |
| 完整 | 深度分析 | 详细 + 全部 DEX 类 + 完整文件列表 + 安全扫描详情 | ~3000+ |

---

# 第二部分：AI 能力等级

## Level 1：AI 分析助手

**定位**：面向普通用户，解读分析结果

**能力范围**：
- 读取 ApkInfo + SecurityScanResult
- 生成通俗易懂的安全报告
- 解释权限用途和风险
- 识别第三方 SDK
- 评估签名状态

**交互模式**：单向问答

**示例**：
```
用户："这个 APK 安全吗？"
AI：读取 ApkInfo + SecurityScanResult
    → 生成安全评估报告（通俗语言）
    → 返回纯文本回复（无 Action Card）
```

**禁止**：
- 不调用任何工具
- 不修改任何文件
- 不执行任何操作

## Level 2：AI 逆向辅助

**定位**：面向开发者和高级用户，提供分析和定位

**能力范围**：
- Level 1 全部能力
- 分析 DEX 类结构，定位可能相关代码
- 分析文件结构，定位修改位置
- 提供修改建议（步骤级别）
- 生成 Action Card 引导跳转到对应工具

**交互模式**：问答 + Action Card 跳转

**示例**：
```
用户："我想修改应用名称"
AI：分析 ApkInfo.entries → 找到 resources.arsc
    → 生成修改步骤
    → 返回 [ACTION:arsc_editor|打开资源编辑器]
    
用户点击 → 跳转 ArscEditorScreen
```

**禁止**：
- 不自动执行修改
- 不自动调用打包流程
- 所有跳转需要用户点击确认

## Level 3：AI Agent 实验能力

**定位**：AI 可以主动调用内部工具读取数据

**能力范围**：
- Level 2 全部能力
- 主动读取 APK 内文件内容（通过 Tool Bridge）
- 主动分析资源文件结构
- 生成完整的修改方案
- 调用打包流程（需要用户确认）

**Tool Bridge 可调用**：

| 工具 | 只读操作 | 写操作 |
|---|---|---|
| 文件读取 | `readFile(path)` — 读取 APK 内文件内容 | ❌ |
| DEX 搜索 | `searchDex(keyword)` — 搜索 DEX 类名 | ❌ |
| 资源分析 | `analyzeResources()` — 解析 arsc 结构 | ❌ |
| 安全扫描 | `runSecurityScan()` — 执行安全扫描 | ❌ |
| 打包 | ❌ | `startRepack()` — 需要用户确认 |
| 签名 | ❌ | `startSigning()` — 需要用户确认 |

**安全机制**：
- 所有只读操作自动执行
- 所有写操作生成确认对话框："AI 建议执行 [操作]，是否继续？"
- 用户必须点击确认才执行
- 每次操作有操作日志

**示例**：
```
用户："帮我分析这个 APK 的所有广告 SDK"
AI：
  1. [自动] 读取 ApkInfo.entries → 搜索 SDK 包名
  2. [自动] 调用 searchDex("com.google.ads") → 确认存在
  3. [自动] 调用 searchDex("com.facebook.ads") → 确认存在
  4. 生成报告：检测到 AdMob + Facebook Ads
  5. [ACTION:security_scanner|查看完整安全报告]
```

## 能力等级切换

| 等级 | 设置项 | 默认 |
|---|---|---|
| Level 1 | 始终启用 | ✅ |
| Level 2 | 设置页开关 `ai_level2_enabled` | ✅ |
| Level 3 | 设置页开关 `ai_level3_enabled` + 风险确认 | ❌ |

---

# 第三部分：真实用户场景（20 个）

## 场景 1（普通用户）：判断 APK 安全

```
用户问题："这个 APK 安全吗？"

AI 获取数据：
  - ApkInfo.manifest（权限、组件）
  - ApkInfo.signatures（签名方案）
  - SecurityScanResult（安全扫描结果）
  - SecurityScoreCalculator.calculateScore()

AI 处理：
  1. 读取安全评分 → 85/100
  2. 分析权限 → 5 个危险权限
  3. 检查签名 → V2+V3 有效
  4. 检查安全扫描 → 无 CRITICAL，2 个 MEDIUM

AI 回答：
  "这个 APK（微信）的安全评分为 85/100，属于低风险。
  
   ✓ 签名有效，使用 V2+V3 方案，未被篡改
   ✓ 未检测到硬编码密钥
   ⚠ 申请了 5 个敏感权限（摄像头、位置、录音、通讯录、存储）
   ⚠ 检测到 AdMob 和 Firebase Analytics 广告 SDK
   
   总体安全，权限申请在社交应用合理范围内。"
```

## 场景 2（普通用户）：权限解释

```
用户问题："为什么申请摄像头权限？"

AI 获取数据：
  - ApkInfo.manifest.permissions
  - ApkInfo.manifest.activities

AI 处理：
  1. 搜索包含 Camera 相关的 Activity
  2. 分析可能的用途

AI 回答：
  "摄像头权限通常用于：
   1. 扫一扫 — 扫描二维码
   2. 视频通话 — 实时视频
   3. 拍照发送 — 聊天中发送照片
   
   这个 APK 有 3 个 Activity 可能使用摄像头：
   - .scanner.BaseScanUI（扫码）
   - .voip.VideoActivity（视频通话）
   - .camera.CameraActivity（拍照）
   
   风险等级：低，是社交应用的常见功能。"
```

## 场景 3（普通用户）：广告 SDK

```
用户问题："这个 APK 有广告吗？"

AI 获取数据：
  - ApkInfo.entries（文件列表）
  - SecurityScanResult.issues（TRACKING_SDK 类型）

AI 处理：
  1. 搜索 entries 中 SDK 包名模式
  2. 汇总检测结果

AI 回答：
  "检测到以下广告/追踪 SDK：
   1. Google AdMob — 最常见的广告 SDK
   2. Facebook Audience Network — Facebook 广告
   3. Firebase Analytics — 数据分析
   
   共 3 个第三方 SDK，占用约 18% 的 APK 体积。
   
   这些 SDK 可能收集：设备 ID、使用习惯、位置信息。
   
   [ACTION:security_scanner|查看完整安全报告]"
```

## 场景 4（普通用户）：隐私风险

```
用户问题："这个 APK 会收集我的隐私吗？"

AI 获取数据：
  - ApkInfo.manifest.permissions（全部权限）
  - SecurityScanResult.issues（危险权限）

AI 处理：
  1. 筛选隐私相关权限
  2. 按严重程度排序
  3. 给出隐私风险等级

AI 回答：
  "隐私风险评估：
   
   高风险权限：
   - ACCESS_FINE_LOCATION — 精确位置追踪
   - READ_CONTACTS — 读取通讯录
   - CAMERA — 可拍照
   
   中风险权限：
   - READ_PHONE_STATE — 读取设备信息
   - RECORD_AUDIO — 可录音
   
   低风险权限：
   - INTERNET — 网络访问（正常）
   
   风险等级：中。位置和通讯录权限较为敏感，
   但对于社交应用属于合理范围。"
```

## 场景 5（普通用户）：安装来源

```
用户问题："这是官方版本吗？"

AI 获取数据：
  - ApkInfo.signatures（签名证书信息）

AI 处理：
  1. 提取签名者信息
  2. 检查签名方案版本
  3. 给出判断建议

AI 回答：
  "从签名信息判断：
   
   签名者：CN=WeChat, OU=WeChat Team, O=Tencent
   签名方案：V2 + V3
   SHA-256：A1B2C3D4...
   
   建议验证方法：
   1. 从官方应用商店下载同名 APK
   2. 对比两者的 SHA-256 指纹
   3. 如果一致，则为官方版本
   
   [ACTION:signing|查看完整签名信息]"
```

## 场景 6（开发者）：分析自己的 APK

```
用户问题："我的 APK 配置正确吗？"

AI 获取数据：
  - ApkInfo 全量
  - SecurityScanResult 全量

AI 处理：
  1. 检查 minSdk / targetSdk
  2. 检查签名方案
  3. 检查安全配置
  4. 检查权限合理性

AI 回答：
  "APK 配置检查：
   
   ✓ targetSdk = 35（最新版本）
   ⚠ minSdk = 21（较低，考虑提升到 24+）
   ✓ 签名：V2+V3（推荐方案）
   ⚠ allowBackup=true（建议关闭）
   ⚠ usesCleartextTraffic=true（建议使用 HTTPS）
   ✓ debuggable=false（正确）
   
   建议：
   1. 提升 minSdk 到 24，减少兼容性测试成本
   2. 设置 allowBackup=false
   3. 使用 networkSecurityConfig 限制明文传输"
```

## 场景 7（开发者）：检查密钥泄漏

```
用户问题："有没有硬编码的密钥？"

AI 获取数据：
  - SecurityScanResult.issues（HARDCODED_* 类型）

AI 处理：
  1. 筛选硬编码类问题
  2. 提取位置和描述

AI 回答：
  "安全扫描检测到以下硬编码问题：
   
   CRITICAL:
   - 在 classes.dex 中发现疑似 API Key（apiKey=AbCd...wXyZ）
     位置：classes.dex
     建议：将密钥移至服务端或使用 Android Keystore
   
   HIGH:
   - 在 classes.dex 中发现疑似密码（password=admin123）
     位置：classes.dex
     建议：不要在代码中硬编码密码
   
   请立即修复这些问题。
   
   [ACTION:security_scanner|查看完整安全报告]"
```

## 场景 8（开发者）：查看资源

```
用户问题："应用图标在哪里？"

AI 获取数据：
  - ApkInfo.entries（文件列表）

AI 处理：
  1. 搜索 res/mipmap 和 res/drawable 目录
  2. 识别图标文件

AI 回答：
  "应用图标文件：
   
   - res/mipmap-xxxhdpi/ic_launcher.png (48 KB)
   - res/mipmap-xxhdpi/ic_launcher.png (32 KB)
   - res/mipmap-xhdpi/ic_launcher.png (16 KB)
   - res/drawable/ic_launcher_foreground.xml (2 KB)
   
   如果要修改应用图标：
   1. 替换 res/mipmap-*/ic_launcher.png
   2. 修改 resources.arsc 中的引用
   3. 重新签名 APK
   
   [ACTION:arsc_editor|打开资源编辑器]"
```

## 场景 9（开发者）：对比版本

```
用户问题："新旧版本有什么区别？"

AI 获取数据：
  - 两个 ApkInfo（旧版 + 新版）

AI 处理：
  1. 对比 versionName / versionCode
  2. 对比权限变化
  3. 对比组件变化
  4. 对比文件结构变化

AI 回答：
  "v8.0.18 → v8.0.20 变化：
   
   版本：8.0.18 → 8.0.20
   大小：238 MB → 245 MB (+7 MB)
   DEX：2 → 3（新增 classes3.dex, 1.2 MB）
   
   新增权限：
   - android.permission.NEARBY_WIFI_DEVICES
   
   新增组件：
   - 3 个 Activity（视频通话相关）
   
   移除：无
   
   [ACTION:diff|打开 APK 对比工具]"
```

## 场景 10（高级用户）：定位功能逻辑

```
用户问题："会员验证逻辑在哪里？"

AI 获取数据：
  - DexRepository.listClasses() → 类名列表
  - ApkInfo.manifest.activities / services

AI 处理：
  1. 搜索包含 "vip" / "member" / "premium" / "subscription" 的类
  2. 搜索包含 "validate" / "verify" / "check" 的方法名
  3. 分析可能的逻辑位置

AI 回答：
  "通过分析 DEX 类名，以下位置可能相关：
   
   高可能性：
   1. com.example.app.vip.VipManager
   2. com.example.app.user.UserValidator
   
   中可能性：
   3. com.example.app.network.VipApiService
   4. com.example.app.billing.SubscriptionManager
   
   关键字搜索建议：
   - isVip() / checkVipStatus() / verifySubscription()
   
   推荐查看路径：
   1. 先查看 VipManager 类
   2. 追踪 isVip() 方法的返回值来源
   3. 查看 VipApiService 的网络请求
   
   ⚠ 注意：修改会员验证可能违反应用服务条款。
   AppDex 不会生成破解代码。修改后必须重新签名。
   
   [ACTION:dex_browser|打开 DEX 浏览器]"
```

## 场景 11（高级用户）：查找关键代码

```
用户问题："网络请求用的是什么框架？"

AI 获取数据：
  - DexRepository.listClasses()

AI 处理：
  1. 搜索常见网络框架包名
  2. 统计使用情况

AI 回答：
  "检测到以下网络框架：
   
   - OkHttp (com.squareup.okhttp3) — HTTP 客户端
   - Retrofit (retrofit2) — REST API
   - okhttp-logging-interceptor — 日志拦截器
   
   网络请求通常在：
   - com.example.app.network.ApiClient
   - com.example.app.network.RetrofitFactory
   
   [ACTION:dex_browser|搜索 OkHttpClient]"
```

## 场景 12（高级用户）：分析配置文件

```
用户问题："有没有 Firebase 配置？"

AI 获取数据：
  - ApkInfo.entries（搜索 google-services.json）
  - ApkInfo.manifest.metaData

AI 处理：
  1. 搜索 firebase 相关文件
  2. 搜索 manifest 中的 firebase meta-data

AI 回答：
  "检测到 Firebase 配置：
   
   配置文件：
   - assets/google-services.json
   
   Manifest meta-data：
   - firebase_app_id = 1:123456:android:abcdef
   - google_api_key = AIzaSy...
   
   使用的 Firebase 服务：
   - Firebase Analytics
   - Firebase Cloud Messaging (推送)
   - Firebase Crashlytics (崩溃上报)
   
   [ACTION:file_manager|查看配置文件]"
```

## 场景 13（高级用户）：修改应用名称

```
用户问题："怎么修改应用名称？"

AI 获取数据：
  - ApkInfo.entries（resources.arsc 存在）

AI 处理：
  1. 分析应用名称存储位置
  2. 生成修改步骤

AI 回答：
  "应用名称存储在 resources.arsc 文件中。
   
   修改步骤：
   1. 打开资源编辑器
   2. 搜索 app_name
   3. 修改为新名称
   4. 保存
   5. 重新签名 APK
   
   ⚠ 修改后必须重新签名，否则无法安装。
   
   [ACTION:arsc_editor|打开资源编辑器]"
```

## 场景 14（高级用户）：修改权限

```
用户问题："能不能去掉某个权限？"

AI 获取数据：
  - ApkInfo.manifest.permissions

AI 处理：
  1. 列出所有权限
  2. 评估移除风险

AI 回答：
  "当前 APK 申请了 32 个权限。
   
   可以移除的权限（低风险）：
   - android.permission.NFC — 如果应用不使用 NFC
   - android.permission.BLUETOOTH — 如果应用不使用蓝牙
   
   不建议移除的权限：
   - android.permission.INTERNET — 大部分功能依赖网络
   
   修改方法：
   1. 打开清单编辑器
   2. 删除对应的 <uses-permission> 标签
   3. 重新打包
   4. 重新签名
   
   [ACTION:axml_editor|打开清单编辑器]"
```

## 场景 15（高级用户）：分析 so 库

```
用户问题："这个 APK 用了哪些原生库？"

AI 获取数据：
  - ApkInfo.entries（筛选 .so 文件）

AI 处理：
  1. 列出所有 .so 文件
  2. 按架构分组
  3. 分析可能的用途

AI 回回答：
  "检测到 15 个原生库（.so 文件）：
   
   arm64-v8a:
   - libnative.so (8.2 MB) — 应用核心逻辑
   - libcrypto.so (2.1 MB) — 加密库
   - libssl.so (1.5 MB) — SSL/TLS
   
   armeabi-v7a:
   - 同上（重复，可移除节省 60MB）
   
   x86_64:
   - 同上（模拟器兼容）
   
   分析：
   - libnative.so 可能包含核心业务逻辑
   - 体积优化：只保留 arm64-v8a 可节省约 60 MB
   
   [ACTION:elf_viewer|分析 libnative.so]
   [ACTION:size_analyzer|查看体积分析]"
```

## 场景 16（普通用户）：应用大小分析

```
用户问题："为什么 APK 这么大？"

AI 获取数据：
  - ApkInfo.entries（按大小排序）
  - ApkInfo.fileSize

AI 处理：
  1. 统计各类文件占比
  2. 识别大文件

AI 回答：
  "APK 总大小 245 MB，各部分占比：
   
   原生库 (.so) — 156 MB (64%)
   ├── 4 套 CPU 架构（可只保留 arm64-v8a）
   └── 建议优化，可节省 80 MB
   
   资源文件 — 52 MB (21%)
   ├── 大量 PNG 图片
   └── 28 种语言包
   
   DEX 代码 — 28 MB (11%)
   └── 3 个 DEX 文件
   
   优化建议：
   1. 移除冗余 CPU 架构 → 节省 80 MB
   2. 删除不需要的语言包 → 节省 15 MB
   3. 压缩 PNG → 节省 10 MB
   
   [ACTION:size_analyzer|查看详细体积分析]"
```

## 场景 17（开发者）：检查导出组件

```
用户问题："有没有安全隐患的导出组件？"

AI 获取数据：
  - ApkInfo.manifest.activities / services / receivers / providers
  - SecurityScanResult.issues（EXPORTED_COMPONENT）

AI 处理：
  1. 检查 exported=true 的组件
  2. 检查是否有权限保护

AI 回答：
  "检测到 3 个导出组件未设置权限保护：
   
   1. .MainActivity — exported=true, 无 permission
      风险：可被第三方应用直接启动
      建议：添加 android:permission 或设 exported=false
   
   2. .DeepLinkActivity — exported=true
      用途：深链接处理（可能正常）
   
   3. .NotificationReceiver — exported=true
      风险：可被第三方应用发送伪造广播
      建议：设置签名级权限
   
   [ACTION:axml_editor|打开清单编辑器]"
```

## 场景 18（开发者）：签名验证

```
用户问题："签名信息正常吗？"

AI 获取数据：
  - ApkInfo.signatures

AI 处理：
  1. 检查签名方案版本
  2. 检查算法强度
  3. 检查证书有效期

AI 回答：
  "签名分析：
   
   签名方案：V2 + V3 ✓
   算法：SHA256withRSA ✓
   有效期：2020-01-01 至 2049-12-31 ✓
   
   证书信息：
   - 主体：CN=WeChat, OU=WeChat Team, O=Tencent
   - 颁发者：CN=WeChat（自签名）
   - 序列号：A1B2C3...
   
   指纹：
   - SHA-256：A1B2C3D4E5F6...
   - SHA-1：  F1E2D3C4B5A6...
   - MD5：    1122334455...
   
   签名状态：有效，未被篡改。
   
   [ACTION:signing|查看完整签名工具]"
```

## 场景 19（高级用户）：数据库分析

```
用户问题："这个 APK 有没有用 SQLite 数据库？"

AI 获取数据：
  - ApkInfo.entries（搜索 .db 文件）

AI 处理：
  1. 搜索 .db 文件
  2. 搜索 SQLite 相关代码

AI 回答：
  "检测到 SQLite 数据库：
   
   预置数据库：
   - assets/databases/app.db (2.3 MB)
   
   运行时数据库路径（推测）：
   - /data/data/com.example.app/databases/
   
   DEX 中的 SQLite 使用：
   - SQLiteOpenHelper → 有数据库管理代码
   - SQLiteQueryBuilder → 有查询逻辑
   
   [ACTION:sqlite_viewer|查看数据库内容]"
```

## 场景 20（高级用户）：综合安全审计

```
用户问题："做一次完整的安全审计"

AI 获取数据：
  - ApkInfo 全量
  - SecurityScanResult 全量
  - DexRepository.listClasses()

AI 处理：
  1. 执行完整安全扫描
  2. 分析权限
  3. 分析组件
  4. 分析签名
  5. 检测硬编码
  6. 检测追踪 SDK
  7. 检测弱加密

AI 回答：
  "完整安全审计报告：
   
   ── 安全评分：85/100 ──
   
   CRITICAL (0)：无
   
   HIGH (0)：无
   
   MEDIUM (4)：
   1. 危险权限：CAMERA
   2. 危险权限：ACCESS_FINE_LOCATION
   3. 弱加密：AES/ECB（在 classes.dex 中）
   4. allowBackup=true
   
   LOW (3)：
   1. 3 个 exported Activity 未设权限保护
   2. minSdk=21（较低）
   3. 使用 java.util.Random
   
   INFO (3)：
   1. AdMob SDK
   2. Firebase Analytics
   3. Tencent Bugly
   
   总体评价：应用安全状况良好。
   建议修复弱加密和导出组件问题。
   
   [ACTION:security_scanner|查看完整报告]"
```

---

# 第四部分：APK Context Schema

## 4.1 数据结构定义

### JSON Schema（用于 AI 上下文）

```json
{
  "apkInfo": {
    "fileName": "wechat.apk",
    "fileSize": 245000000,
    "fileSizeHuman": "245 MB",
    "totalEntries": 1243
  },
  "manifest": {
    "packageName": "com.tencent.mm",
    "versionName": "8.0.20",
    "versionCode": 2400,
    "minSdk": 23,
    "targetSdk": 35,
    "permissions": {
      "total": 32,
      "dangerous": [
        "android.permission.CAMERA",
        "android.permission.ACCESS_FINE_LOCATION",
        "android.permission.RECORD_AUDIO",
        "android.permission.READ_CONTACTS",
        "android.permission.WRITE_EXTERNAL_STORAGE"
      ],
      "normal": [
        "android.permission.INTERNET",
        "android.permission.ACCESS_NETWORK_STATE"
      ]
    },
    "components": {
      "activities": 48,
      "services": 12,
      "receivers": 8,
      "providers": 3
    },
    "metaData": {}
  },
  "signatures": [
    {
      "version": 2,
      "algorithm": "SHA256withRSA",
      "subject": "CN=WeChat, OU=WeChat Team, O=Tencent",
      "validFrom": "2020-01-01",
      "validTo": "2049-12-31",
      "sha256": "A1B2C3..."
    }
  ],
  "dexInfo": {
    "count": 3,
    "files": [
      {"name": "classes.dex", "size": 2300000},
      {"name": "classes2.dex", "size": 1800000},
      {"name": "classes3.dex", "size": 1200000}
    ],
    "topClasses": [
      "com.tencent.mm.MainActivity",
      "com.tencent.mm.app.WeApplication",
      "..."
    ]
  },
  "securityFindings": {
    "score": 85,
    "critical": 0,
    "high": 0,
    "medium": 4,
    "low": 3,
    "info": 3,
    "issues": [
      {
        "severity": "MEDIUM",
        "title": "危险权限: CAMERA",
        "location": "AndroidManifest.xml",
        "recommendation": "评估是否真正需要此权限"
      }
    ]
  },
  "fileStructure": {
    "soLibraries": 15,
    "nativeLibSize": 156000000,
    "resourceFiles": 856,
    "resourceSize": 52000000,
    "largestFiles": [
      {"name": "lib/arm64-v8a/libnative.so", "size": 8200000},
      {"name": "classes.dex", "size": 2300000}
    ]
  },
  "trackingSdks": [
    {"name": "Google AdMob", "package": "com.google.ads"},
    {"name": "Firebase Analytics", "package": "com.google.firebase.analytics"}
  ]
}
```

## 4.2 上下文构建策略

### 分级构建

| 级别 | 触发条件 | 包含字段 | Token 估算 |
|---|---|---|---|
| Minimal | 安全总结 / 一句话问答 | apkInfo + manifest(摘要) + signatures(状态) + securityFindings(score) | ~150 |
| Standard | 通用问答 | Minimal + permissions(全部) + components(数量) + trackingSdks | ~400 |
| Detailed | 代码定位 / 修改指导 | Standard + dexInfo(topClasses 200) + fileStructure | ~1200 |
| Full | 深度审计 | Detailed + securityFindings(全部 issues) + fileStructure(largestFiles) | ~2000 |

### Token 控制策略

| 策略 | 规则 |
|---|---|
| 类名截断 | DEX 类列表最多 200 个，超出显示 "...还有 N 个类" |
| 权限分组 | 危险权限单独列出，普通权限只显示数量 |
| 文件列表 | 只显示前 20 个大文件，按大小排序 |
| 签名简化 | 只发送版本+算法+主体+SHA-256，不发送完整证书链 |
| 安全发现 | 按严重程度分组，INFO 级别只显示数量 |

### 隐私控制

| 数据 | 是否发送 | 理由 |
|---|---|---|
| 签名 SHA-256 | ✅ 发送 | 公开信息，不含私钥 |
| 签名证书主体 | ✅ 发送 | 公开信息 |
| API Key 值 | ❌ 不发送 | 只发送"检测到硬编码 API Key"，不发送实际值 |
| 密码值 | ❌ 不发送 | 同上 |
| DEX 字符串 | ❌ 不发送 | 可能包含敏感信息 |
| 包名 | ✅ 发送 | 公开信息 |
| 权限列表 | ✅ 发送 | 公开信息 |

---

# 第五部分：AI Chat UI 设计

## 5.1 页面定位

不是普通聊天软件。是结合 APK 任务上下文的智能助手。

## 5.2 布局结构

```
┌──────────────────────────────────────────┐
│ ← AI 助手                          [⚙]  │  ← 顶部栏
├──────────────────────────────────────────┤
│ 当前 APK                                 │  ← APK 上下文卡
│ ┌──────────────────────────────────────┐│
│ │ [icon] 微信 8.0.20                    ││
│ │ 85/100 · 低风险 · 已分析              ││
│ │                              [切换→]  ││  ← 可从最近任务切换 APK
│ └──────────────────────────────────────┘│
├──────────────────────────────────────────┤
│                                          │
│  ┌──────────────────────────────────────┐│  ← AI 消息气泡
│ │ 🤖 你好！我已经读取了这个 APK 的分析  ││
│ │    数据。有什么想了解的？             ││
│ │                                      ││
│ │    ┌──────────────────────────────┐  ││  ← Action Card
│ │    │ 📊 查看完整安全报告       →  │  ││
│ │    └──────────────────────────────┘  ││
│ └──────────────────────────────────────┘│
│                                          │
│                    ┌───────────────────┐ │  ← 用户消息气泡
│                    │ 这个 APK 安全吗？│ │
│                    └───────────────────┘ │
│                                          │
│  ┌──────────────────────────────────────┐│  ← AI 流式回复
│ │ 🤖 这个 APK 的安全评分为 85/100...    ││
│ │ ██████████████░░░░ (正在生成)        ││  ← 打字指示器
│ └──────────────────────────────────────┘│
│                                          │
├──────────────────────────────────────────┤
│ 快捷问题：                               │  ← 快捷问题栏
│ [安全吗？] [权限] [SDK] [大小] [签名]    │
├──────────────────────────────────────────┤
│ ┌──────────────────────────────────┐ [→] │  ← 输入栏
│ │ 输入问题…                       │     │
│ └──────────────────────────────────┘     │
└──────────────────────────────────────────┘
```

## 5.3 组件设计

### 5.3.1 APK 上下文卡

| 属性 | 值 |
|---|---|
| 目的 | 让用户和 AI 都知道当前在分析哪个 APK |
| 组件 | `AppDexCard` + `AppDexRow` |
| 数据来源 | `ApkSessionManager.session` |
| 交互 | 点击"切换"→ 最近任务列表 |

显示内容：
- 应用图标 + 应用名 + 版本
- 安全评分 + 风险等级 + 分析状态
- 右侧"切换"按钮

### 5.3.2 消息气泡

| 类型 | 对齐 | 背景 | 头像 |
|---|---|---|---|
| AI 消息 | 左对齐 | `SurfaceAlt` + `BorderLight` | 🤖 (24dp) |
| 用户消息 | 右对齐 | `AmberGoldContainer` | 无 |

气泡内文字：12sp `TextPrimary`，行距 1.5

### 5.3.3 Action Card（消息内嵌工具跳转）

| 属性 | 值 |
|---|---|
| 目的 | AI 建议跳转到某个工具，用户点击后跳转 |
| 组件 | `AppDexCard` + `AppDexRow` |
| 交互 | 点击 → NavController.navigate(route) |

显示格式：
```
┌──────────────────────────────────────┐
│ [icon] 打开资源编辑器             →  │
│        修改 resources.arsc           │
└──────────────────────────────────────┘
```

颜色：`AmberGold` 边框 + `AmberGoldContainer` 背景

### 5.3.4 快捷问题栏

| 属性 | 值 |
|---|---|
| 目的 | 降低用户提问门槛 |
| 组件 | 水平滚动的 Chip 列表 |
| 交互 | 点击 → 自动发送该问题 |

快捷问题列表（动态）：
- 未分析时：["如何选择 APK？"、"AI 能做什么？"]
- 分析完成：["这个 APK 安全吗？"、"有哪些敏感权限？"、"用了哪些 SDK？"、"APK 太大怎么办？"]
- 有签名时：["签名有效吗？"、"是官方版本吗？"]

### 5.3.5 输入栏

| 属性 | 值 |
|---|---|
| 组件 | `AppDexSearchBar`（改造为输入模式）+ 发送按钮 |
| 交互 | 输入文字 → 点击发送/回车 → 发送消息 |

## 5.4 状态管理

```
AiChatState(
    messages: List<ChatMessage>,           // 对话历史
    currentApkInfo: ApkInfo?,              // 当前 APK
    currentSecurityScore: Int?,            // 安全评分
    isLoading: Boolean,                   // AI 正在回复
    isConfigured: Boolean,                 // AI 是否已配置
    error: String?,                        // 错误信息
    streamingContent: String?,             // 流式回复内容
)

sealed interface ChatMessage {
    data class User(val text: String) : ChatMessage
    data class Ai(val text: String, val actions: List<ToolAction>) : ChatMessage
    data class AiStreaming(val partialText: String) : ChatMessage
}

data class ToolAction(
    val toolId: String,        // "arsc_editor"
    val label: String,         // "打开资源编辑器"
    val description: String,   // "修改 resources.arsc"
    val route: Route           // 跳转目标
)
```

## 5.5 未配置 AI 时的 UI

```
┌──────────────────────────────────────────┐
│ ← AI 助手                          [⚙]  │
├──────────────────────────────────────────┤
│                                          │
│         [AI 图标 48dp]                   │
│                                          │
│      AI 助手未配置                       │
│      配置 AI 服务后，可以：              │
│      · 自动生成安全分析报告              │
│      · 用通俗语言解释权限和风险          │
│      · 指导修改 APK 的步骤              │
│      · 定位代码逻辑位置                  │
│                                          │
│      ┌──────────────────────────────┐    │
│      │    ⚙ 配置 AI 服务            │    │
│      └──────────────────────────────┘    │
│                                          │
│      支持 OpenAI / Anthropic / 本地模型  │
│      所有数据仅发送给你配置的 AI 服务    │
│      AppDex 不收集任何数据                │
│                                          │
└──────────────────────────────────────────┘
```

---

# 第六部分：AI 和工具联动设计

## 6.1 Action Card 机制

AI 回复中可以包含 `[ACTION:tool_id|label]` 标记。UI 层解析后渲染为可点击卡片。

### Tool Bridge 映射表

| tool_id | 显示名 | 跳转路由 | 参数 |
|---|---|---|---|
| `arsc_editor` | 打开资源编辑器 | `Route.ArscViewer` | `apkPath` |
| `axml_editor` | 打开清单编辑器 | `Route.AxmlEditor` | `apkPath` |
| `dex_browser` | 打开 DEX 浏览器 | `Route.DexBrowser` | `apkPath` |
| `hex_editor` | 打开 HEX 编辑器 | `Route.HexEditor` | `filePath` |
| `elf_viewer` | 打开 ELF 查看器 | `Route.ElfViewer` | `filePath` |
| `sqlite_viewer` | 打开 SQLite 查看器 | `Route.SqliteViewer` | `dbPath` |
| `signing` | 进入签名流程 | `Route.ApkSigning` | `apkPath` |
| `repack` | 进入重打包流程 | `Route.ApkRepack` | `apkPath` |
| `security_scanner` | 查看安全报告 | `Route.ApkSecurity` | `apkPath` |
| `size_analyzer` | 查看体积分析 | `Route.ApkSizeAnalyzer` | `apkPath` |
| `diff` | 打开 APK 对比 | `Route.ApkDiff` | — |
| `file_manager` | 打开文件管理器 | `Route.Files` | — |
| `terminal` | 打开终端 | `Route.Terminal` | — |
| `settings_ai` | 配置 AI 服务 | `Route.Settings` | (滚动到 AI 分区) |

## 6.2 联动场景

### 场景 A：修改应用名称

```
用户："我想修改应用名称"

AI 回复：
  "应用名称存储在 resources.arsc 中。
   修改步骤：
   1. 打开资源编辑器
   2. 搜索 app_name
   3. 修改为新名称
   4. 保存
   5. 重新签名
   
   [ACTION:arsc_editor|打开资源编辑器]"

→ 用户点击 → 跳转 ArscEditorScreen(apkPath)
```

### 场景 B：重新签名

```
用户："修改完了，怎么签名？"

AI 回复：
  "你的 APK 已修改，需要重新签名才能安装。
   AppDex 内置签名工具，支持 V2+V3 签名方案。
   
   [ACTION:signing|进入签名流程]"

→ 用户点击 → 跳转 SigningScreen(apkPath)
```

### 场景 C：查看代码

```
用户："VipManager 在哪里？"

AI 回复：
  "在 DEX 中找到：
   com.example.app.vip.VipManager
   
   [ACTION:dex_browser|打开 DEX 浏览器]"

→ 用户点击 → 跳转 DexBrowserScreen(apkPath)
   → 自动搜索 "VipManager"
```

### 场景 D：安全检查

```
用户："帮我做安全扫描"

AI 回复：
  "正在分析... 已完成。
   检测到 4 个中风险问题。
   
   [ACTION:security_scanner|查看完整安全报告]"

→ 用户点击 → 跳转 SecurityScannerScreen(apkPath)
```

### 场景 E：多步流程引导

```
用户："我想去掉广告 SDK"

AI 回复：
  "去除广告 SDK 是一个多步操作：
   
   步骤 1：定位广告 SDK 文件
   [ACTION:dex_browser|搜索 AdMob 类]
   
   步骤 2：从 Manifest 中移除相关组件
   [ACTION:axml_editor|打开清单编辑器]
   
   步骤 3：重新打包
   [ACTION:repack|进入重打包]
   
   步骤 4：重新签名
   [ACTION:signing|进入签名流程]
   
   ⚠ 去除广告 SDK 可能导致应用崩溃。
   建议先备份原始 APK。"

→ 用户依次点击每个 Action Card 完成流程
```

## 6.3 Action Card 渲染规则

1. AI 回复末尾的 `[ACTION:...]` 标记被解析为 `ToolAction` 列表
2. 每个 `ToolAction` 渲染为一个可点击卡片
3. 卡片显示：图标 + 标题 + 描述 + 右箭头
4. 点击后调用 `navController.navigate(route)`
5. 如果需要参数（如 apkPath），从 `ApkSessionManager` 获取
6. 如果 Session 状态为 Idle，显示提示"请先选择 APK"

---

# 第七部分：数据安全设计

## 7.1 API Key 存储

| 维度 | 方案 |
|---|---|
| 存储位置 | Android DataStore（非明文 SharedPreferences） |
| 存储方式 | 原文存储在 DataStore，但 UI 显示为 `••••••••` |
| 输入控件 | `TextField` 的 `visualTransformation = PasswordVisualTransformation()` |
| 传输 | 仅通过 HTTPS 发送给用户配置的 AI 服务 |
| 本地不加密 | DataStore 在应用私有目录，root 设备可读取（已知限制） |

### API Key 输入 UI

```
┌──────────────────────────────────────────┐
│ API Key                                  │
│ ┌──────────────────────────────────────┐ │
│ │ ••••••••••••••••        [👁 显示]   │ │
│ └──────────────────────────────────────┘ │
│ 你的 API Key 仅存储在本地，不会上传到    │
│ AppDex 服务器（AppDex 无服务器）。        │
└──────────────────────────────────────────┘
```

## 7.2 发送给 AI 的数据选择

### 数据发送策略

| 数据 | 发送 | 理由 |
|---|---|---|
| 包名 | ✅ | 公开信息，AI 需要用于上下文 |
| 版本号 | ✅ | 公开信息 |
| 权限列表 | ✅ | 公开信息，AI 分析必需 |
| 组件列表 | ✅ | 公开信息 |
| 签名方案+算法 | ✅ | 公开信息 |
| 签名 SHA-256 | ✅ | 公开指纹 |
| 证书主体 | ✅ | 公开信息 |
| DEX 类名 | ✅ | 包名是公开的 |
| 安全评分 | ✅ | 本地计算结果 |
| 安全问题列表 | ✅ | 本地扫描结果（不含实际密钥值） |
| 文件结构 | ✅ | 文件名和大小是公开的 |
| **硬编码密钥的实际值** | ❌ | 敏感信息，只发送"检测到 API Key" |
| **DEX 字符串内容** | ❌ | 可能包含敏感数据 |
| **文件内容** | ❌ | 不发送文件内容 |

### 隐私提示

在 AI Chat 页面底部显示：

```
⚠ 发送给 AI 的数据包含 APK 的公开信息（包名、权限、签名等）。
  不包含你的 API Key（仅用于认证）、不包含文件内容。
  数据仅发送到你配置的 AI 服务，AppDex 不做中转。
```

## 7.3 网络安全

| 维度 | 方案 |
|---|---|
| HTTP 强制 | AI 请求默认使用 HTTPS；用户自定义 URL 允许 HTTP（本地模型） |
| 超时 | 连接 15s，读取 30s |
| 重试 | 失败自动重试 1 次 |
| SSL | 信任系统证书库；自定义 URL 时不跳过证书验证 |
| 日志 | Release 版本不记录请求/响应内容 |
| 错误处理 | 网络错误 → "网络连接失败，请检查网络和 API 地址"；认证错误 → "API Key 无效" |

## 7.4 本地数据保护

| 维度 | 方案 |
|---|---|
| APK 临时文件 | 分析后 APK 副本存储在 `cacheDir/temp_analysis.apk`，App 退出时清理 |
| 对话历史 | 对话历史不持久化（仅内存），退出 App 后清除 |
| AI 配置 | DataStore 存储，在应用私有目录 `/data/data/com.appdex/files/datastore/` |
| Room 数据库 | 最近任务表不存储敏感信息（不存 API Key、不存文件内容） |

---

# 第八部分：禁止设计

## 8.1 明确禁止的能力

| 禁止项 | 理由 |
|---|---|
| 一键破解会员 | 违反应用服务条款 |
| 自动绕过付费 | 违反应用服务条款 |
| 自动生成破解 APK | AppDex 是分析工具，不是破解工具 |
| 自动移除广告 SDK 代码 | 可能导致应用崩溃，且涉及版权问题 |
| 自动修改签名验证逻辑 | 违反应用完整性 |
| 生成注入代码 | AppDex 不生成代码注入 |

## 8.2 AI 遇到敏感请求时的行为

### 场景：用户要求破解

```
用户："帮我破解这个 APK 的会员功能"

AI 回答：
  "AppDex 是 APK 分析和开发辅助工具，不能用于：
   - 破解应用付费功能
   - 绕过验证逻辑
   - 修改应用内购
   
   我可以帮你：
   - 分析会员验证逻辑的位置（定位，不修改）
   - 理解应用的架构和代码结构
   - 检查你自己开发的 APK 的安全性
   
   如果你是应用开发者，想调试自己的会员验证逻辑：
   1. 在 DEX 浏览器中搜索 VipManager
   2. 查看 isVip() 方法的实现
   3. 分析网络验证请求
   
   [ACTION:dex_browser|打开 DEX 浏览器]"
```

### 场景：用户要求修改付费功能

```
用户："怎么把 isVip 改成永远返回 true"

AI 回答：
  "我理解你想了解会员验证的实现方式。
   
   我可以告诉你：
   - isVip() 方法在 com.example.app.vip.VipManager 中
   - 返回值来自本地缓存和网络验证
   
   我不能帮你：
   - 生成修改后的 smali 代码
   - 自动替换方法返回值
   
   如果你是开发者，想测试自己的验证逻辑：
   1. 在 DEX 浏览器中查看 VipManager
   2. 理解验证流程后自行修改
   3. 修改后必须重新签名
   
   ⚠ 修改他人 APK 的付费功能可能违反法律。
   
   [ACTION:dex_browser|查看 VipManager 类]"
```

## 8.3 安全原则

1. **解释原理**：AI 可以解释技术原理（如签名验证如何工作）
2. **定位位置**：AI 可以告诉用户代码在哪个文件、哪个类
3. **提供合法建议**：AI 可以建议开发者如何改善自己的代码安全
4. **要求用户自行确认**：所有修改操作必须用户手动执行
5. **不生成恶意代码**：不生成任何绕过验证、破解、注入的代码

---

# 第九部分：开发 Sprint 计划

## Sprint AI-0：AI Provider 基础设施

| 属性 | 值 |
|---|---|
| **目标** | 实现 AiProvider + AiConfig + AiClient 基础设施 |
| **预估** | 1 天 |
| **新增模块** | `core-ai` |
| **新增文件** | 7 个 |

### 新增文件

| 文件 | 内容 |
|---|---|
| `core/core-ai/build.gradle.kts` | 模块配置，依赖 okhttp |
| `core/core-ai/.../provider/AiProvider.kt` | sealed interface: OpenAI/Anthropic/Local/Custom |
| `core/core-ai/.../config/AiConfig.kt` | data class + AiConfigRepository (DataStore) |
| `core/core-ai/.../client/AiClient.kt` | OkHttp HTTP 调用 + SSE 流式解析 |
| `core/core-ai/.../client/ChatMessage.kt` | ChatMessage data class + ChatRole enum |
| `core/core-ai/.../prompt/PromptEngine.kt` | 系统提示词 + Token 估算 |
| `core/core-ai/.../context/ContextBuilder.kt` | ApkInfo → 上下文文本（Minimal/Standard 分级） |

### 修改文件

| 文件 | 修改 |
|---|---|
| `settings.gradle.kts` | +include(":core:core-ai") |
| `app/build.gradle.kts` | +implementation(project(":core:core-ai")) |
| `gradle/libs.versions.toml` | +okhttp 版本 |

### 验收标准

- [ ] 编译通过
- [ ] App 正常启动
- [ ] 原有功能不受影响
- [ ] AiClient 可构造请求（不需要实际调用）

---

## Sprint AI-1：Context Builder + Prompt System

| 属性 | 值 |
|---|---|
| **目标** | 实现完整的上下文构建和 Prompt 模板系统 |
| **预估** | 0.5 天 |
| **新增文件** | 3 个 |

### 新增文件

| 文件 | 内容 |
|---|---|
| `core/core-ai/.../prompt/PromptTemplates.kt` | 6 个场景模板（安全总结/权限解释/修改指导/代码定位/SDK识别/签名验证） |
| `core/core-ai/.../context/ApkContextSchema.kt` | JSON Schema 数据类 |
| `core/core-ai/.../AiService.kt` | 高层 API: ask() / generateSummary() / testConnection() |

### 验收标准

- [ ] 编译通过
- [ ] ContextBuilder 可从 ApkInfo 生成上下文文本
- [ ] PromptEngine 可生成系统提示词
- [ ] AiService.testConnection() 可发送测试请求

---

## Sprint AI-2：AI 配置 UI

| 属性 | 值 |
|---|---|
| **目标** | 设置页新增 AI 服务配置分区 |
| **预估** | 0.5 天 |
| **修改文件** | 3 个 |
| **新增文件** | 1 个 |

### 文件

| 文件 | 操作 | 内容 |
|---|---|---|
| `feature/feature-settings/.../SettingsScreen.kt` | 修改 | 新增 "AI 服务" 分区 |
| `feature/feature-settings/.../SettingsViewModel.kt` | 修改 | 新增 AI 配置读写 |
| `feature/feature-settings/build.gradle.kts` | 修改 | +core-ai 依赖 |
| `core/core-ui/.../components/AiConfigComponents.kt` | 新增 | Provider 选择器 + 输入框 + 测试按钮 |

### 验收标准

- [ ] 编译通过
- [ ] 设置页显示 AI 服务分区
- [ ] Provider 可选择 OpenAI/Anthropic/Local/Custom
- [ ] API Key 密码模式显示
- [ ] 测试连接有反馈
- [ ] 配置退出后保持

---

## Sprint AI-3：AI Chat UI

| 属性 | 值 |
|---|---|
| **目标** | 实现 AI 对话界面 |
| **预估** | 1 天 |
| **新增模块** | `feature-ai-assistant` |
| **新增文件** | 6 个 |
| **修改文件** | 2 个 |

### 新增文件

| 文件 | 内容 |
|---|---|
| `feature/feature-ai-assistant/build.gradle.kts` | 模块配置 |
| `feature/feature-ai-assistant/.../AiChatScreen.kt` | 对话 UI |
| `feature/feature-ai-assistant/.../AiChatViewModel.kt` | MVI ViewModel |
| `feature/feature-ai-assistant/.../AiChatIntent.kt` | Intent |
| `feature/feature-ai-assistant/.../AiChatState.kt` | State |
| `feature/feature-ai-assistant/.../AiChatEffect.kt` | Effect |

### 修改文件

| 文件 | 修改 |
|---|---|
| `app/.../ui/Route.kt` | +Route.AiChat |
| `app/.../nav/AppDexNavHost.kt` | +AiChat composable |

### 验收标准

- [ ] 编译通过
- [ ] 任务页 AI 按钮可打开 Chat
- [ ] 未配置时显示引导
- [ ] 已配置时可发送消息
- [ ] AI 回复以气泡显示
- [ ] 流式回复有打字指示
- [ ] 快捷问题可点击
- [ ] 可关闭返回

---

## Sprint AI-4：Tool Bridge

| 属性 | 值 |
|---|---|
| **目标** | AI 回复中的 Action Card 可跳转到对应工具 |
| **预估** | 0.5 天 |
| **新增文件** | 2 个 |
| **修改文件** | 1 个 |

### 新增文件

| 文件 | 内容 |
|---|---|
| `core/core-ai/.../bridge/ToolBridge.kt` | 解析 [ACTION:...] 标记 → ToolAction 列表 |
| `core/core-ai/.../bridge/ToolAction.kt` | data class: toolId, label, description, route |

### 修改文件

| 文件 | 修改 |
|---|---|
| `feature/feature-ai-assistant/.../AiChatScreen.kt` | 消息气泡中渲染 Action Card |

### 验收标准

- [ ] AI 回复中的 [ACTION:...] 标记被解析
- [ ] Action Card 正确渲染
- [ ] 点击可跳转到对应工具页面
- [ ] 跳转时传递 apkPath 参数
- [ ] 返回后回到 Chat 页面

---

## Sprint AI-5：集成测试

| 属性 | 值 |
|---|---|
| **目标** | 全流程真实 AI 调用测试 |
| **预估** | 0.5 天 |
| **修改文件** | 无（仅验证） |

### 验收标准

- [ ] 配置 OpenAI API → 测试连接成功
- [ ] 配置 Anthropic API → 测试连接成功
- [ ] 分析 APK 后 → AI 可生成安全总结
- [ ] 问 "安全吗" → AI 回复评分和风险
- [ ] 问 "权限" → AI 解释权限
- [ ] 问 "修改应用名" → AI 返回 Action Card → 可跳转
- [ ] 问 "会员验证" → AI 返回定位结果 + 跳转 DEX
- [ ] 流式回复正常
- [ ] 未配置时显示引导
- [ ] 网络错误有提示
- [ ] API Key 错误有提示

---

## Sprint 依赖关系

```
Sprint AI-0 (Provider 基础)
    ↓
Sprint AI-1 (Context + Prompt)
    ↓
Sprint AI-2 (配置 UI)     ← 可与 AI-1 并行
    ↓
Sprint AI-3 (Chat UI)     ← 依赖 AI-1 + AI-2
    ↓
Sprint AI-4 (Tool Bridge) ← 依赖 AI-3
    ↓
Sprint AI-5 (集成测试)
```

---

## 文件变更总览

| 类别 | 文件数 | 说明 |
|---|---|---|
| 新增 core-ai | ~12 | Provider/Config/Client/Prompt/Context/Bridge/Service |
| 新增 feature-ai-assistant | ~6 | ChatScreen/ViewModel/Intent/State/Effect |
| 新增 UI 组件 | ~2 | AiConfigComponents/AiChatComponents |
| 修改设置页 | ~3 | SettingsScreen/ViewModel/build.gradle |
| 修改导航层 | ~2 | Route.kt/AppDexNavHost.kt |
| 修改 Gradle | ~3 | settings.gradle/app.build.gradle/libs.versions.toml |
| **总计** | **~28** | |

---

*本文档为 AppDex 2.0 AI Assistant System 完整设计。可直接按 Sprint AI-0 至 AI-5 顺序开始执行。*
