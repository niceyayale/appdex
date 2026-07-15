# AppDex 2.0 产品设计文档

> Product Architect Mode — 完整产品设计 + 架构方案 + 迁移策略
>
> 定位：本地 APK 分析助手 + AI 辅助逆向工具

---

# 第一部分：产品信息架构

## 1.1 产品重新定位

| 维度 | 当前 | 新版 |
|---|---|---|
| 产品名 | AppDex — 多功能 Android 逆向工程工具箱 | AppDex — APK 智能分析助手 |
| 第一印象 | 工具列表 | "选择 APK，我来帮你分析" |
| 核心流程 | 用户自己找工具 | 任务驱动，AI 辅助 |
| 技术暴露 | 全部暴露 | 渐进式暴露（普通模式 → 高级模式） |
| 用户画像 | 逆向工程师 | 普通用户 + 专业用户 |

## 1.2 新首页结构

用户打开 App 后 3 秒内知道："我要分析 APK"

### 区域 A：顶部栏

| 属性 | 值 |
|---|---|
| 目的 | 品牌识别 + 状态上下文 |
| 组件 | `AppDexBar(title = "AppDex")` |
| 交互 | 无（纯展示） |
| 数据来源 | 静态 |

**布局**：
```
┌──────────────────────────────────────────┐
│  APPDEX                            [AI]  │
│  AppDex                                   │
└──────────────────────────────────────────┘
```
- 左上角品牌标识 `APPDEX`（AmberGold，9sp，等宽字体）
- 大标题 `AppDex`（18sp，SemiBold）
- 右上角 AI 助手图标按钮（跳转 AI 配置或展开 AI 面板）

### 区域 B：欢迎卡片（Hero Card）

| 属性 | 值 |
|---|---|
| 目的 | 让用户立即知道核心动作 |
| 组件 | `AppDexCard` + `AppDexPrimaryAction` |
| 交互 | 点击"选择 APK"→ 弹出文件选择器 |
| 数据来源 | 静态文案 + 最近 APK 路径（可选） |

**布局**：
```
┌──────────────────────────────────────────┐
│                                          │
│    你好，我可以帮你分析 APK              │
│    选择一个 APK 文件，我会自动完成       │
│    安全分析、权限检查和风险评估。         │
│                                          │
│    ┌────────────────────────────────┐   │
│    │     ⊕ 选择 APK 文件            │   │
│    └────────────────────────────────┘   │
│                                          │
│    上次分析：微信.apk · 2分钟前  →       │
│                                          │
└──────────────────────────────────────────┘
```
- 背景：`HeroGradientStart` → `HeroGradientEnd` 渐变
- 边框：`BorderAccent`
- 主按钮：`AmberGold` 背景，56dp 高，圆角 12dp
- "上次分析"行：如果存在最近任务则显示，点击恢复会话；无则不显示

### 区域 C：最近任务列表

| 属性 | 值 |
|---|---|
| 目的 | 快速恢复之前的分析任务 |
| 组件 | `AppDexSection(label = "最近任务")` + `AppDexRow` 列表 |
| 交互 | 点击列表项 → 恢复该 APK 的会话 → 跳转任务页 |
| 数据来源 | `RecentTaskRepository`（Room 持久化，最多 10 条） |

**布局**：
```
最近任务
┌──────────────────────────────────────────┐
│ [icon] 微信                              │
│        安全评分 85 · 低风险 · 2分钟前  → │
├──────────────────────────────────────────┤
│ [icon] Telegram                          │
│        安全评分 72 · 中风险 · 1小时前  → │
├──────────────────────────────────────────┤
│ [icon] test.apk                          │
│        安全评分 45 · 高风险 · 昨天     → │
└──────────────────────────────────────────┘
```
- 每条使用 `AppDexRow`，图标位显示应用图标 Bitmap
- 评分颜色：≥80 `AuroraGreen`，≥60 `AmberGold`，<60 `RedSupergiant`
- 右侧 chevron 箭头
- 空状态：`EmptyState(icon = Clock, title = "暂无任务", subtitle = "点击上方按钮开始分析")`

### 区域 D：FAB

| 属性 | 值 |
|---|---|
| 目的 | 快速触发分析 |
| 组件 | `AppDexFAB` |
| 交互 | 点击 → 弹出文件选择器 |
| 数据来源 | 无 |

### 首页移除项

| 移除项 | 理由 |
|---|---|
| "多功能 Android 逆向工程工具箱" | 技术定位，普通用户不关心 |
| 快速工具 2×3 网格 | DEX/权限审计/签名验证是专业术语 |
| 硬编码版本号 `v1.0.0` | 不属于用户界面 |
| "开始分析" 按钮跳 Analyzer Tab | 分析应是流程，不是页面 |

---

# 第二部分：APK 分析流程

## 2.1 流程概览

```
用户选择 APK
    ↓
[Analyzing] — 解析中
    ├── 显示进度动画
    ├── 显示当前解析阶段文字
    └── 用户等待
    ↓
[Analyzed] — 分析完成
    ├── 生成安全评分
    ├── 生成风险发现
    ├── AI 生成总结（如已配置）
    └── 自动跳转任务页
    ↓
用户在任务页操作
    ├── 查看文件 → [Analyzed] 保持
    ├── 修改资源 → [Modified]
    ├── 重新签名 → [Signed]
    └── 再次分析 → [Analyzing] → [Analyzed]
    ↓
如果出错 → [Failed]
    ├── 显示错误原因
    └── 提供重试按钮
```

## 2.2 完整状态机

### 状态定义

```
ApkSession (sealed class)
├── Idle          — 无 APK，等待用户选择
├── Analyzing     — 正在解析 APK
├── Analyzed      — 解析完成，有完整 ApkInfo
├── Modified      — APK 内容被修改
├── Signed        — APK 已重新签名
└── Failed        — 解析失败
```

### 每个状态的 UI / 操作 / 下一步

#### Idle

| 维度 | 内容 |
|---|---|
| **UI 展示** | 首页欢迎卡片 + 最近任务列表 |
| **用户操作** | 点击"选择 APK" / FAB / 从文件管理器点击 APK |
| **下一步** | 选择文件后 → `Analyzing` |
| **数据** | 无 APK 上下文 |

#### Analyzing

| 维度 | 内容 |
|---|---|
| **UI 展示** | 全屏 Loading + 阶段文字 |
| 阶段 1 | "正在读取 APK 文件…" |
| 阶段 2 | "正在解析清单文件…" |
| 阶段 3 | "正在分析签名…" |
| 阶段 4 | "正在扫描文件结构…" |
| 阶段 5 | "正在计算安全评分…" |
| 阶段 6 | "正在生成分析报告…" (如配置了 AI) |
| **用户操作** | 可点击取消 → 回到 `Idle` |
| **下一步** | 成功 → `Analyzed`；失败 → `Failed` |
| **数据** | `apkFilePath: String` |

#### Analyzed

