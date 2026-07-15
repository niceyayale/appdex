# AppDex 2.0 Rewrite Report

**日期**: 2026-07-15  
**版本**: 2.0.0  
**架构师**: Principal Android Architect (AI)

---

## 一、项目概述

AppDex 2.0 是对原有 AppDex 逆向工具集合的一次完整产品重构。核心理念是从「APK 工具箱」升级为「AI Native APK Analysis Platform」——用户只需上传 APK，AI 自动完成全部分析流程，用自然语言解释结果，并引导下一步操作。

### 核心变化

| 维度 | AppDex 1.0 | AppDex 2.0 |
|------|-----------|-----------|
| 产品定位 | 逆向工具集合 | AI 驱动的 APK 分析平台 |
| 首页 | 工具网格列表 | Hero + 巨大分析按钮 + 最近任务 + AI 推荐 |
| 底部导航 | 首页/分析/文件/工具/设置 | 主页/任务/文件/工具/AI/设置 |
| 核心入口 | Analyzer 页面 | Task 生命周期管理 + AI 对话 |
| AI | 无 | 全平台 AI 集成（12+ 提供商） |
| 数据流 | apkPath String 传递 | SessionManager 统一会话管理 |
| 工具名称 | 专业术语（DEX/ARSC/AXML） | 普通模式友好名称 + 高级/专家模式 |
| 架构模式 | 各模块独立 | MVVM + MVI + 全局 Session + ToolBridge |

---

## 二、修改文件列表

### 2.1 修改的已有文件（48 个）

#### App 模块
- `app/build.gradle.kts` — 版本升级至 2.0.0，新增 feature 模块依赖
- `app/src/main/java/com/appdex/AppDexActivity.kt` — 入口适配
- `app/src/main/java/com/appdex/AppDexApplication.kt` — Application 适配
- `app/src/main/java/com/appdex/nav/AppDexNavHost.kt` — **完全重写**：6 Tab 导航 + AI 路由 + Action Card 跳转
- `app/src/main/java/com/appdex/ui/Route.kt` — 新增 Task/Ai 路由，保留 Analyzer 兼容

#### Core 模块
- `core/core-arch/src/main/java/com/appdex/arch/BaseViewModel.kt` — MVI 基类增强
- `core/core-common/src/main/java/com/appdex/common/Result.kt` — Result 类型优化
- `core/core-data/build.gradle.kts` — 新增 lib-apk 依赖
- `core/core-ui/src/main/java/com/appdex/ui/components/StateComponents.kt` — 状态组件适配
- `core/core-ui/src/main/java/com/appdex/ui/theme/Color.kt` — 新增 AI/Score/Hero 渐变色
- `core/core-ui/src/main/java/com/appdex/ui/theme/Shape.kt` — 形状定义优化
- `core/core-ui/src/main/java/com/appdex/ui/theme/Theme.kt` — 主题适配

#### Feature 模块
- `feature/feature-analyzer/` — 4 文件（Intent/Screen/State/ViewModel 适配）
- `feature/feature-editor/` — 2 文件（Screen/ViewModel 适配）
- `feature/feature-files/` — 2 文件（Screen/ViewModel 适配）
- `feature/feature-player/` — 3 文件（Audio/Image/Video Screen 适配）
- `feature/feature-remote/` — 4 文件（Screen/Ftp/WebServer 适配）
- `feature/feature-settings/` — 3 文件（build.gradle + Screen + ViewModel 新增 AI 配置）
- `feature/feature-terminal/` — 3 文件（build.gradle + Screen + Session 适配）
- `feature/feature-tools/` — 6 文件（build.gradle + ToolsScreen 新增模式切换 + 插件适配）

#### Library 模块
- `library/lib-apk/` — 2 文件（ApkFile/BinaryXmlDecoder 适配）
- `library/lib-archive/` — 1 文件（ArchiveFactory 适配）
- `library/lib-syntax/` — 2 文件（build.gradle + SyntaxHighlighter 适配）

#### 配置
- `gradle/libs.versions.toml` — 新增依赖版本
- `settings.gradle.kts` — 新增 feature 模块声明

