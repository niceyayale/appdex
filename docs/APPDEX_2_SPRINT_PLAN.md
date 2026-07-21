# AppDex 2.0 Sprint 任务拆分

> Phase 3 输出 — AppDex 2.0 产品方向更新

---

## Sprint 总览

| Sprint | 目标 | 预估文件 | 验证标准 |
|---|---|---|---|
| Sprint 0 | 新增 `core-session` 模块 | ~5 新文件 | 编译通过 + 原有功能不受影响 |
| Sprint 1 | 新增 `core-ai` 模块 | ~8 新文件 | 编译通过 + AI 配置可保存 |
| Sprint 2 | 重构首页 `HomeScreen` | 1 重写 + 1 修改 | 安装 APK + 首页无技术术语 + 选择 APK 可进入分析 |
| Sprint 3 | 重构底部导航 + 路由 | 2 修改 | 安装 APK + 5 Tab 全部可进入 + 任务页可显示分析结果 |
| Sprint 4 | 重构任务详情页 | 1 重写 | 安装 APK + 任务页普通模式/高级模式切换 + 所有操作入口可用 |
| Sprint 5 | 扩展设置页 + AI 配置 | 2 修改 | 安装 APK + AI 配置可保存 + 测试连接有反馈 |
| Sprint 6 | AI 助手对话面板 | 2 新增 | 安装 APK + AI 对话可用 + 未配置时引导 |
| Sprint 7 | 工具页分类重构 | 1 修改 | 安装 APK + 三分类清晰 + 所有工具入口可用 |
| Sprint 8 | 最近任务持久化 | 2 修改 + 1 新增 | 安装 APK + 分析后最近任务列表有记录 + 可恢复 |

**执行原则**：每个 Sprint 完成后必须：
1. `./gradlew assembleDebug` 编译通过
2. 安装 APK 到模拟器
3. 真实用户流程测试
4. 记录 PASS/FAIL

---

## Sprint 0：新增 `core-session` 模块

**目标**：建立 APK 分析会话状态管理基础设施

**新增文件**：

| 文件 | 内容 |
|---|---|
| `core/core-session/build.gradle.kts` | 模块配置，依赖 core-arch + lib-apk |
| `core/core-session/src/main/java/com/appdex/session/ApkSession.kt` | sealed class: Idle / Analyzing / Analyzed(info) / Failed(error) / Modified / Signed |
| `core/core-session/src/main/java/com/appdex/session/ApkSessionManager.kt` | @Singleton，StateFlow\<ApkSession\>，set/get 方法 |
| `core/core-session/src/main/java/com/appdex/session/SecurityScoreCalculator.kt` | 安全评分算法（从 ApkDetailScreen.kt 提取） |
| `core/core-session/src/main/java/com/appdex/session/RiskAssessor.kt` | 风险发现生成器（危险权限、签名状态、SDK 版本等） |

**修改文件**：

| 文件 | 修改内容 |
|---|---|
| `settings.gradle.kts` | 新增 `include(":core:core-session")` |
| `app/build.gradle.kts` | 新增 `implementation(project(":core:core-session"))` |
| `app/src/main/java/com/appdex/AppDexActivity.kt` | 可能需要确认 Hilt 能扫描到新模块 |

**验证**：
- [ ] `./gradlew assembleDebug` 编译通过
- [ ] 安装 APK，原有功能不受影响
- [ ] AppDex 正常启动

**不影响旧功能确认**：
- 仅新增文件和模块，不修改任何现有代码
- ApkDetailScreen.kt 的安全评分逻辑暂不替换（Sprint 4 再替换）

---

## Sprint 1：新增 `core-ai` 模块

**目标**：建立 AI Provider 抽象和 HTTP 调用基础设施

**新增文件**：

| 文件 | 内容 |
|---|---|
| `core/core-ai/build.gradle.kts` | 模块配置，依赖 core-arch + core-data + okhttp |
| `core/core-ai/src/main/java/com/appdex/ai/AiProvider.kt` | sealed interface: OpenAI, Anthropic, Custom |
| `core/core-ai/src/main/java/com/appdex/ai/AiConfig.kt` | data class: provider, baseUrl, apiKey, model |
| `core/core-ai/src/main/java/com/appdex/ai/AiConfigRepository.kt` | DataStore 持久化（扩展 SettingsRepository 或独立） |
| `core/core-ai/src/main/java/com/appdex/ai/AiClient.kt` | HTTP 调用封装，支持 OpenAI 和 Anthropic API 协议 |
| `core/core-ai/src/main/java/com/appdex/ai/PromptTemplates.kt` | 预定义 prompt 模板 |
| `core/core-ai/src/main/java/com/appdex/ai/ApkContextBuilder.kt` | ApkInfo → AI 可读文本 |
| `core/core-ai/src/main/java/com/appdex/ai/AiService.kt` | 高层 API: ask(question, apkInfo): Flow\<String\> |