| 维度 | 内容 |
|---|---|
| **UI 展示** | 任务页：安全评分卡 + 风险发现 + AI 总结 + 操作中心 + 高级模式（折叠） |
| **用户操作** | 浏览报告 / 点击操作项 / 展开 AI 对话 / 展开高级模式 |
| **下一步** | 修改资源 → `Modified`；签名 → `Signed`；重新分析 → `Analyzing` |
| **数据** | `ApkInfo`（完整）、`securityScore`、`riskFindings`、`aiSummary` |

#### Modified

| 维度 | 内容 |
|---|---|
| **UI 展示** | 任务页 + 顶部黄色横幅"APK 已修改，建议重新签名后安装" |
| **用户操作** | 继续修改 / 重新签名 / 查看变更 |
| **下一步** | 签名 → `Signed`；继续修改 → 保持 `Modified` |
| **数据** | `ApkInfo` + `modifiedEntries: List<String>` |

#### Signed

| 维度 | 内容 |
|---|---|
| **UI 展示** | 任务页 + 顶部绿色横幅"APK 已签名，可以安全安装" |
| **用户操作** | 安装 APK / 分享 / 继续分析 |
| **下一步** | 安装 → 系统 Package Installer；重新分析 → `Analyzing` |
| **数据** | `ApkInfo` + `signatureInfo: ApkSignature` + `signedApkPath: String` |

#### Failed

| 维度 | 内容 |
|---|---|
| **UI 展示** | 全屏 ErrorState + 错误原因 + 重试按钮 |
| **用户操作** | 重试 / 返回首页 |
| **下一步** | 重试 → `Analyzing`；返回 → `Idle` |
| **数据** | `errorMessage: String` |

### 状态转换图

```
                    ┌─────────┐
            ┌──────→│  Idle   │←──────────────┐
            │       └────┬────┘               │
            │            │ 选择APK              │ 返回
            │            ↓                      │
            │       ┌────────────┐              │
            │       │ Analyzing  │              │
            │       └──┬─────┬───┘              │
            │          │     │ 失败              │
            │   成功   │     ↓                  │
            │          │  ┌────────┐            │
            │          │  │ Failed │──重试──┐    │
            │          │  └────────┘       │    │
            │          ↓                    │    │
            │     ┌───────────┐             │    │
            │     │ Analyzed  │←──重新分析──┘    │
            │     └──┬─────┬──┘                  │
            │   修改  │     │ 签名                │
            │        ↓     ↓                     │
            │   ┌─────────┐ ┌──────┐            │
            │   │ Modified │→│Signed│            │
            │   └─────────┘ └──┬───┘            │
            │                    │ 重新分析       │
            └────────────────────┘              │
                                                 │
```

---

# 第三部分：APK 详情页重新设计

## 3.1 设计原则

- **普通用户**：看到安全评分、风险发现、AI 总结、操作建议 — 不需要理解技术
- **专业用户**：展开高级模式即可看到 Manifest / DEX / 资源 / 二进制 / 文件结构

## 3.2 普通模式布局

### 3.2.1 顶部栏

```
┌──────────────────────────────────────────┐
│ ←  APPDEX                          [🤖]  │
│    任务                                    │
└──────────────────────────────────────────┘
```
- `AppDexBar(title = "任务", back = true, onBack = popBackStack)`
- 右侧 AI 图标按钮

### 3.2.2 应用概览卡

```
┌──────────────────────────────────────────┐
│                                          │
│  [AppIcon 48dp]  微信                     │
│                  8.0.20 · 245MB · API 23+ │
│                                          │
│  ──────────────────────────────────────  │
│                                          │
│  安全评分              85/100  低风险    │
│  ████████████████░░░░                    │
│                                          │
└──────────────────────────────────────────┘
```
- 背景：`ScoreCardBg`，边框：`BorderAccent`
- 应用图标：48dp 方块，圆角 8dp
- 应用名：14sp Bold `TextPrimary`
- 版本/大小/SDK：10sp `TextSecondary`
- 分数：48sp Bold `TextPrimary`
- 风险等级：11sp，颜色按分数变化
- 进度条：6dp 高度，背景 `ScoreBarBg`，前景按分数着色

### 3.2.3 风险发现卡

```
风险发现
┌──────────────────────────────────────────┐
│  ✓  无病毒特征                          │
│  ⚠  申请了 32 个权限（含 5 个敏感权限）  │
│  ⚠  检测到广告 SDK（AdMob、Firebase）    │
│  ⚠  请求后台运行权限                     │
│  ✓  签名有效（V2 + V3）                 │
│  ✓  目标 SDK 为最新版本（API 35）       │
└──────────────────────────────────────────┘
```
- `AppDexSection(label = "风险发现")` + `AppDexReportCard`
- ✓ 绿色 `AuroraGreen`，⚠ 黄色 `AmberGold`，✗ 红色 `RedSupergiant`
- 每行：状态图标 + 描述文本（12sp `TextPrimary`）

### 3.2.4 AI 总结卡

```
AI 分析
┌──────────────────────────────────────────┐
│                                          │
│  🤖 这个 APK 申请了摄像头、位置、         │
│     通讯录等敏感权限，可能用于广告推送     │
│     和用户追踪。签名使用 V2+V3 方案，      │
│     证书有效期至 2049 年，无篡改风险。     │
│     检测到 AdMob 和 Firebase Analytics    │
│     两个广告/分析 SDK。                   │
│                                          │
│  ┌──────────────────────────────────┐   │
│  │  询问 AI 更多问题  →             │   │
│  └──────────────────────────────────┘   │
│                                          │
└──────────────────────────────────────────┘
```
- **如果未配置 AI**：显示 "配置 AI 助手以获取智能分析" + 跳转设置按钮
- **如果已配置**：自动调用 `AiService.generateSummary(apkInfo)` 生成总结
- 总结文字：12sp `TextPrimary`，行距 1.5
- 底部按钮：点击展开 AI 对话 BottomSheet

### 3.2.5 操作中心

```
操作
┌──────────────────────────────────────────┐
│  📁  查看文件         1,243 个文件     → │
├──────────────────────────────────────────┤
│  📝  查看代码结构     3 个 DEX 文件     → │
├──────────────────────────────────────────┤
│  ✏  修改资源         编辑资源文件       → │
├──────────────────────────────────────────┤
│  📄  编辑清单         AndroidManifest   → │
├──────────────────────────────────────────┤
│  🔏  重新签名         V2 + V3 已签名    → │
├──────────────────────────────────────────┤
│  🔄  重新打包         Smali → DEX → APK → │
├──────────────────────────────────────────┤
│  🔒  安全检查         深度安全扫描       → │
├──────────────────────────────────────────┤
│  📊  对比版本         双 APK 差异分析   → │
├──────────────────────────────────────────┤
│  📏  体积分析         可视化空间占用     → │
└──────────────────────────────────────────┘
```
- 每项使用 `AppDexRow`
- 左侧图标 + 标题（12sp）+ 详情（10sp `TextSecondary`）
- 右侧 chevron
- 点击跳转对应 feature Screen，传递 `apkPath`

## 3.3 高级模式布局