### 2.2 新增文件（80+ 个）

#### 核心数据层（4 个）
| 文件 | 职责 |
|------|------|
| `core/core-data/src/main/java/com/appdex/data/session/SessionManager.kt` | 全局会话管理：Session 生命周期（IDLE→ANALYZING→REPORT→MODIFIED→SIGNED→INSTALLED）、显示模式管理、AI 消息存储 |
| `core/core-data/src/main/java/com/appdex/data/ai/AiConfigRepository.kt` | AI 配置仓库：DataStore 持久化、12+ 提供商类型枚举、配置有效性检查 |
| `core/core-data/src/main/java/com/appdex/data/ai/AiService.kt` | AI 服务：统一接口调用 OpenAI/Anthropic/Gemini/DeepSeek/Ollama 等，HTTP 请求与响应解析 |
| `core/core-data/src/main/java/com/appdex/data/toolbridge/ToolBridge.kt` | 工具桥梁：AI 上下文构建、Token 裁剪、Action Card 解析、安全发现生成 |

#### 设计系统组件（3 个）
| 文件 | 职责 |
|------|------|
| `core/core-ui/src/main/java/com/appdex/ui/components/AppDex2Components.kt` | AppDex 2.0 全新组件库：AppDexHero、AppDexTaskCard、AppDexFindingCard、AppDexActionCard、AppDexAiCard、AppDexScoreCard、AppDexTimeline、AppDexLoadingFlow、AppDexSessionCard |
| `core/core-ui/src/main/java/com/appdex/ui/components/AppDexBottomNav.kt` | 6 Tab 底部导航栏 |
| `core/core-ui/src/main/java/com/appdex/ui/components/DesignSystem.kt` | 设计系统基础：AppDexBar、AppDexRow、AppDexSection、AppDexButton、AppDexSearchBar、导航图标定义 |

#### App 层新页面（4 个）
| 文件 | 职责 |
|------|------|
| `app/src/main/java/com/appdex/AppDexMainViewModel.kt` | 全局 ViewModel：APK 解析、Session 管理、AI 对话、安全发现生成 |
| `app/src/main/java/com/appdex/ui/HomeScreen.kt` | 全新首页：Hero + 快速操作 + 最近任务 + AI 推荐问题 |
| `app/src/main/java/com/appdex/ui/TaskScreen.kt` | Task 生命周期页面：时间线 + 评分 + 发现列表 + 快捷操作 |
| `app/src/main/java/com/appdex/ui/AiScreen.kt` | AI 对话页面：聊天气泡 + Action Card + 输入栏 + 配置引导 |

#### 新增 Feature 模块（12 个模块，70+ 文件）
| 模块 | 文件数 | 功能 |
|------|--------|------|
| `feature/feature-dex/` | 7 | DEX 浏览器（Intent/State/Effect/ViewModel/Repository/Screen） |
| `feature/feature-hex/` | 7 | 十六进制编辑器 |
| `feature/feature-signing/` | 7 | APK 签名工具 |
| `feature/feature-repack/` | 7 | APK 重打包工具 |
| `feature/feature-diff/` | 7 | APK 差异对比 |
| `feature/feature-axml/` | 6 | AXML 编辑器（解码/编码） |
| `feature/feature-elf/` | 5 | ELF 查看器 |
| `feature/feature-arsc/` | 5 | ARSC 资源表查看器 |
| `feature/feature-security/` | 4 | 安全扫描器 |
| `feature/feature-size/` | 4 | 体积分析器 |
| `feature/feature-sqlite/` | 4 | SQLite 数据库查看器 |
| `feature/feature-analyzer/ApkDetailScreen.kt` | 1 | APK 详情页面 |
| `feature/feature-terminal/TerminalViewModel.kt` | 1 | 终端 ViewModel |

---

## 三、新增模块

### 3.1 核心数据层