**修改文件**：

| 文件 | 修改内容 |
|---|---|
| `settings.gradle.kts` | 新增 `include(":core:core-ai")` |
| `app/build.gradle.kts` | 新增 `implementation(project(":core:core-ai"))` |
| `gradle/libs.versions.toml` | 新增 okhttp 依赖版本 |

**验证**：
- [ ] `./gradlew assembleDebug` 编译通过
- [ ] 安装 APK，原有功能不受影响
- [ ] AppDex 正常启动

**不影响旧功能确认**：
- 仅新增文件和模块
- AI 配置暂不接入 SettingsScreen（Sprint 5 再接入）

---

## Sprint 2：重构首页 `HomeScreen`

**目标**：首页从"工具箱控制台"变为"任务驱动入口"

**修改文件**：

| 文件 | 修改内容 |
|---|---|
| `app/src/main/java/com/appdex/ui/HomeScreen.kt` | 完全重写：移除技术术语、移除快速工具网格、新增欢迎卡片、新增最近任务列表（暂用空状态）、FAB 改为"选择 APK" |
| `app/src/main/java/com/appdex/nav/AppDexNavHost.kt` | HomeScreen 的 `onNavigate` 回调改为直接路由（不再传 String），文件选择器逻辑移入 HomeScreen |

**验证**：
- [ ] `./gradlew assembleDebug` 编译通过
- [ ] 安装 APK
- [ ] 首页无技术术语（DEX/HEX/Manifest 等）
- [ ] 首页显示"选择 APK 文件"按钮
- [ ] 点击按钮弹出文件选择器
- [ ] 选择 APK 后进入分析流程
- [ ] 分析完成后进入原有 ApkDetailScreen（Sprint 4 再改为任务页）
- [ ] 空状态显示正确
- [ ] 旋转屏幕不崩溃

**不影响旧功能确认**：
- 仅修改 HomeScreen.kt 和 NavHost 中 Home 路由的回调
- 其他所有页面不变
- 文件管理器、工具页、设置页不受影响

---

## Sprint 3：重构底部导航 + 路由

**目标**：底部导航从"主页/分析/文件/工具/设置"变为"首页/任务/文件/工具/设置"

**修改文件**：

| 文件 | 修改内容 |
|---|---|
| `app/src/main/java/com/appdex/ui/Route.kt` | 新增 `Route.TaskWorkbench`；废弃 `Route.ApkDetail`（保留兼容） |
| `app/src/main/java/com/appdex/nav/AppDexNavHost.kt` | 底部导航第 2 项 "分析"→"任务"；导航目标从 `Route.Analyzer` 改为 `Route.TaskWorkbench`；新增 `TaskWorkbench` composable（暂时复用 ApkDetailScreen 内容） |

**验证**：
- [ ] `./gradlew assembleDebug` 编译通过
- [ ] 安装 APK
- [ ] 底部 5 Tab：首页/任务/文件/工具/设置
- [ ] 全部可进入
- [ ] "任务" Tab 显示 APK 详情内容（暂时复用）
- [ ] 从首页选择 APK → 分析 → 自动跳转到"任务" Tab
- [ ] 文件/工具/设置 Tab 不受影响
- [ ] 返回栈正确

**不影响旧功能确认**：
- 所有子路由（Editor/Terminal/DexBrowser 等）不变
- 仅改变 Tab 标签和导航目标
- ApkAnalyzerScreen 和 ApkDetailScreen 的代码暂不修改（Sprint 4 再改）

---

## Sprint 4：重构任务详情页

**目标**：将 ApkDetailScreen 重构为任务页，支持普通模式/高级模式

**修改文件**：

| 文件 | 修改内容 |
|---|---|
| `feature/feature-analyzer/src/main/java/com/appdex/analyzer/ApkDetailScreen.kt` | 重构为 `TaskWorkbenchScreen`：普通模式（安全评分+风险发现+操作中心）+ 高级模式（折叠的 Manifest/DEX/资源/HEX/ELF/SQLite） |
| `app/src/main/java/com/appdex/nav/AppDexNavHost.kt` | `TaskWorkbench` composable 使用新的 Screen |
| `feature/feature-analyzer/src/main/java/com/appdex/analyzer/ApkAnalyzerViewModel.kt` | 注入 `ApkSessionManager`，分析完成后调用 `setAnalyzed()` |