```
高级分析
┌──────────────────────────────────────────┐
│  ▸  应用清单 (Manifest)                  │
├──────────────────────────────────────────┤
│  ▸  代码结构 (DEX)            3 个文件   │
├──────────────────────────────────────────┤
│  ▸  资源结构 (Resources)     856 个文件 │
├──────────────────────────────────────────┤
│  ▸  二进制 (HEX/ELF)                     │
├──────────────────────────────────────────┤
│  ▸  文件列表                  1,243 项   │
└──────────────────────────────────────────┘
```
- 默认折叠（不展开）
- 展开后每项也是 `AppDexRow`，跳转对应工具
- 技术术语在此层暴露，普通用户不需要展开
- 高级模式标题旁有说明文字："以下为专业技术分析工具"

### 高级模式展开后的子项

| 子项 | 跳转目标 | 参数 |
|---|---|---|
| 应用清单 | `AxmlEditorScreen` | `apkPath` |
| 代码结构 | `DexBrowserScreen` | `apkPath` |
| 资源结构 | `ArscEditorScreen` | `apkPath` |
| 二进制 | 弹出选择：HEX 编辑器 / ELF 查看器 | `filePath` |
| 文件列表 | 内联展开 APK 内文件树 | — |
| SQLite 数据库 | `SqliteViewerScreen` | `dbPath`（如存在 .db 文件） |

---

# 第四部分：AI Assistant 设计

## 4.1 架构概览

```
┌─────────────────────────────────────────────────────┐
│                    core-ai 模块                      │
│                                                      │
│  ┌──────────────┐  ┌──────────────┐  ┌───────────┐ │
│  │ AiProvider   │  │ AiConfig     │  │ AiClient  │ │
│  │ (sealed)    │  │ (data class) │  │ (HTTP)    │ │
│  └──────┬──────┘  └──────┬──────┘  └─────┬─────┘ │
│         │                │                 │        │
│  ┌──────┴──────┐  ┌─────┴──────┐  ┌──────┴─────┐ │
│  │ AiConfig    │  │ Prompt     │  │ ApkContext │ │
│  │ Repository  │  │ Templates  │  │ Builder    │ │
│  │ (DataStore) │  │            │  │            │ │
│  └─────────────┘  └────────────┘  └────────────┘ │
│                                                      │
│              ┌──────────────┐                       │
│              │ AiService    │ ← 高层API             │
│              │ (ask/summary)│                       │
│              └──────────────┘                       │
└─────────────────────────────────────────────────────┘
```

## 4.2 AI Provider 设计

```
sealed interface AiProvider {
    val displayName: String
    val defaultBaseUrl: String
    
    data object OpenAI : AiProvider {
        override val displayName = "OpenAI"
        override val defaultBaseUrl = "https://api.openai.com/v1"
    }
    
    data object Anthropic : AiProvider {
        override val displayName = "Anthropic"
        override val defaultBaseUrl = "https://api.anthropic.com"
    }
    
    data class Custom(
        override val displayName: String,
        override val defaultBaseUrl: String
    ) : AiProvider
}
```

### Provider 差异

| Provider | API 协议 | 请求路径 | 认证头 | 响应解析 |
|---|---|---|---|---|
| OpenAI | OpenAI Chat Completions | `/chat/completions` | `Authorization: Bearer {key}` | `choices[0].message.content` |
| Anthropic | Messages API | `/v1/messages` | `x-api-key: {key}` | `content[0].text` |
| Custom | 兼容 OpenAI 协议 | `/chat/completions` | `Authorization: Bearer {key}` | `choices[0].message.content` |

## 4.3 AiConfig

```
data class AiConfig(
    val provider: AiProvider = AiProvider.OpenAI,
    val baseUrl: String = "",
    val apiKey: String = "",
    val model: String = "gpt-4o-mini"
) {
    val isConfigured: Boolean
        get() = apiKey.isNotEmpty() && baseUrl.isNotEmpty()
}
```

## 4.4 AiConfigRepository（DataStore 持久化）

| Key | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `ai_provider` | String | "openai" | Provider 标识 |
| `ai_base_url` | String | "" | 自定义 API 地址 |
| `ai_api_key` | String | "" | API 密钥（不明文回显） |
| `ai_model` | String | "" | 模型名称 |

- API Key 存储在 DataStore，设置页 UI 以密码模式显示（`••••••••`）
- 提供 `testConnection()` 方法：发送一条测试消息，返回成功/失败

## 4.5 Prompt 模板

### 4.5.1 APK 安全分析总结模板

```
你是一个 APK 安全分析助手。以下是 APK 的分析数据：

{apkContext}

请用通俗易懂的语言（避免技术术语）为普通用户生成一份安全分析总结，包含：
1. 这个应用是做什么的
2. 安全评分的含义
3. 主要风险点（如果有）
4. 建议

字数控制在 200 字以内。
```

### 4.5.2 权限解释模板

```
用户问："这个 APK 为什么申请 {permission} 权限？"

以下是 APK 的权限列表和组件信息：
{apkContext}

请解释：
1. {permission} 权限的作用
2. 这个 APK 可能为什么需要它
3. 风险等级（低/中/高）
4. 用户是否应该担心

用普通人能懂的语言回答。
```

### 4.5.3 修改指导模板

```
用户想修改 APK 的 {target}。

以下是 APK 的文件结构：
{apkContext}

请分析：
1. 需要修改的文件位置
2. 推荐使用的工具
3. 修改步骤
4. 修改后的注意事项（如需要重新签名）

不要生成任何破解或绕过验证的代码。
引导用户使用 AppDex 内置的编辑工具。
```

### 4.5.4 代码定位模板

```
用户问："{question}"

以下是 APK 的 DEX 类列表（前 200 个）和组件清单：
{dexClasses}
{components}

请分析可能相关的代码位置：
1. 哪些类/方法可能包含相关逻辑
2. 关键字搜索建议
3. 推荐的查看路径

不要自动修改代码。提供分析后，用户可以跳转到 DEX 浏览器自行查看。
```

## 4.6 ApkContextBuilder

将 `ApkInfo` 转为 AI 可读文本：

```
=== APK 基本信息 ===
应用包名: com.tencent.mm
版本: 8.0.20 (versionCode 2400)
文件大小: 245 MB
最小 SDK: 23 (Android 6.0)
目标 SDK: 35 (Android 15)

=== 权限 (32 个) ===
普通权限 (27 个):
  - android.permission.INTERNET
  - android.permission.ACCESS_NETWORK_STATE
  ...

敏感权限 (5 个):
  - android.permission.CAMERA
  - android.permission.ACCESS_FINE_LOCATION
  - android.permission.RECORD_AUDIO
  - android.permission.READ_CONTACTS
  - android.permission.WRITE_EXTERNAL_STORAGE

=== 组件 ===
Activities: 48
Services: 12
Receivers: 8
Providers: 3

=== 签名 ===
签名方案: V2 + V3
算法: SHA256withRSA
证书主体: CN=WeChat
SHA-256: A1B2C3...
有效期: 2020-01-01 至 2049-12-31

=== 文件结构 ===
总文件数: 1,243
DEX 文件: 3 (classes.dex 2.3MB, classes2.dex 1.8MB, classes3.dex 1.2MB)
资源文件: 856
原生库: 15 (.so 文件)
```