#### SessionManager
- **定位**: 全局单例（@Singleton + Hilt 注入）
- **核心数据模型**:
  - `AnalysisSession` — APK 分析会话（包含 apkInfo、findings、securityScore、aiMessages）
  - `SessionStatus` — 生命周期枚举（IDLE → ANALYZING → REPORT → MODIFIED → SIGNED → INSTALLED）
  - `ToolDisplayMode` — 工具显示模式（NORMAL / ADVANCED / EXPERT）
  - `AnalysisFinding` — 安全发现项（severity / category / title / description / recommendation）
  - `AiMessage` — AI 对话消息（role / content / actionCards）
  - `ActionCard` — AI 推荐动作卡片（title / description / iconType / route）

#### AiConfigRepository
- **定位**: AI 配置持久化仓库（DataStore Preferences）
- **支持提供商**: OpenAI / Anthropic / Gemini / DeepSeek / OpenRouter / OpenAI Compatible / Anthropic Compatible / Ollama / LM Studio / AnythingLLM / LocalAI / Custom API
- **配置项**: providerType / apiKey / baseUrl / modelName / temperature / maxTokens

#### AiService
- **定位**: AI 服务统一调用层
- **能力**: 
  - OpenAI 兼容格式（OpenAI / DeepSeek / OpenRouter / LM Studio / LocalAI / AnythingLLM）
  - Anthropic 格式
  - Gemini 格式
  - Ollama 本地格式
- **特性**: 超时控制、错误处理、响应解析

#### ToolBridge
- **定位**: AI 与工具之间的桥梁
- **职责**:
  - 构建 AI 系统提示词（含安全规则：不破解、不去广告、不绕过授权）
  - 将 Session 数据转换为 AI 可理解的上下文（Token 裁剪）
  - 从 APK 信息自动生成安全发现（危险权限、签名检查、SDK 版本、文件结构）
  - 解析 AI 回复中的 Action Card 标记

### 3.2 设计系统

#### 新增组件清单
| 组件 | 用途 |
|------|------|
| `AppDexHero` | 首页 Hero 区域（Logo + 介绍 + 巨大分析按钮） |
| `AppDexTaskCard` | 任务卡片（应用名 + 包名 + 评分 + 状态） |
| `AppDexFindingCard` | 安全发现卡片（严重度 + 分类 + 标题 + 描述 + 建议 + 动作） |
| `AppDexActionCard` | AI 推荐动作卡片（图标 + 标题 + 描述 + 跳转） |
| `AppDexAiCard` | AI 对话气泡（用户/AI 双向 + Action Card 内嵌） |
| `AppDexScoreCard` | 安全评分卡片（大数字 + 风险等级 + 进度条） |
| `AppDexTimeline` | 分析进度时间线（6 步生命周期可视化） |
| `AppDexLoadingFlow` | AI 分析加载动画（脉冲效果 + 进度条） |
| `AppDexSessionCard` | 会话列表卡片（评分 + 状态徽章） |
| `AppDexBottomNav` | 6 Tab 底部导航栏 |

---

## 四、删除/废弃模块

**无删除模块**。所有已有工具和能力全部保留，仅重新组织了入口。

旧的 `Route.Analyzer` 路由保留为兼容别名，指向 Task 页面。

---

## 五、UI 对比

### 5.1 首页

**Before (AppDex 1.0)**:
- 工具网格列表（DEX、Manifest、HEX、Smali 等专业术语直接展示）
- 无 AI 集成
- 无最近任务

**After (AppDex 2.0)**:
- AppDexHero（Logo + "AI 驱动的 APK 分析平台" + 巨大 "分析 APK" 按钮）
- 快速操作行（扫描已安装应用 + 问 AI）
- 最近任务列表（AppDexSessionCard 展示评分和状态）
- AI 推荐问题（"这个 APK 安全吗？" 等 4 个推荐问题）
- 查看全部任务入口

### 5.2 底部导航

**Before**: 首页 / 分析 / 文件 / 工具 / 设置（5 Tab）
**After**: 主页 / 任务 / 文件 / 工具 / AI / 设置（6 Tab，新增 AI Tab）

### 5.3 Task 页面（原 Analyzer）