**新增组件**（在 core-ui 中新增，不修改现有组件）：

| 文件 | 内容 |
|---|---|
| `core/core-ui/src/main/java/com/appdex/ui/components/TaskWorkbenchComponents.kt` | `AppDexStatusCard`、`AppDexReportCard`、`AppDexPrimaryAction` |

**验证**：
- [ ] `./gradlew assembleDebug` 编译通过
- [ ] 安装 APK
- [ ] 从首页选择 APK → 分析 → 任务页显示
- [ ] 普通模式显示：应用概览 + 安全评分 + 风险发现 + 操作中心
- [ ] 高级模式默认折叠
- [ ] 展开高级模式可看到 Manifest/DEX/资源/HEX/ELF/SQLite 入口
- [ ] 操作中心每项点击跳转到对应功能页面
- [ ] 签名、重打包、安全扫描入口传递 apkPath
- [ ] 返回后回到任务页

**不影响旧功能确认**：
- 所有功能页面（SigningScreen/RepackagingScreen 等）不变
- ApkAnalyzerScreen（选择 APK 入口）不变
- 仅重构 ApkDetailScreen 的展示层

---

## Sprint 5：扩展设置页 + AI 配置

**目标**：设置页新增 AI 服务配置分区

**修改文件**：

| 文件 | 修改内容 |
|---|---|
| `feature/feature-settings/src/main/java/com/appdex/settings/SettingsScreen.kt` | 新增 "AI 服务" 分区，包含 Provider/Base URL/API Key/Model/测试连接 |
| `feature/feature-settings/src/main/java/com/appdex/settings/SettingsViewModel.kt` | 新增 AI 配置读写方法，注入 `AiConfigRepository` |
| `feature/feature-settings/build.gradle.kts` | 新增 `implementation(project(":core:core-ai"))` |

**新增组件**：

| 文件 | 内容 |
|---|---|
| `core/core-ui/src/main/java/com/appdex/ui/components/AiConfigComponents.kt` | `AppDexAiConfigCard` — AI 配置表单组件 |

**验证**：
- [ ] `./gradlew assembleDebug` 编译通过
- [ ] 安装 APK
- [ ] 设置页显示 "AI 服务" 分区
- [ ] Provider 可选择 OpenAI/Anthropic/Custom
- [ ] Base URL 输入框正常
- [ ] API Key 输入框为密码模式
- [ ] Model 输入框正常
- [ ] 点击"测试连接"有反馈（成功/失败 Toast）
- [ ] 配置退出后重进，数据保持
- [ ] 其他设置项不受影响

**不影响旧功能确认**：
- 仅在 SettingsScreen 末尾新增分区
- 不修改现有配置/语言/编辑器/终端/关于分区

---

## Sprint 6：AI 助手对话面板

**目标**：任务页新增 AI 助手对话入口

**新增文件**：

| 文件 | 内容 |
|---|---|
| `core/core-ui/src/main/java/com/appdex/ui/components/AiAssistantPanel.kt` | AI 对话 BottomSheet 组件：对话气泡 + 输入框 + 快捷问题 |
| `app/src/main/java/com/appdex/nav/AppDexNavHost.kt` | TaskWorkbench Screen 中新增 AI 助手按钮 |

**修改文件**：

| 文件 | 修改内容 |
|---|---|
| `feature/feature-analyzer/src/main/java/com/appdex/analyzer/ApkDetailScreen.kt` | 顶部栏新增 AI 图标按钮，点击展开 BottomSheet |
| `feature/feature-analyzer/build.gradle.kts` | 新增 `implementation(project(":core:core-ai"))` |

**验证**：
- [ ] `./gradlew assembleDebug` 编译通过
- [ ] 安装 APK
- [ ] 任务页顶部有 AI 助手按钮
- [ ] 点击展开 BottomSheet
- [ ] 未配置 AI 时显示 "请在设置中配置 AI 服务" + 跳转按钮
- [ ] 已配置 AI 时可输入问题
- [ ] 快捷问题按钮可用
- [ ] AI 回复以气泡形式显示
- [ ] 可关闭 BottomSheet
- [ ] 任务页其他功能不受影响

**不影响旧功能确认**：
- 仅新增 BottomSheet 组件和按钮
- 不修改任务页主体内容

---

## Sprint 7：工具页分类重构

**目标**：工具页按任务分类重新组织

**修改文件**：