## 4.7 AiService 高层 API

```
class AiService @Inject constructor(
    private val configRepository: AiConfigRepository,
    private val contextBuilder: ApkContextBuilder
) {
    // 生成 APK 安全总结
    suspend fun generateSummary(apkInfo: ApkInfo): Result<String>
    
    // 回答用户问题
    suspend fun ask(question: String, apkInfo: ApkInfo): Result<String>
    
    // 流式回复
    fun askStream(question: String, apkInfo: ApkInfo): Flow<String>
    
    // 测试连接
    suspend fun testConnection(): Result<String>
    
    // 是否已配置
    fun isConfigured(): Boolean
}
```

---

# 第五部分：AI 使用场景设计

## 场景 1：安全评估

> **用户**："这个 APK 安全吗？"
>
> **AI 读取**：ApkInfo 全量数据 — 权限、签名、组件、SDK 版本、文件结构
>
> **AI 回答**：
> "这个 APK（微信）的安全评分为 85/100，属于低风险。
>
> 好的方面：
> - 签名有效，使用 V2+V3 方案
> - 目标 SDK 为最新版本
> - 无病毒特征
>
> 需要注意：
> - 申请了 5 个敏感权限（摄像头、位置、录音、通讯录、存储）
> - 包含 AdMob 和 Firebase Analytics 广告 SDK
> - 申请后台运行权限
>
> 总体来说，这是一个正常的社交应用，权限申请在合理范围内。"

## 场景 2：权限解释

> **用户**："这个 APK 为什么申请摄像头权限？"
>
> **AI 读取**：权限列表 + Activities 列表
>
> **AI 回答**：
> "摄像头权限通常用于以下功能：
> 1. 扫一扫 — 扫描二维码
> 2. 视频通话 — 实时视频
> 3. 拍照发送 — 聊天中发送照片
>
> 这个 APK 有 3 个 Activity 声明了摄像头相关功能：
> - .plugin.scanner.ui.BaseScanUI（扫码）
> - .voip.video.VideoActivity（视频通话）
> - .camera.CameraActivity（拍照）
>
> 风险等级：低。这些是社交应用的常见功能。"

## 场景 3：修改应用名称

> **用户**："我想修改自己的 APK 应用名称"
>
> **AI 读取**：文件结构 + ArscEditor 数据
>
> **AI 回答**：
> "修改应用名称需要编辑 APK 内的资源文件。
>
> 修改位置：`resources.arsc` 文件中的 `app_name` 字段
>
> 推荐工具：AppDex 内置的资源编辑器
>
> 步骤：
> 1. 点击「修改资源」打开资源编辑器
> 2. 搜索 `app_name`
> 3. 修改为新名称
> 4. 保存
> 5. 重新签名 APK
>
> 点击下方按钮直接打开资源编辑器。"
>
> **操作**：底部出现"打开资源编辑器"按钮 → 跳转 `ArscEditorScreen`

## 场景 4：SDK 识别

> **用户**："这个 APK 用了哪些第三方 SDK？"
>
> **AI 读取**：文件结构 + DEX 类列表
>
> **AI 回答**：
> "通过分析 APK 文件结构和代码包名，检测到以下第三方 SDK：
>
> 广告类：
> - Google AdMob (com.google.ads)
> - Facebook Audience Network (com.facebook.ads)
>
> 分析类：
> - Firebase Analytics (com.google.firebase.analytics)
> - Bugly 崩溃上报 (com.tencent.bugly)
>
> 推送类：
> - 个推 (com.igexin)
>
> 地图类：
> - 高德地图 (com.amap.api)
>
> 共 7 个第三方 SDK，占用约 18% 的 APK 体积。"

## 场景 5：代码定位（高级用户）

> **用户**："告诉我会员验证逻辑在哪里"
>
> **AI 读取**：DEX 类列表 + 方法名搜索
>
> **AI 回答**：
> "通过分析 DEX 文件中的类和方法名，以下位置可能与会员验证逻辑相关：
>
> 可能相关的类：
> 1. `com.example.app.vip.VipManager` — 名称直接匹配
> 2. `com.example.app.user.UserValidator` — 可能包含验证逻辑
> 3. `com.example.app.network.ApiClient$VipApi` — 网络请求相关
>
> 关键字搜索建议：
> - `isVip` / `checkVip` / `vipStatus`
> - `subscription` / `premium`
> - `validate` / `verify`
>
> 推荐查看路径：
> 1. 先在 DEX 浏览器中搜索 `VipManager`
> 2. 查看其 `checkVipStatus()` 方法
> 3. 追踪网络请求 `VipApi.validate()`
>
> ⚠ 风险说明：修改会员验证逻辑可能违反应用的服务条款。
> AppDex 不会自动生成破解代码，仅提供分析定位。
> 修改后的 APK 必须重新签名才能安装。
>
> 点击下方按钮打开 DEX 浏览器。"
>
> **操作**：底部出现"打开 DEX 浏览器"按钮 → 跳转 `DexBrowserScreen`

## 场景 6：签名验证

> **用户**："这个 APK 的签名信息是怎样的？"
>
> **AI 读取**：签名数据
>
> **AI 回答**：
> "这个 APK 使用了 V2 + V3 签名方案：
>
> 签名者：CN=WeChat, OU=WeChat Team, O=Tencent
> 算法：SHA256withRSA
> 有效期：2020-01-01 至 2049-12-31（还有 23 年有效）
>
> 指纹：
> - SHA-256: A1B2C3D4...
> - SHA-1:   E5F6G7H8...
> - MD5:     I9J0K1L2...
>
> 签名状态：有效，未被篡改。
> 如果你要修改 APK，修改后必须重新签名才能安装。"

## 场景 7：体积优化建议

> **用户**："这个 APK 太大了，能优化吗？"
>
> **AI 读取**：文件结构 + SizeAnalyzer 数据
>
> **AI 回答**：
> "这个 APK 总大小 245 MB，各部分占用：
>
> - 原生库 (.so): 156 MB (64%) — armeabi-v7a + arm64-v8a + x86 + x86_64 四套架构
> - 资源文件: 52 MB (21%) — 含大量图片和语言包
> - DEX 代码: 28 MB (11%)
> - 其他: 9 MB (4%)
>
> 优化建议：
> 1. 只保留 arm64-v8a 架构的 .so 文件 — 可节省约 80 MB
> 2. 删除不需要的语言包（res/values-de, res/values-ja 等）— 可节省约 15 MB
> 3. 压缩 PNG 图片 — 可节省约 10 MB
>
> 点击「体积分析」查看详细可视化。"
>
> **操作**：底部出现"打开体积分析"按钮 → 跳转 `SizeAnalyzerScreen`

## 场景 8：Diff 版本对比

> **用户**："新版本和旧版本有什么区别？"
>
> **AI 读取**：两个 APK 的 ApkInfo 对比
>
> **AI 回回答**：
> "对比了 v8.0.20 和 v8.0.18：
>
> 变化：
> - 版本号：8.0.18 → 8.0.20
> - 文件大小：238 MB → 245 MB (+7 MB)
> - DEX 文件：2 个 → 3 个（新增 classes3.dex，约 1.2 MB）
> - 新增权限：android.permission.NEARBY_WIFI_DEVICES
> - 移除权限：无
> - 新增 Activity：3 个（视频通话相关）
>
> 总结：新版本主要新增了视频通话功能，申请了 WiFi 相关权限。"