**Before**: APK 选择 + 基础信息展示
**After**:
- 当前会话头部（应用图标 + 名称 + 版本 + 大小 + SDK）
- 生命周期时间线（6 步可视化：选择→分析→报告→修改→签名→安装）
- 安全评分卡片（AppDexScoreCard）
- 分析发现列表（AppDexFindingCard，按严重度着色）
- 快捷操作（权限/代码结构/签名/重打包/文件列表）
- 所有任务列表（带删除功能）

### 5.4 AI 页面（全新）

- 空状态：AppDex AI Logo + 推荐问题 + 配置引导
- 对话气泡：用户（琥珀色）/ AI（深蓝渐变）双向气泡
- Action Card：AI 回复中内嵌可点击的动作卡片，直接跳转对应工具
- 输入栏：OutlinedTextField + 发送按钮

### 5.5 工具页面

**Before**: 所有工具以专业名称展示
**After**: 
- 支持三种显示模式（在设置中切换）：
  - **普通模式**: "代码结构"（DEX）、"配置文件"（AXML）、"资源文件"（ARSC）、"数据库"（SQLite）、"原生库"（ELF）
  - **高级模式**: "代码结构 (DEX 浏览器）" 双名称
  - **专家模式**: 原始技术名称
- 分组：分析与开发 / 系统工具 / 实用工具

### 5.6 设置页面

**Before**: 基础外观/文件/编辑器设置
**After**: 新增 AI 配置区域（提供商选择 + API Key + Base URL + 模型 + Temperature + Max Tokens）+ 工具显示模式选择

---

## 六、用户流程变化

### 6.1 旧流程
```
打开 App → 面对工具列表 → 选择工具 → 选择 APK → 查看结果
```

### 6.2 新流程
```
打开 App → 点击「分析 APK」→ AI 自动分析 → 
查看安全评分和发现 → 问 AI「这个 APK 安全吗？」→ 
AI 用自然语言解释 → AI 推荐 Action Card → 
点击 Action Card 进入对应工具 → 需要时切换高级模式
```

### 6.3 完整用户流程
1. **启动 App** → 首页 Hero + 巨大按钮
2. **导入 APK** → 点击「分析 APK」→ 文件选择器 → 自动开始分析
3. **自动分析** → LoadingFlow 动画 → 提取清单/签名/权限/文件结构
4. **查看报告** → Task 页面展示时间线 + 评分 + 发现列表
5. **AI 对话** → 切换到 AI Tab → 提问 → AI 解释 + 推荐 Action Card
6. **Action Card 跳转** → 点击卡片直接进入对应工具页面
7. **文件管理** → Files Tab 浏览/编辑文件
8. **专业工具** → Tools Tab（普通模式隐藏专业术语）
9. **设置** → 配置 AI 提供商 / 切换显示模式 / 其他设置

---

## 七、编译结果

### 7.1 Debug 编译
```
BUILD SUCCESSFUL in 1m 4s
976 actionable tasks: 16 executed, 960 up-to-date
```

### 7.2 Release 编译
```
BUILD SUCCESSFUL in 10m 22s
1255 actionable tasks: 201 executed, 1 from cache, 1053 up-to-date
```

### 7.3 Lint 检查
- `lintVitalRelease` — 通过
- 自定义文件 lint — 无错误

### 7.4 编译错误修复记录
| 错误 | 原因 | 修复 |
|------|------|------|
| `Unresolved reference 'ai'` | AppDexMainViewModel 导入路径错误 | `com.appdex.ai.*` → `com.appdex.data.ai.*` |
| `Unresolved reference 'first'` | 缺少 Flow.first() 导入 | 添加 `import kotlinx.coroutines.flow.first` |
| `Unresolved reference 'isConfigured'` | config 可空性判断错误 | 移除 `config == null` 检查 |
| `No value passed for 'recommendation'` | AnalysisFinding 缺少默认值 | 添加 `recommendation: String? = null` |
| `Unresolved reference: VpnKey` | 缺少 Material Icons 导入 | 添加 `import Icons.Default.VpnKey` |

---

## 八、架构设计