| 文件 | 修改内容 |
|---|---|
| `feature/feature-tools/src/main/java/com/appdex/tools/ToolsScreen.kt` | 重构 3 个分区为 3 个分类：分析工具 / 修改工具 / 开发工具；移除"快速扫描"行；移除"分析"导航入口 |

**验证**：
- [ ] `./gradlew assembleDebug` 编译通过
- [ ] 安装 APK
- [ ] 工具页显示 3 个分类
- [ ] 分析工具：权限检测/安全扫描/包信息/体积分析
- [ ] 修改工具：文件编辑/资源修改/清单编辑/重打包/版本对比
- [ ] 开发工具：DEX 浏览/HEX 编辑/ELF 查看/SQLite 查看/签名验证/终端/远程管理/哈希计算/编码转换/设备信息/插件中心
- [ ] 每项点击跳转到对应页面
- [ ] 所有入口可用，无死路
- [ ] 返回后回到工具页

**不影响旧功能确认**：
- 仅修改 ToolsScreen.kt 的布局
- 所有子工具页面不变
- 插件系统不变

---

## Sprint 8：最近任务持久化

**目标**：首页"最近任务"列表可持久化

**新增文件**：

| 文件 | 内容 |
|---|---|
| `core/core-session/src/main/java/com/appdex/session/RecentTask.kt` | data class: appIcon, appName, packageName, versionName, score, riskLevel, timestamp, apkPath |
| `core/core-session/src/main/java/com/appdex/session/RecentTaskRepository.kt` | Room DAO + Entity，保存最近分析记录 |

**修改文件**：

| 文件 | 修改内容 |
|---|---|
| `app/src/main/java/com/appdex/ui/HomeScreen.kt` | "最近任务" 从空状态变为读取 RecentTaskRepository，显示最近 5 条，点击恢复会话 |
| `feature/feature-analyzer/src/main/java/com/appdex/analyzer/ApkAnalyzerViewModel.kt` | 分析完成后调用 `RecentTaskRepository.save()` |
| `core/core-database/src/main/java/com/appdex/db/AppDexDatabase.kt` | 新增 RecentTaskEntity + DAO |

**验证**：
- [ ] `./gradlew assembleDebug` 编译通过
- [ ] 安装 APK
- [ ] 首次打开首页，"最近任务" 为空状态
- [ ] 分析一个 APK 后，返回首页
- [ ] "最近任务" 列表显示刚分析的 APK
- [ ] 显示应用名、评分、时间
- [ ] 点击列表项跳转到任务页
- [ ] 退出 App 重新进入，记录保持
- [ ] 空状态正确

**不影响旧功能确认**：
- 仅新增 Room Entity 和修改 HomeScreen
- 分析流程的核心逻辑不变

---

## Sprint 执行依赖关系

```
Sprint 0 (core-session)
  ↓
Sprint 1 (core-ai) ← 独立于 Sprint 0，可并行
  ↓
Sprint 2 (首页重构) ← 依赖 Sprint 0（可选，如果首页需要读 Session）
  ↓
Sprint 3 (导航重构) ← 依赖 Sprint 0
  ↓
Sprint 4 (任务页重构) ← 依赖 Sprint 0 + Sprint 3
  ↓
Sprint 5 (AI 配置) ← 依赖 Sprint 1
  ↓
Sprint 6 (AI 助手面板) ← 依赖 Sprint 1 + Sprint 4 + Sprint 5
  ↓
Sprint 7 (工具页分类) ← 独立，可并行
  ↓
Sprint 8 (最近任务) ← 依赖 Sprint 0 + Sprint 4
```

### 并行机会

- Sprint 0 和 Sprint 1 可并行执行
- Sprint 7 可在 Sprint 3 之后任意时间并行执行
- Sprint 8 可在 Sprint 4 之后任意时间并行执行

---

## 最终验证清单（全部 Sprint 完成后）

### 编译验证
- [ ] `./gradlew assembleDebug` 零错误
- [ ] `./gradlew assembleRelease` 零错误（含 R8）

### 安装验证
- [ ] APK 安装成功
- [ ] 启动无崩溃
- [ ] 5 Tab 全部可进入