## 场景 9：Manifest 解读

> **用户**："这个 APK 的 Manifest 里有什么值得关注的？"
>
> **AI 读取**：ApkManifest 全量数据
>
> **AI 回答**：
> "Manifest 分析结果：
>
> 组件数量：
> - 48 个 Activity（界面）
> - 12 个 Service（后台服务）
> - 8 个 Receiver（广播接收器）
> - 3 个 Provider（内容提供器）
>
> 值得关注：
> 1. `allowBackup="true"` — 允许应用数据备份，有数据泄露风险
> 2. `debuggable="false"` — 正常，发布版本应为 false
> 3. `exported="true"` 的 Activity 有 3 个 — 可能被外部应用调用
> 4. `usesCleartextTraffic="true"` — 允许 HTTP 明文传输
>
> 建议查看「编辑清单」修改安全配置。"

## 场景 10：安全漏洞检测

> **用户**："这个 APK 有安全漏洞吗？"
>
> **AI 读取**：SecurityScanner 扫描结果 + ApkInfo
>
> **AI 回答**：
> "安全扫描完成，发现以下问题：
>
> 高风险：
> - 检测到硬编码 API Key（com.example.app.config.Constants）
> - 允许 HTTP 明文传输
>
> 中风险：
> - allowBackup=true（数据可备份）
> - 3 个 exported Activity 未设置权限保护
>
> 低风险：
> - minSdk=21（支持较低版本，可能有兼容性问题）
>
> 好的方面：
> - 签名有效
> - debuggable=false
> - 未检测到 root 检测绕过代码
>
> 点击「安全检查」查看完整报告。"
>
> **操作**：底部出现"打开安全扫描"按钮 → 跳转 `SecurityScannerScreen`

## 场景 11：安装来源验证

> **用户**："这个 APK 是官方版本吗？"
>
> **AI 读取**：签名证书 + 文件信息
>
> **AI 回答**：
> "无法 100% 确认是否为官方版本，但可以从以下线索判断：
>
> 签名信息：
> - 签名者：CN=WeChat, OU=WeChat Team, O=Tencent
> - 这与腾讯官方签名一致
>
> 文件信息：
> - 文件大小：245 MB（与官方版本接近）
> - DEX 文件数量：3 个（正常）
>
> 建议：
> 1. 对比官方应用商店下载的 APK 签名指纹
> 2. 如果 SHA-256 指纹一致，则为官方版本
> 3. 如果不一致，可能被二次打包
>
> 点击「签名验证」查看完整签名信息。"

---

# 第六部分：UI 页面重新设计

## 6.1 HomeScreen（首页）

| 属性 | 值 |
|---|---|
| **目标** | 让用户 3 秒内知道"我要分析 APK" |
| **布局** | LazyColumn 自上而下 |
| **组件** | AppDexBar → HeroCard → RecentTaskList → FAB |
| **按钮** | "选择 APK 文件"（主按钮）、最近任务列表项 |
| **交互** | 点击主按钮/FAB → FilePicker → 分析流程；点击列表项 → 恢复会话 → 任务页 |

**布局详图**：
```
┌──────────────────────────────────────────┐
│  APPDEX                            [🤖]  │
│  AppDex                                   │
├──────────────────────────────────────────┤
│                                          │
│  ┌──────────────────────────────────────┐│
│  │ 你好，我可以帮你分析 APK             ││
│  │ 选择一个 APK 文件，我会自动完成      ││
│  │ 安全分析、权限检查和风险评估。       ││
│  │                                      ││
│  │ ┌────────────────────────────────┐  ││
│  │ │     ⊕ 选择 APK 文件            │  ││
│  │ └────────────────────────────────┘  ││
│  └──────────────────────────────────────┘│
│                                          │
│  最近任务                                │
│  ┌──────────────────────────────────────┐│
│  │[icon] 微信 85 低风险 2分钟前      → ││
│  │[icon] Telegram 72 中风险 1小时前  → ││
│  └──────────────────────────────────────┘│
│                                          │
│                              ┌────────┐ │
│                              │   ⊕    │ │
│                              └────────┘ │
└──────────────────────────────────────────┘
```

## 6.2 AnalysisFlowScreen（分析流程页）

| 属性 | 值 |
|---|---|
| **目标** | 在 APK 解析过程中给用户即时反馈 |
| **布局** | 全屏居中 |
| **组件** | 进度动画 + 阶段文字 + 取消按钮 |
| **按钮** | "取消"（返回首页） |
| **交互** | 自动完成后跳转任务页；失败显示错误+重试 |

**布局详图**：
```
┌──────────────────────────────────────────┐
│                                          │
│                                          │
│              ◌ (加载动画)                 │
│                                          │
│         正在解析清单文件…                  │
│         已完成 3/6 步                     │
│                                          │
│  ┌──────────────────────────────────┐    │
│  │ ████████████░░░░░░░░░░  50%     │    │
│  └──────────────────────────────────┘    │
│                                          │
│           [ 取消 ]                        │
│                                          │
└──────────────────────────────────────────┘
```

**阶段文字映射**：
| 阶段 | 文字 | 对应操作 |
|---|---|---|
| 1 | "正在读取 APK 文件…" | copyUriToCache |
| 2 | "正在解析清单文件…" | BinaryXmlDecoder.decode |
| 3 | "正在分析签名…" | ApkFile.getSignatures |
| 4 | "正在扫描文件结构…" | ApkFile.listEntries |
| 5 | "正在计算安全评分…" | SecurityScoreCalculator |
| 6 | "正在生成分析报告…" | AiService.generateSummary（如已配置） |

## 6.3 TaskScreen（任务页）

| 属性 | 值 |
|---|---|
| **目标** | 当前 APK 的工作台 — 普通模式 + 高级模式 + AI 助手 |
| **布局** | LazyColumn + 可折叠高级区域 + BottomSheet |
| **组件** | AppDexBar → AppOverviewCard → SecurityScoreCard → RiskReportCard → AiSummaryCard → ActionCenter → AdvancedMode（折叠）|
| **按钮** | AI 助手图标、操作中心每项、高级模式展开/折叠 |
| **交互** | 操作项跳转对应功能页（传 apkPath）；AI 图标展开对话面板 |