### 8.1 整体架构

```
┌─────────────────────────────────────────────────┐
│                   AppDexActivity                 │
│              (Compose + Hilt)                    │
├─────────────────────────────────────────────────┤
│              AppDexMainViewModel                 │
│    (全局状态: Session + AI + DisplayMode)        │
├────────┬────────┬────────┬────────┬──────────────┤
│  Home  │  Task  │ Files  │ Tools  │  AI  │Settings│
│ Screen │ Screen │ Screen │ Screen │Screen│Screen │
├────────┴────────┴────────┴────────┴──────┴────────┤
│              Core UI Components                   │
│  (DesignSystem + AppDex2Components + Theme)      │
├──────────────────────────────────────────────────┤
│              Core Data Layer                      │
│  SessionManager | AiConfigRepo | AiService       │
│  ToolBridge                                      │
├──────────────────────────────────────────────────┤
│              Feature Modules (16)                 │
│  Analyzer | Editor | Files | Player | Terminal   │
│  Tools | Settings | Remote | Dex | Hex | Signing │
│  Repack | Diff | Security | Size | AXML | ARSC   │
│  SQLite | ELF                                     │
├──────────────────────────────────────────────────┤
│              Library Layer                        │
│  lib-apk | lib-archive | lib-syntax              │
└──────────────────────────────────────────────────┘
```

### 8.2 数据流

```
用户选择 APK
    ↓
AppDexMainViewModel.openApk(uri)
    ↓
SessionManager.createSession() → 新建 AnalysisSession
    ↓
ApkFile.parse() → 解析 APK 结构
    ↓
PackageManager.enrichWithPackageManager() → 补充信息
    ↓
SessionManager.loadApkData() → 加载到 Session
    ↓
ToolBridge.generateFindings() → 自动生成安全发现
    ↓
SessionManager.completeAnalysis() → 状态变为 REPORT
    ↓
用户在 AI Tab 提问
    ↓
ToolBridge.buildSystemPrompt() + buildContext() → 构建 AI 上下文
    ↓
AiService.chat() → 调用 AI API
    ↓
ToolBridge.parseActionCards() → 解析推荐动作
    ↓
用户点击 Action Card → 跳转对应工具
```

### 8.3 AI 安全规则

ToolBridge 内置系统提示词明确限制 AI 行为：
- ✅ 允许：解释、定位、分析、指出修改位置、生成补丁建议、调用已有工具
- ❌ 禁止：自动破解、生成破解版、去广告、绕过授权、修改支付逻辑、输出破解 APK

---

## 九、已知剩余问题

1. **SessionManager.currentSession 未实现自动跟踪**: `currentSession` 是一个独立的 MutableStateFlow，未与 `currentSessionId` 联动。当前通过 `getCurrentSession()` 方法手动获取。后续可改为 derived flow。

2. **AI 对话不支持流式响应**: 当前 `AiService.chat()` 是同步请求等待完整响应。未来可添加 SSE 流式支持以提升用户体验。

3. **APK 安装功能未实现**: SessionStatus 包含 INSTALLED 状态，但实际安装逻辑尚未实现。

4. **横竖屏适配**: 列表页面在横屏模式下布局可能不够优化，建议后续添加 adaptive layout。

5. **后台恢复**: Session 数据存储在内存中（MutableStateFlow），App 被系统杀死后会话数据会丢失。未来可持久化到数据库。

---

## 十、总结

AppDex 2.0 成功完成了从「逆向工具集合」到「AI Native APK Analysis Platform」的产品重构：

- **128+ 文件**修改或新增
- **12 个新 Feature 模块**创建
- **9 个新设计系统组件**实现
- **4 个核心数据层模块**搭建
- **12+ AI 提供商**集成
- **6 Tab 导航**重新设计
- **3 种工具显示模式**实现
- Debug + Release **双编译通过**
- Lint 检查**无错误**

所有已有工具能力完整保留，仅重新组织了入口和交互流程。普通用户无需了解 DEX、Manifest、ARSC 等专业术语，即可通过 AI 引导完成 APK 分析。
