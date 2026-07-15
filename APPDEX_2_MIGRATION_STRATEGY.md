# AppDex 2.0 迁移策略

> **本文档不写代码。不修改文件。只做架构迁移规划。**
>
> 现实约束：AppDex 不是最高优先级项目。开发资源有限。个人开发者维护开源项目的节奏。
>
> 核心原则：渐进式升级。不推倒重写。不破坏已有功能。

---

## 目录

- [第一部分：当前架构评估](#第一部分当前架构评估)
- [第二部分：最小可行升级路线（MVP）](#第二部分最小可行升级路线mvp)
- [第三部分：分阶段迁移路线](#第三部分分阶段迁移路线)
- [第四部分：代码保护策略](#第四部分代码保护策略)
- [第五部分：AI 与现有功能连接方式](#第五部分ai-与现有功能连接方式)
- [第六部分：Session 系统迁移](#第六部分session-系统迁移)
- [第七部分：UI 迁移策略](#第七部分ui-迁移策略)
- [第八部分：开发优先级](#第八部分开发优先级)
- [第九部分：最终目标](#第九部分最终目标)
- [迁移路线图](#迁移路线图)

---

---

## 第一部分：当前架构评估

### 1.1 评估标准

每个模块按以下四个等级分类：

| 等级 | 含义 | 操作 |
|---|---|---|
| **A** | 无需修改 | 完全保留，不动一行代码 |
| **B** | 增加入口 | 在 UI 层新增 AI 引导入口，内部代码不动 |
| **C** | 增加AI接口 | 新增一个 Wrapper/Facade，让 AI 能读取该模块的数据 |
| **D** | 未来重构 | 暂不动，后续有需要时再重构 |

### 1.2 模块评估总表

#### Core 模块

| 模块 | 当前作用 | 2.0 角色 | 等级 | 说明 |
|---|---|---|---|---|
| `core-arch` | MVI 基础设施：BaseViewModel、MviIntent、MviState、MviEffect | 不变，AI 的 ViewModel 也继承这套基础设施 | **A** | 不动 |
| `core-ui` | 设计系统：Color/Shape/Theme + 17 个组件 | 不变，新增 AI 相关组件时只新增不修改 | **A** | 不动现有组件，可以新增组件文件 |
| `core-data` | DataStore 持久化：SettingsRepository、BookmarkRepository | 扩展：新增 AI 配置持久化字段 | **B** | 在 SettingsRepository 中新增 AI 相关 PreferencesKey，不改现有字段 |
| `core-model` | 数据模型：FileItem、Bookmark、FileOperation | 扩展：新增 AI 相关数据模型 | **A** | 现有模型不动，新增模型放新模块 |
| `core-common` | 工具类：FormatUtil、MediaNavigationBus、Result | 不变 | **A** | 不动 |
| `core-plugin` | 插件系统：PluginManager、Plugin 接口 | 不变 | **A** | 不动 |
| `core-database` | Room 数据库：AppDexDatabase | 扩展：新增最近任务表 | **B** | 新增 Entity + DAO，不改现有表 |

#### KMP 解析引擎

| 模块 | 当前作用 | 2.0 角色 | 等级 | 说明 |
|---|---|---|---|---|
| `core/apk` | APK ZIP 读取、签名块解析 | 不变，AI 通过 ToolBridge 间接调用 | **A** | 不动 |
| `core/arsc` | resources.arsc 二进制解析 | 不变 | **A** | 不动 |
| `core/axml` | Binary XML 解码/编码 | 不变 | **A** | 不动 |
| `core/dex` | DEX 文件解析 | 不变 | **A** | 不动 |
| `core/io` | 底层 I/O 工具 | 不变 | **A** | 不动 |

#### Library 模块

| 模块 | 当前作用 | 2.0 角色 | 等级 | 说明 |
|---|---|---|---|---|
| `lib-apk` | ApkFile、ApkModels、BinaryXmlDecoder | 不变，AI 的上下文构建器读取 ApkInfo | **A** | 不动 |
| `lib-archive` | ArchiveFactory — ZIP/压缩处理 | 不变 | **A** | 不动 |
| `lib-syntax` | SyntaxHighlighter、SyntaxEngine | 不变 | **A** | 不动 |

#### Feature 模块

| 模块 | 当前作用 | 2.0 角色 | 等级 | 说明 |
|---|---|---|---|---|
| `feature-analyzer` | APK 选择 + 分析 + 详情页 | 核心改造区：详情页重构为任务页 | **D** | ApkAnalyzerScreen 保留不变；ApkDetailScreen 未来重构为任务页 |
| `feature-files` | 文件浏览器 | 不变 | **A** | 不动 |
| `feature-editor` | 文本编辑器 | 不变，AI Action Card 可跳转到这里 | **B** | Screen 不动，NavHost 中新增跳转入口 |
| `feature-settings` | 应用设置 | 扩展：新增 AI 配置分区 | **B** | SettingsScreen 末尾新增分区，不改现有分区 |
| `feature-tools` | 工具列表 | 未来重构分类 | **D** | 暂不动，Phase 4 再改 |
| `feature-terminal` | 终端 | 不变 | **A** | 不动 |
| `feature-remote` | FTP + Web 服务器 | 不变 | **A** | 不动 |
| `feature-player` | 音频/图片/视频播放 | 不变 | **A** | 不动 |
| `feature-dex` | DEX 浏览器 | 不变，AI 可通过 ToolBridge 读取数据 | **C** | Repository 不动，新增 ToolBridge Wrapper |
| `feature-hex` | HEX 编辑器 | 不变 | **A** | 不动 |
| `feature-signing` | 签名工具 | 不变，AI Action Card 可跳转 | **B** | Screen 不动 |
| `feature-repack` | 重打包 | 不变，AI Action Card 可跳转 | **B** | Screen 不动 |
| `feature-diff` | APK 对比 | 不变 | **A** | 不动 |
| `feature-security` | 安全扫描 | 不变，AI 通过 ToolBridge 读取扫描结果 | **C** | Repository 不动，新增 ToolBridge Wrapper |
| `feature-size` | 体积分析 | 不变 | **A** | 不动 |
| `feature-axml` | AXML 编辑器 | 不变 | **A** | 不动 |
| `feature-arsc` | ARSC 编辑器 | 不变 | **A** | 不动 |
| `feature-sqlite` | SQLite 查看器 | 不变 | **A** | 不动 |
| `feature-elf` | ELF 查看器 | 不变 | **A** | 不动 |

#### App 模块

| 文件 | 当前作用 | 2.0 角色 | 等级 | 说明 |
|---|---|---|---|---|
| `Route.kt` | 路由定义 | 新增 AI 相关路由 | **B** | 在 sealed interface 中新增 Route，不改现有 |
| `AppDexNavHost.kt` | 导航图 | 新增路由注册 + 调整底部导航 | **B** | 新增 composable 注册，不改现有路由 |
| `HomeScreen.kt` | 首页 | 完全重写 | **D** | Phase 2 重写 |
| `AppDexActivity.kt` | 单 Activity 入口 | 可能需要确认 Hilt 扫描 | **A** | 基本不动 |
| `AppDexApplication.kt` | Hilt Application | 不变 | **A** | 不动 |

### 1.3 评估总结

| 等级 | 模块数 | 占比 |
|---|---|---|
| **A: 无需修改** | 26 | 76% |
| **B: 增加入口** | 7 | 21% |
| **C: 增加AI接口** | 2 | 3% |
| **D: 未来重构** | 2 | — |

**结论：** 76% 的模块完全不动。只有 2 个模块需要"未来重构"（HomeScreen 和 ApkDetailScreen），且这两个重构可以分阶段进行，不影响其他模块。

---

---

## 第二部分：最小可行升级路线（MVP）

### 2.1 MVP 定义

**目标：** 在不破坏任何现有功能的前提下，让 AppDex 具备最基本的 AI 能力。

**MVP 包含 6 个功能点：**

1. AI 配置页面（Provider/URL/Key/Model）
2. AI 聊天入口（对话框 + 流式回复）
3. APK 上下文生成（把 ApkInfo 转成 AI 可读文本）
4. AI 总结分析（分析完成后自动生成自然语言总结）
5. Action Card 跳转（AI 回复中的操作按钮，点击跳转到现有工具）
6. 首页重新布局（AI 欢迎卡 + 最近任务列表）

### 2.2 MVP 文件变更清单

**约束：最多修改 10 个文件。**

| 序号 | 文件 | 操作 | 改动说明 |
|---|---|---|---|
| 1 | `settings.gradle.kts` | 修改 | 新增 2 行 `include(":core:core-ai")` 和 `include(":core:core-session")` |
| 2 | `app/build.gradle.kts` | 修改 | 新增 2 行 `implementation(project(":core:core-ai"))` 和 `implementation(project(":core:core-session"))` |
| 3 | `gradle/libs.versions.toml` | 修改 | 新增 okhttp 依赖版本（或复用已有 ktor-client） |
| 4 | `app/.../ui/Route.kt` | 修改 | 新增 `Route.AiAssistant` 和 `Route.AiConfig`，不改现有路由 |
| 5 | `app/.../nav/AppDexNavHost.kt` | 修改 | 新增 2 个 composable 注册；HomeScreen 的 onNavigate 增加新路由 |
| 6 | `app/.../ui/HomeScreen.kt` | 重写 | 从工具箱首页变为 AI 欢迎卡首页 |
| 7 | `feature/.../settings/SettingsScreen.kt` | 修改 | 末尾新增 AI 配置分区 |
| 8 | `feature/.../settings/SettingsViewModel.kt` | 修改 | 新增 AI 配置读写方法 |
| 9 | `feature/.../analyzer/ApkDetailScreen.kt` | 修改 | 顶部栏新增 AI 助手按钮，点击弹出 BottomSheet |
| 10 | `feature/.../analyzer/build.gradle.kts` | 修改 | 新增 `implementation(project(":core:core-ai"))` |

**新增文件（不计入"修改"）：**

| 序号 | 文件 | 说明 |
|---|---|---|
| N1 | `core/core-ai/build.gradle.kts` | 新模块配置 |
| N2 | `core/core-ai/.../AiProvider.kt` | Provider 枚举 |
| N3 | `core/core-ai/.../AiConfig.kt` | 配置数据类 |
| N4 | `core/core-ai/.../AiConfigRepository.kt` | DataStore 持久化 |
| N5 | `core/core-ai/.../AiClient.kt` | HTTP 调用封装 |
| N6 | `core/core-ai/.../ApkContextBuilder.kt` | ApkInfo → AI 上下文文本 |
| N7 | `core/core-ai/.../AiService.kt` | 高层 API：ask(question, context): Flow<String> |
| N8 | `core/core-ai/.../ToolBridge.kt` | AI 与 AppDex 能力的桥接层 |
| N9 | `core/core-session/build.gradle.kts` | 新模块配置 |
| N10 | `core/core-session/.../ApkSession.kt` | 会话状态 sealed class |
| N11 | `core/core-session/.../ApkSessionManager.kt` | @Singleton StateFlow |
| N12 | `core/core-ui/.../AiAssistantPanel.kt` | AI 对话 BottomSheet 组件 |
| N13 | `core/core-ui/.../AiConfigCard.kt` | AI 配置表单组件 |

### 2.3 MVP 不包含

以下功能**不在 MVP 范围内**，留到后续阶段：

- ❌ 三级模式切换（普通/高级/专家）
- ❌ Prompt Library（100 个推荐问题）
- ❌ 55 个 Action Card 的完整实现
- ❌ 最近任务持久化（Room 表）
- ❌ 工具页分类重构
- ❌ 底部导航标签变更
- ❌ ApkDetailScreen 的普通/高级模式拆分
- ❌ MCP 协议
- ❌ Agent 能力

### 2.4 MVP 验证标准

| 验证项 | 标准 |
|---|---|
| 编译 | `./gradlew assembleDebug` 零错误 |
| 原有功能 | 所有 19 个 feature 模块功能不受影响 |
| 首页 | 显示 AI 欢迎卡，不再显示技术术语 |
| AI 配置 | 设置页可配置 Provider/URL/Key/Model，测试连接成功 |
| AI 对话 | 在 APK 详情页点击 AI 按钮，弹出对话框，可输入问题并获得流式回复 |
| AI 总结 | 配置 AI 后，分析 APK 完成后自动生成一段总结 |
| Action Card | AI 回复中包含可点击的操作按钮，点击跳转到对应工具 |
| 无 AI 配置时 | 显示"请配置 AI 服务"引导，不崩溃 |

### 2.5 MVP 的关键设计决策

**为什么用 ktor 而不是 okhttp？**
项目中已经有 ktor 依赖（`libs.versions.toml` 中的 `ktor = "3.0.3"`），且 ktor 支持 SSE 流式响应。不需要引入新依赖。

**为什么不新增 feature-ai-assistant 模块？**
MVP 阶段 AI 对话 UI 只是 `core-ui` 中的一个 BottomSheet 组件，不需要独立模块。等功能复杂后再提取。

**为什么不新增 feature-home 模块？**
HomeScreen 已经在 `app` 模块中，重写它不涉及模块拆分风险。等后续再考虑提取。

**为什么 core-session 是单独模块？**
ApkSessionManager 需要是 @Singleton，被多个 ViewModel 共享。放在 core-session 中可以让 app 模块和 feature-analyzer 模块都依赖它。

---

---

## 第三部分：分阶段迁移路线

### Phase 0：基础设施

**目标：** 新增 `core-ai` 和 `core-session` 模块骨架，编译通过。

**修改范围：**

| 文件 | 操作 |
|---|---|
| `settings.gradle.kts` | +2 行 include |
| `app/build.gradle.kts` | +2 行 implementation |
| `gradle/libs.versions.toml` | 确认 ktor 依赖已存在（已有），无需新增 |

**新增文件：**

| 文件 | 内容 |
|---|---|
| `core/core-ai/build.gradle.kts` | 模块配置，依赖 core-arch + core-data + ktor-client |
| `core/core-ai/.../AiProvider.kt` | sealed interface: OpenAI / Anthropic / OpenAICompatible / Custom |
| `core/core-ai/.../AiConfig.kt` | data class: provider, baseUrl, apiKey, model, temperature |
| `core/core-ai/.../AiConfigRepository.kt` | DataStore 持久化 AI 配置 |
| `core/core-session/build.gradle.kts` | 模块配置，依赖 core-arch + lib-apk |
| `core/core-session/.../ApkSession.kt` | sealed class: Idle / Analyzing / Analyzed / Failed |
| `core/core-session/.../ApkSessionManager.kt` | @Singleton, StateFlow<ApkSession> |

**风险：** 极低。纯新增文件，不触碰任何现有代码。

**验证方式：**
- `./gradlew assembleDebug` 编译通过
- 安装 APK，AppDex 正常启动
- 所有原有功能不受影响（逐一测试底部导航 5 Tab + 工具页入口）

**预估工作量：** 1-2 个开发时段（每时段约 2-3 小时）

---

### Phase 1：AI 接入

**目标：** 实现 AI 聊天核心能力。用户可以配置 AI、测试连接、在 APK 详情页与 AI 对话。

**修改范围（7 个现有文件）：**

| 文件 | 操作 |
|---|---|
| `app/.../ui/Route.kt` | 新增 Route.AiAssistant, Route.AiConfig |
| `app/.../nav/AppDexNavHost.kt` | 新增 composable 注册 |
| `feature/.../settings/SettingsScreen.kt` | 末尾新增 AI 配置分区 |
| `feature/.../settings/SettingsViewModel.kt` | 新增 AI 配置读写方法 |
| `feature/.../settings/build.gradle.kts` | +1 行 implementation |
| `feature/.../analyzer/ApkDetailScreen.kt` | 顶部栏新增 AI 按钮 |
| `feature/.../analyzer/build.gradle.kts` | +1 行 implementation |

**新增文件：**

| 文件 | 内容 |
|---|---|
| `core/core-ai/.../AiClient.kt` | HTTP 调用封装，支持 OpenAI 和 Anthropic 协议，SSE 流式 |
| `core/core-ai/.../ApkContextBuilder.kt` | ApkInfo → AI 可读文本 |
| `core/core-ai/.../AiService.kt` | 高层 API: ask(question, apkInfo): Flow<String> |
| `core/core-ui/.../AiAssistantPanel.kt` | AI 对话 BottomSheet |
| `core/core-ui/.../AiConfigCard.kt` | AI 配置表单组件 |

**风险：** 低。SettingsScreen 只在末尾追加，不改现有分区。ApkDetailScreen 只加一个 IconButton，不改主体布局。

**验证方式：**
- 编译通过
- 设置页显示 AI 配置分区，可保存配置
- 测试连接成功/失败有反馈
- APK 详情页点击 AI 按钮弹出对话框
- 未配置 AI 时显示引导
- 已配置 AI 时可对话，流式回复
- 原有设置项不受影响

**预估工作量：** 3-4 个开发时段

---

### Phase 2：首页改造

**目标：** 首页从"工具箱控制台"变为"AI 欢迎入口"。

**修改范围（2 个现有文件）：**

| 文件 | 操作 |
|---|---|
| `app/.../ui/HomeScreen.kt` | 完全重写 |
| `app/.../nav/AppDexNavHost.kt` | HomeScreen 的 onNavigate 回调更新 |

**重写后的首页结构：**
1. AI 欢迎卡（"你好，我可以帮你分析 APK" + 选择按钮）
2. 最近任务列表（暂时空状态，Phase 3 接入持久化）
3. FAB（选择 APK）

**关键决策：**
- 不在首页放任何技术术语（DEX、HEX、Manifest 等）
- 不在首页放快速工具网格（移到工具 Tab）
- 首页的"选择 APK"按钮触发与之前 FAB 相同的逻辑（跳转 Analyzer）

**风险：** 中低。重写 HomeScreen 是最大改动，但 HomeScreen 是独立文件，不影响其他模块。如果出问题，回退即可。

**验证方式：**
- 编译通过
- 首页无技术术语
- 点击"选择 APK"弹出文件选择器
- 选择 APK 后进入分析流程（不变）
- FAB 功能正常
- 其他 Tab 不受影响

**预估工作量：** 1-2 个开发时段

---

### Phase 3：任务页改造

**目标：** ApkDetailScreen 增加普通模式/高级模式切换 + AI 总结卡。

**修改范围（2 个现有文件）：**

| 文件 | 操作 |
|---|---|
| `feature/.../analyzer/ApkDetailScreen.kt` | 中等重构：新增普通模式层（安全评分 + AI 总结 + 操作中心），原有 6 Tab 变为高级模式 |
| `feature/.../analyzer/ApkAnalyzerViewModel.kt` | 分析完成后调用 ApkSessionManager.setAnalyzed() |

**新增文件：**

| 文件 | 内容 |
|---|---|
| `core/core-ui/.../TaskWorkbenchComponents.kt` | AppDexStatusCard、AppDexReportCard 等新组件 |

**改造策略：**
- ApkDetailScreen 现有代码**不删除**，用 `if (normalMode) { 新UI } else { 原UI }` 包裹
- 默认显示普通模式（安全评分 + AI 总结 + 操作中心）
- 普通模式下方有"高级分析"折叠区，展开后显示原有 6 Tab
- 安全评分算法从 Screen 中提取到 core-session 的 SecurityScoreCalculator

**风险：** 中。这是改动最大的文件。需要仔细测试所有原有 Tab 功能。

**验证方式：**
- 编译通过
- 分析 APK 后进入任务页
- 普通模式显示：安全评分 + 风险发现 + AI 总结
- 高级模式可展开，展开后显示原有 6 Tab
- 所有原有 Tab 功能正常（Manifest/DEX/资源/签名/文件）
- 操作中心每项点击跳转到对应工具
- AI 总结在配置 AI 后自动生成

**预估工作量：** 3-4 个开发时段

---

### Phase 4：工具隐藏和分类

**目标：** 工具页按分类重新组织。底部导航标签从"分析"改为"任务"。

**修改范围（3 个现有文件）：**

| 文件 | 操作 |
|---|---|
| `feature/.../tools/ToolsScreen.kt` | 重构为三分类：分析工具 / 修改工具 / 开发工具 |
| `app/.../nav/AppDexNavHost.kt` | 底部导航第 2 项标签"分析"→"任务" |
| `core/core-ui/.../AppDexBottomNav.kt` | 标签更新 |

**风险：** 低。ToolsScreen 重构只改布局，不改跳转逻辑。底部导航只改标签文字。

**验证方式：**
- 工具页显示 3 个分类
- 每项点击跳转正常
- 底部导航"任务"Tab 可进入
- 其他 Tab 不受影响

**预估工作量：** 1 个开发时段

---

### Phase 5：高级 Agent 能力

**目标：** 实现 Action Card 系统、Prompt Library、最近任务持久化、三级模式。

**修改范围：**

| 文件 | 操作 |
|---|---|
| `core/core-ai/.../ToolBridge.kt` | 实现 AI → AppDex 能力调度 |
| `core/core-ai/.../PromptLibrary.kt` | 100 个推荐问题 |
| `core/core-ai/.../ActionCardRegistry.kt` | 55 个 Action Card 注册 |
| `core/core-session/.../RecentTaskRepository.kt` | Room 持久化最近任务 |
| `core/core-database/.../AppDexDatabase.kt` | 新增 RecentTask Entity + DAO |
| `app/.../ui/HomeScreen.kt` | 最近任务列表接入持久化 |
| `feature/.../analyzer/ApkDetailScreen.kt` | AI 回复中渲染 Action Card |
| `feature/.../settings/SettingsScreen.kt` | 新增模式切换选项 |

**风险：** 中。涉及数据库 Schema 变更，需要处理迁移。

**验证方式：**
- AI 回复中出现可点击的 Action Card
- 点击 Action Card 跳转到正确工具并传递参数
- Prompt Library 可点击发送
- 分析后最近任务列表有记录
- 退出重进记录保持
- 三级模式可切换

**预估工作量：** 4-6 个开发时段

---

### Phase 总结

| Phase | 目标 | 修改文件数 | 新增文件数 | 风险 | 工作量 |
|---|---|---|---|---|---|
| Phase 0 | 基础设施 | 2 | 7 | 极低 | 1-2 时段 |
| Phase 1 | AI 接入 | 7 | 5 | 低 | 3-4 时段 |
| Phase 2 | 首页改造 | 2 | 0 | 中低 | 1-2 时段 |
| Phase 3 | 任务页改造 | 2 | 1 | 中 | 3-4 时段 |
| Phase 4 | 工具分类 | 3 | 0 | 低 | 1 时段 |
| Phase 5 | 高级能力 | 8 | 4 | 中 | 4-6 时段 |
| **合计** | — | **24** | **17** | — | **13-19 时段** |

**按个人开发者每周投入 2-3 个开发时段计算：**
- 最快：约 5-6 周
- 正常：约 7-9 周
- 保守：约 10-12 周

---

---

## 第四部分：代码保护策略

### 4.1 核心原则

**旧功能稳定是第一优先级。任何改动都不能导致已有功能损坏。**

### 4.2 四条红线

| 红线 | 内容 | 原因 |
|---|---|---|
| 红线 1 | **禁止删除旧 Screen** | 所有 feature 模块的 Screen 文件保留，包括 ApkDetailScreen 的原有代码 |
| 红线 2 | **禁止修改核心 Repository** | SecurityScannerRepository、DexRepository、SigningRepository 等的公共方法签名不变 |
| 红线 3 | **禁止改变解析引擎** | core/apk、core/arsc、core/axml、core/dex、core/io 和 lib-apk 不动一行代码 |
| 红线 4 | **禁止修改 MVI 架构** | BaseViewModel、MviIntent、MviState、MviEffect 接口不变 |

### 4.3 保护手段

#### 手段 1：新增 Wrapper，不改原有

**原则：** AI 需要读取某个 Repository 的数据时，不直接调用 Repository，而是新建一个 Wrapper。

```
// ❌ 错误：AI 直接依赖 SecurityScannerRepository
AiService → SecurityScannerRepository.scan()

// ✅ 正确：通过 ToolBridge Wrapper
AiService → ToolBridge → SecurityScannerRepository.scan()
```

**为什么：**
- SecurityScannerRepository 的方法签名不变，不影响 SecurityScannerScreen
- ToolBridge 可以做数据裁剪（去掉不需要的字段，避免上下文过大）
- ToolBridge 可以做错误处理（Repository 抛异常时返回空结果而不是崩溃）
- 未来如果 Repository 重构，只需要改 ToolBridge

#### 手段 2：新增 Service，不改 ViewModel

**原则：** AI 逻辑放在新的 Service/ViewModel 中，不修改现有 ViewModel。

```
// ❌ 错误：在 ApkAnalyzerViewModel 中加 AI 逻辑
ApkAnalyzerViewModel.handleIntent(OpenApk) {
    ...
    aiService.ask(summary)  // 混入 AI 逻辑
}

// ✅ 正确：ApkAnalyzerViewModel 只管分析，AI 逻辑在别处
ApkAnalyzerViewModel.handleIntent(OpenApk) {
    ...
    sessionManager.setAnalyzed(apkInfo)  // 只通知 Session
}

// AI Service 监听 Session 变化，自动触发
AiService.observeSession() → when(Analyzed) → ask(summary)
```

#### 手段 3：新增 Facade，不改 UI 组件

**原则：** 新增 UI 组件，不修改现有组件。

```
// ❌ 错误：修改 AppDexBar 加 AI 按钮
AppDexBar(title, back, onBack, actions = { AIButton() })  // 改了组件签名

// ✅ 正确：在 Screen 中组合使用，不改组件
ApkDetailScreen {
    AppDexBar(title, back = true, onBack)
    IconButton(onClick = { showAiPanel = true }) { AIIcon() }  // 在 Screen 层加
}
```

#### 手段 4：新增路由，不改现有路由

**原则：** Route.kt 只增不删不改。

```
// ✅ 正确：sealed interface 中只新增
sealed interface Route {
    // 现有路由全部保留
    data object Home : Route
    data object Analyzer : Route
    ...

    // 新增路由
    data object AiAssistant : Route
    data object AiConfig : Route
}
```

### 4.4 修改前必须确认的检查清单

每次修改现有文件前，确认以下事项：

| 检查项 | 要求 |
|---|---|
| 是否删除了任何现有函数/类？ | ❌ 不能删除 |
| 是否修改了任何现有函数签名？ | ❌ 不能修改签名 |
| 是否修改了任何现有 UI 组件的参数？ | ❌ 不能修改参数 |
| 新增代码是否在文件末尾/独立区域？ | ✅ 应该是 |
| 原有功能是否仍然可达？ | ✅ 必须可达 |
| 编译是否通过？ | ✅ 必须通过 |
| 是否测试了受影响的功能？ | ✅ 必须测试 |

### 4.5 回退策略

每个 Phase 完成后，如果发现问题：

1. **立即回退：** `git checkout` 回退到上一个稳定 commit
2. **隔离问题：** 在新分支上复现问题
3. **修复后重试：** 修复后再合入

**不要在主干上边修边测。每个 Phase 在独立分支上完成，测试通过后再合入。**

---

---

## 第五部分：AI 与现有功能连接方式

### 5.1 架构设计

```
┌──────────────────────────────────────────────────────────┐
│                     用户                                  │
│                   ↓ 提问                                  │
│              ┌───────────────┐                           │
│              │  AI Chat UI    │  ← BottomSheet 对话界面    │
│              └───────┬───────┘                           │
│                      ↓                                   │
│              ┌───────────────┐                           │
│              │  AiService     │  ← 高层 API               │
│              │  ask(question, │                           │
│              │  apkInfo)       │                           │
│              └───────┬───────┘                           │
│                      ↓                                   │
│         ┌────────────────────────┐                       │
│         │    ApkContextBuilder     │  ← 组装上下文         │
│         │  ApkInfo → 文本          │                       │
│         └─────────────┬──────────┘                       │
│                       ↓                                   │
│              ┌───────────────┐                           │
│              │   AiClient     │  ← HTTP 调用 LLM API      │
│              │   (ktor SSE)   │                           │
│              └───────┬───────┘                           │
│                      ↓                                   │
│              ┌───────────────┐                           │
│              │  LLM Provider  │  ← OpenAI/Anthropic/...  │
│              │  (用户配置)     │                           │
│              └───────────────┘                           │
│                                                          │
│  ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─  │
│                  ToolBridge 分界线                        │
│  ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─  │
│                                                          │
│              ┌───────────────┐                           │
│              │  ToolBridge    │  ← AI 调用 AppDex 能力   │
│              └───────┬───────┘                           │
│          ┌───────────┼───────────┐                       │
│          ↓           ↓           ↓                       │
│  ┌───────────┐ ┌───────────┐ ┌───────────┐             │
│  │Security   │ │Dex        │ │ApkFile    │              │
│  │Scanner    │ │Repository │ │(lib-apk)  │              │
│  │Repository │ │           │ │           │              │
│  └───────────┘ └───────────┘ └───────────┘             │
│  (现有，不动)   (现有，不动)   (现有，不动)                │
└──────────────────────────────────────────────────────────┘
```

### 5.2 为什么不让 AI 直接调用 Feature

**如果 AI 直接调用 Repository：**

```
AiService → SecurityScannerRepository.scan(apkPath)
AiService → DexRepository.listClasses(apkPath, dexName)
AiService → ApkFile(path).parse()
```

**问题：**
1. AiService 直接依赖所有 Repository，耦合度高
2. Repository 返回的原始数据可能很大（如 DEX 有 8000+ 个类），直接发给 LLM 会超 Token 限制
3. Repository 抛异常时，AiService 需要处理所有异常类型
4. 未来 Repository 重构时，AiService 也要改
5. 无法统一控制 AI 能访问哪些数据（安全边界模糊）

### 5.3 ToolBridge 设计

**ToolBridge 是 AI 和 AppDex 之间的中间层。**

```
ToolBridge
├── getManifestSummary(apkPath): String        // 返回精简的 Manifest 摘要
├── getPermissionList(apkPath): List<String>   // 返回权限列表
├── getDangerousPermissions(apkPath): List<String>  // 返回危险权限
├── getSignatureSummary(apkPath): String       // 返回签名摘要
├── getSecurityScanResult(apkPath): String      // 返回安全扫描结果摘要
├── getDexFileList(apkPath): List<DexFileInfo>  // 返回 DEX 文件列表
├── getDexClassList(apkPath, dexName): String  // 返回类列表（截断前 N 个）
├── searchDexStrings(apkPath, keyword): List<String>  // 搜索 DEX 字符串
├── getFileList(apkPath): List<ApkEntry>       // 返回文件列表
├── getFileSize(apkPath): Long                 // 返回 APK 大小
├── getTrackingSdks(apkPath): List<String>      // 返回追踪 SDK 列表
└── getHardcodedSecrets(apkPath): List<String>  // 返回硬编码密钥列表
```

**ToolBridge 的职责：**

| 职责 | 说明 |
|---|---|
| 数据裁剪 | Repository 返回 8000 个类，ToolBridge 只取前 100 个发给 AI |
| 格式转换 | Repository 返回 DexClassInfo 对象，ToolBridge 转成 AI 可读文本 |
| 错误处理 | Repository 抛异常时，ToolBridge 返回空结果 + 错误信息，不崩溃 |
| 安全边界 | ToolBridge 只暴露 AI 应该访问的数据，不暴露敏感操作 |
| 缓存 | 同一个 APK 的同一份数据，避免重复调用 Repository |

### 5.4 ToolBridge 实现（概念）

ToolBridge 依赖现有的 Repository，但不修改它们：

```
ToolBridge 注入：
  - SecurityScannerRepository (现有，@Singleton，不改)
  - DexRepository (现有，@Singleton，不改)
  - ApkFile (现有，每次 new，不改)

ToolBridge 是新的 @Singleton
  - 被 AiService 注入
  - 不被任何现有 Screen/ViewModel 直接引用
```

### 5.5 AI 不直接操作工具

**AI 不能：**
- 直接打开 DEX Browser（但可以生成一个"跳转到 DEX Browser"的 Action Card）
- 直接修改文件（但可以告诉用户在哪里修改）
- 直接执行签名（但可以生成一个"跳转到签名工具"的 Action Card）

**AI 只做两件事：**
1. 读取数据（通过 ToolBridge）
2. 生成建议 + Action Card（由 UI 层渲染为按钮，用户点击后跳转）

**用户点击 Action Card 后的跳转由 NavHost 处理，AI 不参与。**

### 5.6 为什么这样设计

| 原因 | 说明 |
|---|---|
| 解耦 | AI 层和 Feature 层完全解耦，任何一端变化不影响另一端 |
| 安全 | AI 只能读不能写，无法直接修改 APK |
| 稳定 | Repository 不变，现有功能不受影响 |
| 可测 | ToolBridge 可以独立测试，不依赖 LLM |
| 可扩展 | 未来新增 AI 能力只需在 ToolBridge 中加方法，不改 Repository |
| Token 控制 | ToolBridge 控制发给 LLM 的数据量，避免超限 |

---

---

## 第六部分：Session 系统迁移

### 6.1 当前状态

当前 APK 分析状态由 `ApkAnalyzerViewModel` 持有：

```
ApkAnalyzerViewModel
  └── state: StateFlow<ApkAnalyzerState>
      ├── apkInfo: ApkInfo?       // 分析结果
      ├── appIcon: Bitmap?       // 应用图标
      ├── apkFilePath: String?   // APK 路径
      ├── isLoading: Boolean     // 加载中
      └── error: String?         // 错误
```

**问题：**
- 状态只在 ApkAnalyzerViewModel 内持有
- 其他页面（DexBrowser/Signing/Security）需要 apkPath 时，通过路由参数传递
- ApkDetailScreen 依赖父级 ViewModel 的 SavedStateHandle，耦合
- AI 无法感知当前分析状态

### 6.2 目标状态

引入 `ApkSessionManager`（@Singleton），全局共享 APK 分析状态：

```
ApkSessionManager (@Singleton)
  └── session: StateFlow<ApkSession>
      ├── Idle          // 空闲
      ├── Analyzing      // 分析中
      ├── Analyzed       // 分析完成（含 ApkInfo）
      ├── Failed         // 分析失败（含 error）
      ├── Modified       // 已修改
      └── Signed         // 已签名
```

### 6.3 迁移策略：双轨并行

**不一次性替换。让 ApkAnalyzerViewModel 和 ApkSessionManager 同时工作。**

#### 阶段 A：ApkSessionManager 被动接收

```
ApkAnalyzerViewModel.handleIntent(OpenApk)
  ↓
  分析 APK（原有逻辑不变）
  ↓
  分析完成
  ↓
  update { it.copy(apkInfo = ...) }        ← 原有逻辑，保留
  sessionManager.setAnalyzed(apkInfo)     ← 新增一行，通知 Session
```

**改动量：** ApkAnalyzerViewModel 新增 1 行注入 + 1 行调用。原有逻辑完全不变。

#### 阶段 B：新页面读取 Session

新增的 AI 面板和 Action Card 系统从 ApkSessionManager 读取状态：

```
AiAssistantPanel
  └── 注入 ApkSessionManager
  └── 读取 session.value
  └── 如果是 Analyzed → 构建上下文 → 可对话
  └── 如果是 Idle → 显示"请先选择 APK"
```

#### 阶段 C：旧页面逐步迁移（可选）

**不强制。** 旧页面（DexBrowser/Signing/Security）继续通过路由参数接收 apkPath。

只有当某个旧页面需要改造时，才考虑让它从 ApkSessionManager 读取，而非路由参数。

**迁移优先级：**
- ApkDetailScreen → 可以迁移（Phase 3 改造时顺便做）
- DexBrowserScreen → 不迁移（当前工作正常）
- SigningScreen → 不迁移
- SecurityScannerScreen → 不迁移

### 6.4 兼容性保证

| 场景 | ApkAnalyzerViewModel | ApkSessionManager | 结果 |
|---|---|---|---|
| 用户选择 APK | 原有逻辑执行 | 被动接收 setAnalyzed | ✅ 两边都有数据 |
| 用户从 DexBrowser 返回 | 原有 SavedStateHandle 恢复 | 不变 | ✅ 两者一致 |
| AI 面板读取状态 | 不影响 | 读取 Analyzed | ✅ 正常 |
| 用户清除分析 | 原有 Clear 逻辑 | sessionManager.reset() | ✅ 两边都清空 |
| 进程被杀恢复 | SavedStateHandle 恢复 apkFilePath | Session 变为 Idle | ⚠ 不一致，但 AI 面板显示"请重新分析"即可 |

### 6.5 为什么不直接替换

| 如果直接替换 | 后果 |
|---|---|
| 删除 ApkAnalyzerState | ApkAnalyzerScreen 和 ApkDetailScreen 全部编译失败 |
| 删除 SavedStateHandle 持久化 | 进程被杀后无法恢复分析状态 |
| 改 DexBrowserScreen 从 Session 读取 | DexBrowser 的路由参数逻辑要改，可能引入 Bug |

**渐进式迁移的核心：旧路径保留，新路径并行。等新路径稳定后再考虑清理旧路径。**

---

---

## 第七部分：UI 迁移策略

### 7.1 迁移目标

从"工具首页"变成"AI 首页"。但工具入口全部保留，只是层级化。

### 7.2 三级模式的实现策略

**不是三套 UI。是一套 UI + 一个 `mode` 变量控制显示。**

```
HomeScreen(mode: AnalysisMode) {
    when(mode) {
        NORMAL -> {
            // AI 欢迎卡 + 最近任务
            // 不显示工具入口
        }
        ADVANCED -> {
            // AI 欢迎卡 + 最近任务 + 快捷工具
        }
        EXPERT -> {
            // 最近任务 + 工具网格
        }
    }
}
```

### 7.3 首页迁移

**当前首页：**
- AppDexBar("工作台")
- Hero 卡片（AppDex 品牌 + v1.0.0 + "开始分析"）
- 快速工具 2×3 网格（DEX/权限审计/签名验证/终端/编辑器/快速扫描）
- 最近分析（空状态）
- FAB

**迁移后首页（普通模式）：**
- AppDexBar("AppDex")
- AI 欢迎卡（"你好，我可以帮你分析 APK" + [选择 APK]）
- 最近任务列表（接入持久化后）
- FAB（选择 APK）

**迁移后首页（高级模式）：**
- AppDexBar("AppDex")
- AI 欢迎卡
- 最近任务列表
- 快捷工具入口（折叠）

**迁移后首页（专家模式）：**
- AppDexBar("AppDex")
- 最近任务列表
- 工具网格（全部平铺）
- [选择 APK] 按钮

**迁移策略：**
1. Phase 2：重写 HomeScreen，默认普通模式
2. Phase 4：新增模式切换（Settings 中）
3. Phase 5：三种模式完整实现

### 7.4 任务页迁移

**当前 ApkDetailScreen：**
- 6 Tab（概览/清单/DEX/资源/签名/文件）
- 概览 Tab 内联安全评分算法
- 快捷入口（DEX/签名/重打包）

**迁移后（普通模式）：**
- 应用概览卡（图标 + 名称 + 评分）
- 风险发现卡
- AI 总结卡
- 操作中心（查看文件/查看代码/修改资源/签名/重打包/安全检查）
- AI 对话入口

**迁移后（高级模式）：**
- 上述全部 + 折叠的 6 Tab

**迁移策略：**
1. Phase 3：新增普通模式 UI，原有 6 Tab 代码用 `if/else` 包裹为高级模式
2. 默认显示普通模式
3. 高级模式可展开

### 7.5 工具页迁移

**当前 ToolsScreen：** 扁平列表

**迁移后：** 三分类（分析工具/修改工具/开发工具）

**迁移策略：** Phase 4 重构 ToolsScreen 布局。所有工具跳转逻辑不变。

### 7.6 底部导航迁移

**当前：** 主页 / 分析 / 文件 / 工具 / 设置

**迁移后：** 首页 / 任务 / 文件 / 工具 / 设置

**迁移策略：** Phase 4 修改标签文字。Route.Analyzer 保留不变，只改显示标签。

### 7.7 旧入口隐藏不是删除

| 入口 | 普通模式 | 高级模式 | 专家模式 |
|---|---|---|---|
| 首页快速工具网格 | 隐藏 | 显示 | 显示 |
| 工具页全部工具 | 显示"切换高级模式" | 三分类折叠 | 全部平铺 |
| ApkDetail 6 Tab | 隐藏在折叠区 | 折叠区可展开 | 默认展开 |
| Terminal | 隐藏 | 隐藏 | 可用 |
| HEX Editor | 隐藏 | 工具页可见 | 可用 |
| ELF Viewer | 隐藏 | 工具页可见 | 可用 |
| FTP / Web Server | 隐藏 | 工具页可见 | 可用 |
| Encoding Converter | 隐藏 | 工具页可见 | 可用 |
| Device Info | 隐藏 | 工具页可见 | 可用 |

**隐藏 = UI 不显示入口，但路由仍然可达。专家模式通过设置切换。**

### 7.8 模式切换实现

```
SettingsRepository 新增：
  val analysisMode: Flow<AnalysisMode>  // NORMAL / ADVANCED / EXPERT

SettingsScreen 新增：
  "分析模式" 分区
  ○ 普通（隐藏技术细节）
  ○ 高级（显示工具入口）
  ○ 专家（全部开放）

HomeScreen / ToolsScreen / ApkDetailScreen 读取 analysisMode：
  when(mode) {
      NORMAL -> ...
      ADVANCED -> ...
      EXPERT -> ...
  }
```

---

---

## 第八部分：开发优先级

### 8.1 评估维度

| 维度 | 说明 |
|---|---|
| 投入时间 | 需要多少个开发时段（每时段 2-3 小时） |
| 收益 | 对用户体验的提升程度 |
| 风险 | 破坏现有功能的可能性 |

### 8.2 优先级分类

#### 必须做（Must Do）

| 序号 | 任务 | Phase | 投入 | 收益 | 风险 | 理由 |
|---|---|---|---|---|---|---|
| M1 | 新增 core-ai + core-session 模块骨架 | Phase 0 | 1-2 | 基础 | 极低 | 没有 AI 模块就没有 2.0 |
| M2 | AI 配置页面 | Phase 1 | 1-2 | 高 | 低 | 用户必须能配置 AI 才能用 |
| M3 | AI 聊天核心（AiClient + AiService） | Phase 1 | 2-3 | 极高 | 低 | 核心功能 |
| M4 | APK 上下文生成（ApkContextBuilder） | Phase 1 | 1 | 高 | 低 | AI 需要 APK 数据才能分析 |
| M5 | 首页 AI 欢迎卡 | Phase 2 | 1-2 | 高 | 中低 | 首页是第一印象 |
| M6 | ApkDetailScreen AI 按钮 + BottomSheet | Phase 1 | 1 | 高 | 低 | AI 对话入口 |

#### 应该做（Should Do）

| 序号 | 任务 | Phase | 投入 | 收益 | 风险 | 理由 |
|---|---|---|---|---|---|---|
| S1 | ApkDetailScreen 普通模式 | Phase 3 | 3-4 | 高 | 中 | 核心体验改造 |
| S2 | ToolBridge 基础能力 | Phase 1-5 | 2 | 高 | 低 | AI 需要读取 AppDex 数据 |
| S3 | AI 自动总结 | Phase 3 | 1 | 高 | 低 | 分析后自动生成报告 |
| S4 | Action Card 跳转（核心 10 个） | Phase 5 | 2 | 高 | 低 | AI 回复中的操作入口 |
| S5 | 底部导航"分析"→"任务" | Phase 4 | 0.5 | 中 | 低 | 定位转变 |
| S6 | 工具页三分类 | Phase 4 | 1 | 中 | 低 | 工具组织改善 |
| S7 | Prompt Library（50 个先做） | Phase 5 | 1 | 中 | 低 | 用户引导 |

#### 以后做（Later）

| 序号 | 任务 | Phase | 投入 | 收益 | 风险 | 理由 |
|---|---|---|---|---|---|---|
| L1 | 最近任务持久化（Room） | Phase 5 | 2 | 中 | 中 | 涉及 DB Schema 变更 |
| L2 | 三级模式切换 | Phase 5 | 2 | 中 | 低 | 非核心，渐进式实现 |
| L3 | Prompt Library 扩展到 100 个 | Phase 5+ | 1 | 低 | 低 | 50 个已够用 |
| L4 | Action Card 扩展到 55 个 | Phase 5+ | 2 | 中 | 低 | 10 个核心先做 |
| L5 | 多 Provider 配置保存 | Phase 5+ | 1 | 低 | 低 | 单个够用 |
| L6 | AI 高级设置（Temperature/Prompt） | Phase 5+ | 0.5 | 低 | 低 | 专家才需要 |

#### 不要做（Won't Do）

| 序号 | 任务 | 理由 |
|---|---|---|
| W1 | MCP 协议 | 过度设计，当前阶段不需要 |
| W2 | Agent 自主规划 | AI 能力不足，风险高 |
| W3 | 自动执行修改 | 违反安全红线 |
| W4 | 重写 MVI 架构 | 没必要，现有架构完全够用 |
| W5 | 重写 KMP 解析引擎 | 稳定且高性能，不需要重写 |
| W6 | 拆分 feature-analyzer 模块 | 收益低，风险高 |
| W7 | AI 联网搜索 | AppDex 不需要联网搜索 |
| W8 | AI 代码编译/执行 | AppDex 没有编译器 |

### 8.3 推荐执行顺序

```
第 1-2 周：Phase 0（M1）
  ↓ 编译通过确认
第 3-5 周：Phase 1（M2 + M3 + M4 + M6 + S2 基础）
  ↓ AI 可对话确认
第 6-7 周：Phase 2（M5）
  ↓ 首页改造确认
第 8-11 周：Phase 3（S1 + S3）
  ↓ 任务页改造确认
第 12 周：Phase 4（S5 + S6）
  ↓ 工具分类确认
第 13-18 周：Phase 5（S4 + S7 + L1 + L2）
  ↓ 高级能力确认
```

**每个阶段完成后：**
1. 编译通过
2. 安装测试
3. 确认原有功能不受影响
4. commit + tag
5. 再开始下一阶段

---

---

## 第九部分：最终目标

### 9.1 AppDex 2.0 完成后的用户流程

#### 普通用户（3 步）

```
Step 1: 选择 APK
  → 首页点击"选择 APK"
  → 文件选择器
  → 选择 .apk 文件

Step 2: AI 分析
  → 自动分析（Manifest + 签名 + 安全 + DEX + 体积）
  → AI 自动生成总结
  → "这个应用安全评分 85/100，低风险。
      它申请了摄像头和位置权限（用于拍照和分享），
      集成了 Google AdMob 广告 SDK，
      签名有效。没有发现恶意行为。"

Step 3: 看懂结果
  → 用户看到安全评分（大数字 + 风险等级）
  → 用户看到 AI 总结（人话）
  → 用户看到风险发现（✓/⚠/✗ 列表）
  → 用户可以继续问："广告在哪？" → AI 定位 + [查看代码]
  → 用户可以继续问："怎么改名字？" → AI 建议 + [打开编辑器]
  → 用户全程不需要知道什么是 DEX/Manifest/Smali
```

#### 高级用户

```
Step 1: 选择 APK
  → 首页点击"选择 APK"或从文件管理器点击 APK

Step 2: AI 辅助
  → 自动分析 + AI 总结
  → 用户问"登录逻辑在哪？" → AI 定位到 LoginActivity
  → 用户点击 [打开代码] → 跳转到 DEX Browser，自动定位到该类
  → 用户在 DEX Browser 中查看 Smali 代码

Step 3: 进入专业工具
  → 用户从 DEX Browser 返回任务页
  → 用户点击操作中心的"修改资源" → 跳转到 ARSC 编辑器
  → 用户修改后点击"重新签名" → 跳转到签名工具
  → 用户签名后点击"重新打包" → 跳转到重打包工具
```

#### 专家用户

```
直接工具链：
  → 首页 → 工具 Tab → DEX Browser / HEX Editor / Terminal / ...
  → 或者：首页 → 文件 Tab → 浏览文件 → 点击 APK → 分析 → 任务页 → 高级模式 → 6 Tab
  → AI 可选，不是必须
```

### 9.2 成功标准

| 指标 | 目标 |
|---|---|
| 普通用户能在 3 步内得到 APK 安全评估 | ✅ 选择 → 分析 → 看懂 |
| AI 回复中至少 80% 包含可操作的 Action Card | ✅ |
| 所有 19 个 feature 模块功能不受影响 | ✅ |
| 编译零错误 | ✅ |
| 无崩溃 | ✅ |
| 首页零技术术语（普通模式） | ✅ |
| AI 对话响应时间 < 10 秒（首 Token < 3 秒） | ✅ |

### 9.3 产品定位验证

完成后的 AppDex 2.0 应该能回答以下问题：

| 问题 | 回答方式 |
|---|---|
| "这个 APP 安全吗？" | AI 综合安全扫描结果，给出评分和结论 |
| "广告在哪？" | AI 定位广告 SDK 位置，提供 [查看代码] 按钮 |
| "怎么改名字？" | AI 定位 resources.arsc 中的 app_name，提供 [打开编辑器] 按钮 |
| "为什么申请摄像头？" | AI 解释权限用途和风险 |
| "登录逻辑在哪？" | AI 搜索 DEX 中的 LoginActivity，提供 [查看代码] 按钮 |
| "签名有效吗？" | AI 检查签名版本和证书，给出结论 |
| "为什么闪退？" | AI 综合分析可能原因，给出排查清单 |

---

---

## 迁移路线图

```
                    AppDex 2.0 迁移路线图
            （个人开发者节奏，每周 2-3 个开发时段）

Week 1-2 ──── Phase 0: 基础设施 ──────────────────────────
  │  新增 core-ai + core-session 模块骨架
  │  修改: settings.gradle.kts, app/build.gradle.kts (2 文件)
  │  新增: 7 个文件
  │  风险: 极低
  │  验证: 编译通过 + 原有功能不变
  │
  ↓
Week 3-5 ──── Phase 1: AI 接入 ────────────────────────────
  │  AI 配置 + AI 聊天 + APK 上下文生成
  │  修改: Route.kt, NavHost, SettingsScreen/VM,
  │        ApkDetailScreen, 2 个 build.gradle.kts (7 文件)
  │  新增: AiClient, ApkContextBuilder, AiService,
  │        AiAssistantPanel, AiConfigCard (5 文件)
  │  风险: 低
  │  验证: AI 配置可保存 + 测试连接 + 对话可用
  │
  ↓
Week 6-7 ──── Phase 2: 首页改造 ───────────────────────────
  │  从工具箱首页变为 AI 欢迎入口
  │  修改: HomeScreen.kt, AppDexNavHost.kt (2 文件)
  │  风险: 中低
  │  验证: 首页无技术术语 + 选择 APK 可进入分析
  │
  ↓
Week 8-11 ─── Phase 3: 任务页改造 ────────────────────────
  │  ApkDetailScreen 增加普通模式 + AI 总结
  │  修改: ApkDetailScreen.kt, ApkAnalyzerViewModel.kt (2 文件)
  │  新增: TaskWorkbenchComponents.kt (1 文件)
  │  风险: 中
  │  验证: 普通模式/高级模式切换 + 所有 Tab 功能正常
  │
  ↓
Week 12 ────── Phase 4: 工具分类 ─────────────────────────
  │  工具页三分类 + 底部导航标签
  │  修改: ToolsScreen.kt, NavHost, BottomNav (3 文件)
  │  风险: 低
  │  验证: 三分类清晰 + 所有工具入口可用
  │
  ↓
Week 13-18 ── Phase 5: 高级能力 ──────────────────────────
  │  Action Card + Prompt Library + 最近任务 + 三级模式
  │  修改: 8 文件 + 新增 4 文件
  │  风险: 中
  │  验证: Action Card 可点击 + Prompt Library 可用 +
  │        最近任务持久化 + 三级模式可切换
  │
  ↓
  ═══════════════════════════════════════════════════
  ║  AppDex 2.0 完成                              ║
  ║  AI 驱动的 APK 分析助手                        ║
  ║  所有原有功能保留 + AI 能力加持                 ║
  ═══════════════════════════════════════════════════
```

### 关键里程碑

| 里程碑 | 时间 | 标志 |
|---|---|---|
| **M1: AI 可用** | Week 5 | 用户可以配置 AI 并在 APK 详情页对话 |
| **M2: 首页改造** | Week 7 | 首页不再是工具箱，是 AI 欢迎入口 |
| **M3: 任务页改造** | Week 11 | APK 详情页有普通模式（评分+AI总结+操作中心） |
| **M4: 工具分类** | Week 12 | 工具页三分类 + 底部导航标签更新 |
| **M5: 2.0 完成** | Week 18 | Action Card + Prompt Library + 三级模式全部就绪 |

### 弹性时间

- 如果每周只能投入 1 个时段：总时间 × 2 = 约 26-36 周
- 如果遇到 Bug 或功能调整：每个 Phase 预留 1-2 周缓冲
- 如果某个 Phase 太复杂可以拆分：Phase 5 可以拆成 5a/5b

### 每个阶段的纪律

1. **先编译** — 每次改动后先确认编译通过
2. **先测试旧功能** — 每次改动后先测试原有功能是否正常
3. **再测试新功能** — 确认旧功能不坏后再测试新功能
4. **commit + tag** — 每个 Phase 完成后打 tag，方便回退
5. **不跨 Phase 开发** — 不要在 Phase 1 时就改 Phase 3 的文件

---

*本文档为 AppDex 2.0 架构迁移策略。按照个人开发者维护开源项目的节奏设计，务实、渐进、安全。*