**普通模式布局详图**：
```
┌──────────────────────────────────────────┐
│ ←  APPDEX                          [🤖]  │
│    任务                                    │
├──────────────────────────────────────────┤
│ ┌──────────────────────────────────────┐ │
│ │ [icon] 微信 8.0.20 · 245MB · API 23+ │ │
│ │ ──────────────────────────────────── │ │
│ │ 安全评分           85/100  低风险    │ │
│ │ ████████████████░░░░                 │ │
│ └──────────────────────────────────────┘ │
│                                          │
│ 风险发现                                  │
│ ┌──────────────────────────────────────┐ │
│ │ ✓ 无病毒特征                        │ │
│ │ ⚠ 32 个权限（5 个敏感）             │ │
│ │ ⚠ 广告 SDK（AdMob、Firebase）        │ │
│ │ ⚠ 后台运行权限                       │ │
│ │ ✓ 签名有效（V2+V3）                  │ │
│ └──────────────────────────────────────┘ │
│                                          │
│ AI 分析                                   │
│ ┌──────────────────────────────────────┐ │
│ │ 🤖 这个 APK 申请了摄像头、位置…       │ │
│ │    点击 [询问 AI 更多问题]           │ │
│ └──────────────────────────────────────┘ │
│                                          │
│ 操作                                      │
│ ┌──────────────────────────────────────┐ │
│ │ 📁 查看文件        1,243 个文件    → │ │
│ │ 📝 查看代码结构    3 个 DEX         → │ │
│ │ ✏ 修改资源        编辑资源文件     → │ │
│ │ 📄 编辑清单        AndroidManifest   → │ │
│ │ 🔏 重新签名        V2+V3 已签名     → │ │
│ │ 🔄 重新打包        Smali→DEX→APK   → │ │
│ │ 🔒 安全检查        深度安全扫描      → │ │
│ │ 📊 对比版本        双 APK 差异      → │ │
│ │ 📏 体积分析        可视化空间占用    → │ │
│ └──────────────────────────────────────┘ │
│                                          │
│ 高级分析                                  │
│ ┌──────────────────────────────────────┐ │
│ │ ▸ 专业技术分析工具                    │ │
│ │   (展开查看 Manifest/DEX/资源/...)    │ │
│ └──────────────────────────────────────┘ │
└──────────────────────────────────────────┘
```

**高级模式展开后布局**：
```
│ 高级分析                                  │
│ ┌──────────────────────────────────────┐ │
│ │ ▾ 专业技术分析工具                    │ │
│ │   ──────────────────────────────────│ │
│ │   ▸ 应用清单 (Manifest)             → │ │
│ │   ▸ 代码结构 (DEX)        3 个文件  → │ │
│ │   ▸ 资源结构 (Resources)  856 个    → │ │
│ │   ▸ 二进制 (HEX/ELF)               → │ │
│ │   ▸ SQLite 数据库                  → │ │
│ │   ▸ 文件列表             1,243 项   → │ │
│ └──────────────────────────────────────┘ │
```

**AI 助手 BottomSheet 布局**：
```
┌──────────────────────────────────────────┐
│ (任务页内容模糊背景)                      │
├──────────────────────────────────────────┤
│ AI 助手                            [✕]   │
│                                          │
│ ┌──────────────────────────────────────┐ │
│ │ 🤖 你好！我已经读取了这个 APK 的分析  │ │
│ │    数据。有什么想了解的？             │ │
│ └──────────────────────────────────────┘ │
│                                          │
│ ┌────────────┐ ┌────────────┐           │
│ │ 这个APK安全？│ │ 权限解释   │           │
│ └────────────┘ └────────────┘           │
│ ┌────────────┐ ┌────────────┐           │
│ │ 用了哪些SDK │ │ 修改应用名 │           │
│ └────────────┘ └────────────┘           │
│                                          │
│ ┌──────────────────────────────────┐    │
│ │ 输入问题…                    [→] │    │
│ └──────────────────────────────────┘    │
└──────────────────────────────────────────┘
```

## 6.4 ToolsScreen（工具页）

| 属性 | 值 |
|---|---|
| **目标** | 所有专业工具的集中入口，按任务分类 |
| **布局** | LazyColumn，3 个分区 |
| **组件** | AppDexBar + 3 × AppDexSection |
| **按钮** | 每项 AppDexRow |
| **交互** | 点击跳转对应功能页面 |

**布局详图**：
```
┌──────────────────────────────────────────┐
│  APPDEX                                   │
│  工具                                     │
├──────────────────────────────────────────┤
│                                          │
│ 分析工具                                  │
│ ┌──────────────────────────────────────┐ │
│ │ 🔒 权限检测      组件导出/危险权限  → │ │
│ │ 🐛 安全扫描      密钥/漏洞检测       → │ │
│ │ 📦 包信息        需要先选择 APK       → │ │
│ │ 📏 体积分析      可视化空间占用       → │ │
│ └──────────────────────────────────────┘ │
│                                          │
│ 修改工具                                  │
│ ┌──────────────────────────────────────┐ │
│ │ ✏ 文件编辑       支持语法定义       → │ │
│ │ 🎨 资源修改       编辑 resources.arsc → │ │
│ │ 📄 清单编辑       二进制 XML 编解码   → │ │
│ │ 🔄 重打包         Smali→DEX→APK     → │ │
│ │ 📊 版本对比       双 APK 差异分析     → │ │
│ └──────────────────────────────────────┘ │
│                                          │
│ 开发工具                                  │
│ ┌──────────────────────────────────────┐ │
│ │ 📝 DEX 浏览      类/方法/字符串索引  → │ │
│ │ 🔢 HEX 编辑      十六进制查看/编辑    → │ │
│ │ 🧩 ELF 查看      .so 共享库解析      → │ │
│ │ 💾 SQLite 查看   .db 数据库浏览       → │ │
│ │ 🔏 签名验证      证书/摘要/签名方案   → │ │
│ │ > 终端           本地 shell 会话     → │ │
│ │ ☁ 远程管理       局域网文件访问       → │ │
│ │ # 哈希计算       MD5/SHA-1/SHA-256   → │ │
│ │ 🔤 编码转换       Base64/URL/Hex     → │ │
│ │ 📱 设备信息       硬件/系统信息       → │ │
│ │ 🔌 插件中心       3 个插件            → │ │
│ └──────────────────────────────────────┘ │
└──────────────────────────────────────────┘
```

## 6.5 SettingsScreen（设置页）

| 属性 | 值 |
|---|---|
| **目标** | 应用配置 + AI 服务配置 |
| **布局** | LazyColumn，可折叠分区 |
| **组件** | AppDexBar + ExpandableConfigRow × N |
| **按钮** | 各配置项 + "测试连接"按钮 |
| **交互** | AI 配置保存到 DataStore；测试连接发送消息验证 |

**布局详图**：
```
┌──────────────────────────────────────────┐
│  APPDEX                                   │
│  设置                                     │
├──────────────────────────────────────────┤
│                                          │
│ AI 服务                                   │
│ ┌──────────────────────────────────────┐ │
│ │ 服务商:  [OpenAI] [Anthropic] [自定义]│ │
│ │ 地址:    https://api.openai.com/v1   │ │
│ │ 密钥:    ••••••••••••••••             │ │
│ │ 模型:    gpt-4o-mini                  │ │
│ │                                      │ │
│ │         [ 测试连接 ]                  │ │
│ └──────────────────────────────────────┘ │
│                                          │
│ 配置                                      │
│ ┌──────────────────────────────────────┐ │
│ │ ▸ 文件管理      隐藏文件/默认目录     │ │
│ │ ▸ 外观          深色模式/信息密度     │ │
│ │ ▸ 高级选项      缓存/日志/实验功能     │ │
│ └──────────────────────────────────────┘ │
│                                          │
│ 语言     [系统] [中文] [English]          │
│                                          │
│ 编辑器    字体大小: 14    Tab 宽度: 4      │
│                                          │
│ 终端      字体大小: 13    回滚: 1000      │
│                                          │
│ 关于      v1.0.0 · Apache 2.0            │
└──────────────────────────────────────────┘
```