### 核心用户流程验证
- [ ] 首页 → 选择 APK → 分析 → 任务页 → 查看安全评分 → 查看风险 → 进入操作
- [ ] 首页 → 选择 APK → 分析 → 任务页 → 展开高级模式 → 查看 Manifest
- [ ] 首页 → 选择 APK → 分析 → 任务页 → 进入 DEX 浏览 → 返回
- [ ] 首页 → 选择 APK → 分析 → 任务页 → 进入签名 → 返回
- [ ] 首页 → 选择 APK → 分析 → 任务页 → 进入重打包 → 返回
- [ ] 首页 → 选择 APK → 分析 → 任务页 → 进入安全扫描 → 返回
- [ ] 首页 → 最近任务 → 点击恢复 → 任务页正确显示
- [ ] 工具页 → 3 分类 → 每项可进入
- [ ] 设置页 → AI 配置 → 保存 → 重进保持
- [ ] 设置页 → AI 配置 → 测试连接
- [ ] 任务页 → AI 助手 → 对话
- [ ] 文件管理器 → 打开文件 → Editor → 编辑 → 保存
- [ ] Terminal → 可输入命令
- [ ] Web Server → 可启动
- [ ] Settings → 返回首页

### 稳定性验证
- [ ] 旋转屏幕不崩溃
- [ ] 后台恢复不崩溃
- [ ] 退出重进不丢失状态
- [ ] 无空页面
- [ ] 无无法返回的页面
- [ ] Loading 可消失
- [ ] Error 可恢复

---

## 文件变更总览

### 新增文件（~20）

| 路径 | Sprint |
|---|---|
| `core/core-session/build.gradle.kts` | S0 |
| `core/core-session/.../ApkSession.kt` | S0 |
| `core/core-session/.../ApkSessionManager.kt` | S0 |
| `core/core-session/.../SecurityScoreCalculator.kt` | S0 |
| `core/core-session/.../RiskAssessor.kt` | S0 |
| `core/core-session/.../RecentTask.kt` | S8 |
| `core/core-session/.../RecentTaskRepository.kt` | S8 |
| `core/core-ai/build.gradle.kts` | S1 |
| `core/core-ai/.../AiProvider.kt` | S1 |
| `core/core-ai/.../AiConfig.kt` | S1 |
| `core/core-ai/.../AiConfigRepository.kt` | S1 |
| `core/core-ai/.../AiClient.kt` | S1 |
| `core/core-ai/.../PromptTemplates.kt` | S1 |
| `core/core-ai/.../ApkContextBuilder.kt` | S1 |
| `core/core-ai/.../AiService.kt` | S1 |
| `core/core-ui/.../TaskWorkbenchComponents.kt` | S4 |
| `core/core-ui/.../AiConfigComponents.kt` | S5 |
| `core/core-ui/.../AiAssistantPanel.kt` | S6 |

### 修改文件（~8）

| 路径 | Sprint | 修改程度 |
|---|---|---|
| `settings.gradle.kts` | S0, S1 | 新增 2 行 include |
| `app/build.gradle.kts` | S0, S1 | 新增 2 行 dependency |
| `gradle/libs.versions.toml` | S1 | 新增 okhttp 版本 |
| `app/.../ui/HomeScreen.kt` | S2, S8 | 重写 |
| `app/.../ui/Route.kt` | S3 | 新增路由 |
| `app/.../nav/AppDexNavHost.kt` | S2, S3, S4, S6 | 中等重构 |
| `feature/.../analyzer/ApkDetailScreen.kt` | S4, S6 | 重构 |
| `feature/.../analyzer/ApkAnalyzerViewModel.kt` | S4, S8 | 轻度修改 |
| `feature/.../settings/SettingsScreen.kt` | S5 | 轻度扩展 |
| `feature/.../settings/SettingsViewModel.kt` | S5 | 轻度扩展 |
| `feature/.../settings/build.gradle.kts` | S5 | 新增 1 行 |
| `feature/.../analyzer/build.gradle.kts` | S6 | 新增 1 行 |
| `feature/.../tools/ToolsScreen.kt` | S7 | 中等重构 |
| `core/core-database/.../AppDexDatabase.kt` | S8 | 轻度扩展 |

### 不变文件

- 全部 KMP 解析引擎模块（core/apk, core/arsc, core/axml, core/dex, core/io）
- 全部 Library 模块（lib-apk, lib-archive, lib-syntax）
- 全部 Feature 模块的 Screen（除 ApkDetailScreen 和 ToolsScreen）
- 全部 Feature 模块的 ViewModel（除 ApkAnalyzerViewModel 和 SettingsViewModel）
- 全部 Feature 模块的 Repository
- Design System（Color.kt, Shape.kt, Theme.kt）
- 现有组件（DesignSystem.kt, StateComponents.kt, AppDexBottomNav.kt）
- MVI 架构（BaseViewModel.kt, MviIntent.kt, MviState.kt, MviEffect.kt）

---

*本文档为 AppDex 2.0 产品方向更新的第三阶段输出。可直接按 Sprint 顺序开始执行。*