## 6.6 AIChatScreen（AI 对话页）

| 属性 | 值 |
|---|---|
| **目标** | AI 辅助对话，读取当前 APK 上下文 |
| **布局** | BottomSheet 或独立全屏页 |
| **组件** | 对话气泡列表 + 输入框 + 快捷问题 |
| **按钮** | 发送、快捷问题按钮、关闭 |
| **交互** | 输入问题 → AI 读取 ApkSession → 流式回复 |

**布局详图**：
```
┌──────────────────────────────────────────┐
│ AI 助手                            [✕]   │
├──────────────────────────────────────────┤
│                                          │
│  ┌──────────────────────────────────────┐│
│  │🤖 你好！我已经读取了这个 APK 的分析   ││
│  │   数据。有什么想了解的？              ││
│  └──────────────────────────────────────┘│
│                                          │
│                    ┌───────────────────┐ │
│                    │ 这个APK安全吗？  │ │
│                    └───────────────────┘ │
│                                          │
│  ┌──────────────────────────────────────┐│
│  │🤖 这个 APK 的安全评分为 85/100，    ││
│  │   属于低风险…                         ││
│  └──────────────────────────────────────┘│
│                                          │
│  快捷问题:                                │
│  [权限解释] [SDK识别] [修改指导] [代码定位]│
│                                          │
├──────────────────────────────────────────┤
│ ┌──────────────────────────────────┐    │
│ │ 输入问题…                    [→] │    │
│ └──────────────────────────────────┘    │
└──────────────────────────────────────────┘
```

---

# 第七部分：代码迁移策略

## 7.1 保留不变（零改动）

| 模块 | 文件数 | 理由 |
|---|---|---|
| KMP 解析引擎 (core/apk, arsc, axml, dex, io) | ~30 | 纯 Kotlin 解析器，完全稳定 |
| Library (lib-apk, lib-archive, lib-syntax) | ~10 | 高层封装，稳定 |
| 所有 Feature Screen (除 ApkDetailScreen, ToolsScreen) | ~24 | 各功能页面，不改动 |
| 所有 ViewModel (除 ApkAnalyzerViewModel, SettingsViewModel) | ~15 | 状态管理，不改动 |
| 所有 Repository | ~13 | 业务逻辑，不改动 |
| MVI 架构 (BaseViewModel, MviIntent, MviState, MviEffect) | 4 | 架构基础 |
| Design System (Color, Shape, Theme) | 3 | 设计系统 |
| 现有组件 (DesignSystem, StateComponents, AppDexBottomNav) | 3 | 组件库 |
| Hilt DI 配置 | 不变 | 仅新增模块的 DI |
| **合计保留** | **~102 文件** | |

## 7.2 新增模块

| 模块 | 文件 | 职责 |
|---|---|---|
| `core-session` | ApkSession.kt | 会话状态 sealed class |
| | ApkSessionManager.kt | @Singleton 状态管理 |
| | SecurityScoreCalculator.kt | 安全评分（从 ApkDetailScreen 提取） |
| | RiskAssessor.kt | 风险发现生成 |
| | RecentTask.kt | 最近任务数据类 |
| | RecentTaskRepository.kt | Room 持久化 |
| `core-ai` | AiProvider.kt | Provider 抽象 |
| | AiConfig.kt | 配置数据类 |
| | AiConfigRepository.kt | DataStore 持久化 |
| | AiClient.kt | HTTP 调用 |
| | PromptTemplates.kt | Prompt 模板 |
| | ApkContextBuilder.kt | ApkInfo→文本 |
| | AiService.kt | 高层 API |
| `core-ui` 新增组件 | TaskWorkbenchComponents.kt | AppDexStatusCard 等 |
| | AiAssistantPanel.kt | AI 对话 BottomSheet |
| | AiConfigComponents.kt | AI 配置表单 |
| **合计新增** | **~17 文件** | |

## 7.3 需要重构

| 文件 | 改动程度 | 改动内容 |
|---|---|---|
| `Route.kt` | 轻度 | 新增 `TaskWorkbench`、`AiAssistant`、`AiConfig` 路由；废弃 `ApkDetail`（保留兼容） |
| `AppDexNavHost.kt` | 中等 | 底部 Tab 2 从"分析"→"任务"；导航目标改为 `TaskWorkbench`；新增 AI 路由注册 |
| `HomeScreen.kt` | 完全重写 | 移除技术术语、移除快速工具网格、新增欢迎卡片、接入最近任务 |
| `ApkDetailScreen.kt` | 重构 | 改为 `TaskWorkbenchScreen`：普通模式+高级模式+AI 助手入口 |
| `ApkAnalyzerViewModel.kt` | 轻度 | 注入 `ApkSessionManager`，分析完成调用 `setAnalyzed()` |
| `SettingsScreen.kt` | 轻度扩展 | 新增 AI 服务配置分区 |
| `SettingsViewModel.kt` | 轻度扩展 | 新增 AI 配置读写 |
| `ToolsScreen.kt` | 中等重构 | 三分类重组 |
| `settings.gradle.kts` | 轻度 | +2 include |
| `app/build.gradle.kts` | 轻度 | +2 dependency |
| `feature-settings/build.gradle.kts` | 轻度 | +1 dependency |
| `feature-analyzer/build.gradle.kts` | 轻度 | +1 dependency |
| `gradle/libs.versions.toml` | 轻度 | +okhttp 版本 |
| `AppDexDatabase.kt` | 轻度 | +RecentTask Entity |
| **合计重构** | **~14 文件** | |

## 7.4 迁移原则

1. **渐进式迁移** — 每个 Sprint 后必须编译通过 + 安装测试
2. **不删功能** — 所有现有 Screen/ViewModel/Repository 保留
3. **不降能力** — 专业工具完整保留，仅入口层级化
4. **不改架构** — MVI、Hilt、Compose、DataStore 不变
5. **不改设计** — Color/Shape/Theme/现有组件不变，仅新增组件

## 7.5 风险矩阵

| 风险 | 概率 | 影响 | 缓解措施 |
|---|---|---|---|
| 路由变更导致现有页面无法跳转 | 中 | 高 | 渐进式路由变更，保留旧路由兼容 |
| ApkSessionManager 单例状态丢失 | 低 | 中 | 使用 SavedStateHandle + Room 双重持久化 |
| AI API 调用超时/失败 | 高 | 低 | 超时 30s + 降级为"AI 不可用"提示 |
| 安全评分提取后计算结果不一致 | 低 | 低 | 先保留原逻辑，Sprint 4 再替换 |
| 最近任务 Room 版本冲突 | 低 | 中 | 数据库版本递增 + migration 策略 |

---

# 第八部分：Sprint 实施计划

## Sprint 0：基础设施

| 属性 | 值 |
|---|---|
| **目标** | 新增 `core-session` + `core-ai` 模块骨架 |
| **预估** | 1 天 |
| **修改文件** | `settings.gradle.kts`、`app/build.gradle.kts`、`gradle/libs.versions.toml` |
| **新增文件** | ~12 文件（两个模块的骨架） |
| **风险** | 低 — 仅新增模块，不修改现有代码 |
| **验收标准** | 1. `./gradlew assembleDebug` 编译通过 2. 安装 APK 3. App 正常启动 4. 原有功能不受影响 |

**新增内容**：
- `core-session`：ApkSession sealed class、ApkSessionManager (@Singleton)、SecurityScoreCalculator、RiskAssessor、RecentTask data class、RecentTaskRepository
- `core-ai`：AiProvider sealed interface、AiConfig data class、AiConfigRepository、AiClient、PromptTemplates、ApkContextBuilder、AiService

## Sprint 1：首页重构

| 属性 | 值 |
|---|---|
| **目标** | 首页从"工具箱控制台"变为"任务驱动入口" |
| **预估** | 0.5 天 |
| **修改文件** | `HomeScreen.kt`（完全重写）、`AppDexNavHost.kt`（Home 路由回调） |
| **新增组件** | `AppDexPrimaryAction`（在 core-ui 新增） |
| **风险** | 低 — 仅修改首页，其他页面不变 |
| **验收标准** | 1. 编译通过 2. 安装 APK 3. 首页无技术术语 4. "选择 APK"按钮可弹出文件选择器 5. 选择 APK 后进入分析流程 6. 空状态正确显示 7. 旋转不崩溃 |

## Sprint 2：Session 系统

| 属性 | 值 |
|---|---|
| **目标** | 接入 ApkSessionManager，分析完成后持久化状态 |
| **预估** | 0.5 天 |
| **修改文件** | `ApkAnalyzerViewModel.kt`（注入 ApkSessionManager）、`AppDexNavHost.kt`（分析完成跳转逻辑） |
| **新增文件** | `RecentTaskRepository.kt`（Room DAO + Entity） |
| **修改文件** | `AppDexDatabase.kt`（新增 Entity） |
| **风险** | 中 — 修改 ViewModel 可能影响现有分析流程 |
| **验收标准** | 1. 编译通过 2. 分析 APK 后 Session 状态变为 Analyzed 3. 退出重进后状态可恢复 4. 原有分析流程不受影响 |

## Sprint 3：AI 助手

| 属性 | 值 |
|---|---|
| **目标** | 接入 AI 服务，设置页可配置 AI Provider |
| **预估** | 1 天 |
| **修改文件** | `SettingsScreen.kt`（新增 AI 配置分区）、`SettingsViewModel.kt`（新增 AI 配置方法）、`feature-settings/build.gradle.kts` |
| **新增组件** | `AiConfigComponents.kt`（AI 配置表单）、`AiAssistantPanel.kt`（AI 对话 BottomSheet） |
| **风险** | 中 — AI API 调用可能因网络/配置问题失败 |
| **验收标准** | 1. 编译通过 2. 设置页显示 AI 配置 3. 可保存/读取配置 4. 测试连接有反馈 5. API Key 不明文显示 6. 原有设置不受影响 |

## Sprint 4：任务详情

| 属性 | 值 |
|---|---|
| **目标** | ApkDetailScreen 重构为 TaskWorkbenchScreen |
| **预估** | 1 天 |
| **修改文件** | `ApkDetailScreen.kt`（重构为普通模式+高级模式）、`AppDexNavHost.kt`（路由调整）、`Route.kt`（新增 TaskWorkbench 路由）、`feature-analyzer/build.gradle.kts` |
| **新增组件** | `TaskWorkbenchComponents.kt`（AppDexStatusCard、AppDexReportCard） |
| **风险** | 高 — 核心页面重构，影响最大 |
| **验收标准** | 1. 编译通过 2. 分析后进入任务页 3. 普通模式显示评分+风险+AI总结+操作中心 4. 高级模式可折叠/展开 5. 操作中心每项可跳转 6. 跳转后返回不丢状态 7. AI 助手按钮可展开 BottomSheet |

## Sprint 5：工具重新组织

| 属性 | 值 |
|---|---|
| **目标** | 工具页按三分类重组 |
| **预估** | 0.5 天 |
| **修改文件** | `ToolsScreen.kt`（重构三分类） |
| **风险** | 低 — 仅修改布局，所有子工具不变 |
| **验收标准** | 1. 编译通过 2. 工具页显示三分类 3. 每项可跳转 4. 无死路 5. 所有专业工具入口可用 |

## Sprint 6：完整测试

| 属性 | 值 |
|---|---|
| **目标** | 全流程真机验证 |
| **预估** | 0.5 天 |
| **修改文件** | 无（仅验证） |
| **风险** | 无 |
| **验收标准** | 见下方完整验证清单 |

### 完整验证清单

**编译验证**：
- [ ] `./gradlew assembleDebug` 零错误
- [ ] `./gradlew assembleRelease` 零错误（含 R8）

**安装验证**：
- [ ] APK 安装成功
- [ ] 启动无崩溃
- [ ] 底部 5 Tab 全部可进入：首页/任务/文件/工具/设置

**核心用户流程验证**：
- [ ] 首页 → 选择 APK → 分析 → 任务页 → 查看安全评分
- [ ] 任务页 → 查看风险发现
- [ ] 任务页 → AI 总结（已配置时）
- [ ] 任务页 → AI 助手对话
- [ ] 任务页 → 展开高级模式 → 查看 Manifest
- [ ] 任务页 → DEX 浏览 → 返回
- [ ] 任务页 → 签名 → 返回
- [ ] 任务页 → 重打包 → 返回
- [ ] 任务页 → 安全扫描 → 返回
- [ ] 首页 → 最近任务 → 点击恢复 → 任务页
- [ ] 工具页 → 三分类 → 每项可进入
- [ ] 设置页 → AI 配置 → 保存 → 重进保持
- [ ] 设置页 → 测试连接 → 反馈
- [ ] 文件管理器 → 打开文件 → Editor → 编辑 → 保存
- [ ] Terminal → 可输入命令
- [ ] Web Server → 可启动
- [ ] Settings → 返回首页

**稳定性验证**：
- [ ] 旋转屏幕不崩溃
- [ ] 后台恢复不崩溃
- [ ] 退出重进不丢失状态
- [ ] 无空页面
- [ ] 无无法返回的页面
- [ ] Loading 可消失
- [ ] Error 可恢复

---

## 文件变更总览

| 类别 | 文件数 | 说明 |
|---|---|---|
| 新增 | ~17 | core-session + core-ai + 新 UI 组件 |
| 重构 | ~14 | 导航层 + 首页 + 任务页 + 设置页 + 工具页 |
| 保留不变 | ~102 | KMP + Library + Feature + MVI + Design System |
| **总计** | **~133** | |

---

*本文档为 AppDex 2.0 产品架构师模式完整输出。可直接按 Sprint 0-6 顺序开始执行。*
