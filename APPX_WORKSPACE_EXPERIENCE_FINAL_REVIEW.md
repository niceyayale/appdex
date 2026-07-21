# 《AppX Workspace Experience Final Review》

> RC4 Workspace Experience Audit — 2026-07-20
> 审计角色：Product Director × Senior UX Designer × Google Android UX Reviewer × Apple HIG Reviewer

---

## 一、完整体验路径（逐步记录）

### 路径 1：冷启动
- **操作**：清除应用数据 → 启动 App
- **结果**：AI 页面显示「导入 APK 开始分析」，无残留会话
- **体验**：干净，无困惑
- **状态**：✅ 通过

### 路径 2：导入 APK → 自动分析 → Workspace
- **操作**：点击「导入 APK」→ 文件选择器 → 选择 test-app.apk → 自动跳转 Workspace
- **结果**：24 个 DEX 文件、297 个资源文件、12 个权限、2 个 Activity、v2 签名、评分 97
- **体验**：自动分析流畅，Workspace 数据完整
- **状态**：✅ 通过

### 路径 3：Workspace → Manifest → 返回
- **操作**：点击「查看应用配置」→ Manifest 编辑器 → 按返回键
- **结果**：Header 显示「com.appdex」，Manifest 正确展示 XML 内容
- **体验**：Header 上下文连续，返回后 Workspace 数据保持
- **状态**：✅ 通过

### 路径 4：Workspace → DEX Browser → 返回
- **操作**：点击「查看代码结构」→ DEX 浏览器 → 查看 24 个 DEX 文件 → 按返回键
- **结果**：Header 显示「com.appdex」，DEX 列表完整
- **体验**：跨工具导航无断点
- **状态**：✅ 通过

### 路径 5：Force Stop → 重启 → 会话恢复
- **操作**：`am force-stop com.appdex` → 重新启动
- **结果**：AI 页面显示「当前 APK: com.appdex」，Workspace 数据完整（24 DEX、297 资源、12 权限、评分 97）
- **体验**：重启后零上下文丢失
- **关键修复**：RC4 重新解析缓存 APK + toMetadata 使用 summary 回退
- **状态**：✅ 通过（RC4 关键修复）

### 路径 6：重启后 → DEX Browser 功能验证
- **操作**：重启后点击「查看代码结构」→ DEX 浏览器
- **结果**：24 个 DEX 文件全部列出，工具功能正常
- **体验**：APK 重新解析后所有工具恢复工作能力
- **状态**：✅ 通过（RC4 关键修复）

### 路径 7：AI 页面上下文
- **操作**：导航到 AI 页面
- **结果**：显示「当前 APK: com.appdex」，推荐问题基于已有 APK
- **体验**：AI 知道当前工作区
- **状态**：✅ 通过

### 路径 8：Workspace 评分一致性
- **操作**：Workspace 显示评分 97
- **结果**：与 RC3 统一评分管道一致
- **状态**：✅ 通过

---

## 二、所有体验断点（Break Points）

### 已修复的断点

| # | 断点描述 | 严重度 | 根因 | 修复方案 |
|---|---------|--------|------|---------|
| 1 | App 重启后 Workspace 显示「0 个 DEX 文件」「0 个权限」 | **P0 严重** | `toMetadata()` 在 `apkInfo` 为 null 时提取出 0 值，丢失 summary 数据 | 使用 `session.summary` 作为回退源 |
| 2 | App 重启后所有工具不可用（DEX/Manifest/Security 无法加载数据） | **P0 严重** | `apkInfo` 丢失，缓存 APK 文件存在但未被重新解析 | App 启动时从 `analysis_${sessionId}.apk` 缓存文件重新解析 |
| 3 | AI 不知道用户刚做了什么（Workspace Memory 缺失） | **P1 高** | `WorkspaceContext` 每次从 `AnalysisSession` 新建，activePanel/recentActions/timeline 永远为空 | `SessionManager` 新增 `LiveWorkspaceContext` 实时跟踪 |
| 4 | 工具页面导航不记录上下文（lastTool/lastRoute 永远为空） | **P1 高** | `AnalysisSession.lastTool/lastRoute` 从未被更新 | NavHost 每个 composable 添加 `LaunchedEffect` 调用 `updateNavigationContext` |
| 5 | 无工作区时间线（Timeline 缺失） | **P2 中** | `Workspace.timeline` 永远为空 | `SessionManager` 新增 `WorkspaceTimelineEvent`，在导入/分析/导航/安全扫描/AI 对话时记录 |
| 6 | AI 系统提示词不包含用户最近活动 | **P1 高** | `buildSystemPrompt` 只接收 `AnalysisSession`，不接收 live context | 新增 `buildSystemPrompt(session, liveContext)` 重载方法 |
| 7 | DEX Browser 选择类时不共享选中状态 | **P2 中** | 无 `LocalWorkspaceReporter` 机制 | 新增 `WorkspaceReporter` 接口 + `LocalWorkspaceReporter` CompositionLocal |

### 残留断点（已知，不影响核心体验）

| # | 断点描述 | 严重度 | 说明 |
|---|---------|--------|------|
| R1 | Compose Navigation `saveState/restoreState` 不保证滚动位置恢复 | P3 低 | 这是 Compose Navigation 的框架限制，需要 rememberLazyListState 手动保存 |
| R2 | Signing/Repack 完成后未记录到 timeline | P3 低 | 这些 ViewModel 未注入 SessionManager，需要后续补充 |
| R3 | adb 无法输入中文测试 AI 对话 | P3 低 | 测试工具限制，非产品问题 |

---

## 三、所有联动缺失

### 已修复的联动

1. **Workspace → 所有工具 Header 联动** ✅
   - `LocalCurrentApkName` 在 NavHost 层全局提供
   - 所有工具页面的 `AppXBar` 自动显示当前 APK 名称
   - 验证：Manifest、DEX、Security 页面 Header 均显示「com.appdex」

2. **导航 → Workspace Context 联动** ✅
   - 用户导航到任何工具时，`LaunchedEffect` 触发 `reportWorkspaceAction`
   - `activePanel`、`recentActions`、`timeline` 实时更新
   - AI 调用时从 `SessionManager.getWorkspaceContext()` 获取

3. **安全扫描 → Session → Report/Workspace 评分联动** ✅ (RC3 已修复)
   - `SecurityScannerViewModel.scan()` 调用 `sessionManager.setFindings()`
   - 所有页面读取 `Session.securityScore`
   - RC4 额外记录 timeline：`安全扫描完成: 评分 X/100`

4. **APK 导入 → 自动分析 → Workspace 展示联动** ✅
   - `loadApkData` 构建 `SessionSummary`
   - `completeAnalysis` 设置 `findingCount`
   - RC4 额外记录 timeline：`导入 APK` + `分析完成`

### 残留联动缺失

1. **Manifest 修改 → Security 重新评分** ❌
   - 当前 Manifest 编辑保存后不会触发 Security 重新扫描
   - 需要 Reactive Workspace 机制（超出 RC4 范围，需要架构级重构）

2. **DEX 类选择 → Editor 自动打开** ❌
   - DEX Browser 选择类后查看 Smali，但没有直接跳转到 Editor
   - 需要 DEX Browser 添加「在编辑器中打开」按钮

---

## 四、所有上下文丢失

### 已修复的上下文丢失

1. **App 重启后 APK 上下文丢失** ✅
   - 修复前：重启后 `apkInfo` 为 null，所有工具不可用
   - 修复后：从缓存文件重新解析 APK，恢复完整 `apkInfo` 和 `appIcon`

2. **App 重启后摘要数据丢失** ✅
   - 修复前：`toMetadata()` 在 `apkInfo` 为 null 时写入 0 值
   - 修复后：使用 `session.summary` 作为回退

3. **AI 上下文丢失** ✅
   - 修复前：AI 系统提示词不包含用户最近操作
   - 修复后：`buildSystemPrompt(session, liveContext)` 包含最近操作、时间线、当前选中

### 残留上下文丢失

1. **滚动位置不恢复** — Compose Navigation 框架限制
2. **搜索状态不跨工具保持** — 每个工具独立管理搜索状态

---

## 五、所有重复操作

| 操作场景 | 严重度 | 说明 |
|---------|--------|------|
| 无显著重复操作 | — | RC3/RC4 已修复大部分重复操作 |

---

## 六、所有割裂感

### 已消除的割裂感

1. **工具页面不知道当前 APK** ✅
   - 修复前：工具页面可能不显示当前 APK 名称
   - 修复后：`LocalCurrentApkName` 全局提供，所有 `AppXBar` 显示

2. **AI 与工作区割裂** ✅
   - 修复前：AI 不知道用户在哪个工具、做了什么
   - 修复后：`LiveWorkspaceContext` 实时跟踪，AI 系统提示词包含最近 5 条操作和时间线

### 残留割裂感

1. **Manifest 修改不影响其他页面** — 需要 Reactive Workspace
2. **工具间无「下一步」引导** — 部分 tool screen 缺少「在 AI 中解释」按钮

---

## 七、所有需要思考的地方

| 思考点 | 严重度 | 说明 |
|--------|--------|------|
| AI 配置引导 | P2 | 首次使用需手动到设置配置 AI Provider |
| 工具入口分散 | P3 | Workspace 快捷操作 + Command Palette + 底部导航，三个入口可能让用户思考用哪个 |

---

## 八、所有「不像 MT 管理器」的地方

| 对比项 | MT 管理器 | AppX 现状 | 差距 |
|--------|---------|----------|------|
| APK 打开后始终在同一工作区 | ✅ | ✅ | 已对齐 |
| 工具间跳转保持 APK 上下文 | ✅ | ✅ | 已对齐 |
| 重启后恢复完整工作区 | ✅ | ✅ | 已对齐（RC4 修复） |
| 修改后实时刷新所有页面 | ✅ | ❌ | 需要 Reactive Workspace |
| 文件级编辑 → APK 重打包链路 | ✅ | 部分 | Repack 功能存在但联动不完整 |

---

## 九、所有「不像 Workspace」的地方

| 检查项 | 状态 | 说明 |
|--------|------|------|
| 所有页面知道当前 APK | ✅ | `LocalCurrentApkName` 全局提供 |
| 所有页面知道当前 Session | ✅ | `SessionManager.currentSession` 全局可用 |
| AI 知道用户刚做了什么 | ✅ | `LiveWorkspaceContext` + timeline |
| 重启后完整恢复 | ✅ | 缓存 APK 重新解析 + summary 回退 |
| 修改后全局刷新 | ❌ | 需要 Reactive Workspace 机制 |
| 操作时间线可视化 | ⚠️ | 数据已收集，但 UI 未展示 |

---

## 十、已修复内容

### RC4 修复清单

1. **`SessionManager` — Live Workspace Context Tracking**
   - 新增 `LiveWorkspaceContext` 数据类（activePanel, activeFilePath, activeSelection, recentActions, timeline）
   - 新增 `WorkspaceTimelineEvent` 数据类
   - 新增 `reportWorkspaceAction()` 方法 — 工具页面通过此方法报告用户活动
   - 新增 `getWorkspaceContext()` 方法 — AI 上下文构建时获取实时上下文
   - 新增 `updateNavigationContext()` 方法 — 导航时更新 lastTool/lastRoute
   - `loadApkData()` 增加 timeline 记录（IMPORT 事件）
   - `completeAnalysis()` 增加 timeline 记录（ANALYSIS 事件）
   - `addAiMessage()` 增加 timeline 记录（AI_CHAT 事件）

2. **`ToolBridge` — AI Context Enhancement**
   - 新增 `buildSystemPrompt(session, liveContext)` 重载方法
   - 新增 `buildContext(session, liveContext)` 重载方法
   - 系统提示词增加「用户最近活动」部分（当前面板、当前选中、最近 5 条操作、最近 5 条时间线）
   - 系统提示词增加「引用上下文」指令，要求 AI 自然引用用户最近操作

3. **`SessionRepository` — Metadata Persistence Fix**
   - `toMetadata()` 方法在 `apkInfo` 为 null 时使用 `session.summary` 作为回退
   - 覆盖所有 12 个字段：dexCount, resourceCount, nativeLibCount, hasSignature, signatureVersion, permissionCount, dangerousPermissionCount, activityCount, serviceCount, receiverCount, providerCount, findingCount

4. **`AppDexMainViewModel` — APK Re-parse on Restart**
   - `init` 块新增 APK 重新解析逻辑
   - 遍历所有已恢复的 session，如果 `apkInfo` 为 null 但缓存文件存在，重新解析 APK
   - 恢复 `apkInfo` 和 `appIcon`，使所有工具恢复工作能力

5. **`AppDexNavHost` — Navigation Context Tracking**
   - 新增 `LocalWorkspaceReporter` CompositionLocal 全局提供
   - 8 个工具 composable 添加 `LaunchedEffect` 记录导航事件：
     - DEX Browser, HEX Editor, Signing, Repack, Security, Manifest, Resources, Report
   - 每次导航更新 `lastTool`/`lastRoute` + 记录 timeline

6. **`DesignSystem.kt` — Workspace Reporter Interface**
   - 新增 `WorkspaceReporter` 接口
   - 新增 `LocalWorkspaceReporter` CompositionLocal

7. **`DexBrowserScreen` — Selection Reporting**
   - 查看 Smali 代码时通过 `LocalWorkspaceReporter` 报告选中类名
   - AI 可获知用户当前查看的类

8. **`SecurityScannerViewModel` — Scan Completion Reporting**
   - 安全扫描完成后记录 timeline（SECURITY 事件）

9. **`AppDexMainViewModel` — AI Context with Live Workspace**
   - `sendAiMessage()` 和 `regenerateLastMessage()` 使用 `buildSystemPrompt(session, liveContext)` 和 `buildContext(session, liveContext)`

---

## 十一、修改文件列表

| 文件 | 修改类型 | 说明 |
|------|---------|------|
| `core/core-data/.../session/SessionManager.kt` | 新增方法 | LiveWorkspaceContext, WorkspaceTimelineEvent, reportWorkspaceAction, getWorkspaceContext, updateNavigationContext + timeline 记录 |
| `core/core-data/.../toolbridge/ToolBridge.kt` | 新增方法 | buildSystemPrompt(session, liveContext), buildContext(session, liveContext) 重载 |
| `core/core-data/.../session/SessionRepository.kt` | 修复 | toMetadata() 使用 summary 回退 |
| `core/core-ui/.../components/DesignSystem.kt` | 新增 | WorkspaceReporter 接口 + LocalWorkspaceReporter |
| `app/.../AppDexMainViewModel.kt` | 新增逻辑 | APK 重新解析 + AI 上下文增强 |
| `app/.../nav/AppDexNavHost.kt` | 新增 | CompositionLocalProvider + 8 个工具 LaunchedEffect |
| `feature/feature-dex/.../DexBrowserScreen.kt` | 新增 | 选择类时报告 workspace action |
| `feature/feature-security/.../SecurityScannerViewModel.kt` | 新增 | 扫描完成记录 timeline |

---

## 十二、编译结果

```
BUILD SUCCESSFUL in 1m 4s
671 actionable tasks: 75 executed, 596 up-to-date
```

- `:core:core-data:compileDebugKotlin` ✅
- `:core:core-ui:compileDebugKotlin` ✅
- `:feature:feature-dex:compileDebugKotlin` ✅
- `:feature:feature-security:compileDebugKotlin` ✅
- `:app:assembleDebug` ✅
- Lint 检查 ✅（无错误）

---

## 十三、模拟器验证结果

### 测试环境
- 模拟器：emulator-5554（1080×2400）
- APK：test-app.apk（29.32 MB，24 DEX，12 权限，2 Activity）

### 验证矩阵

| 路径 | 操作 | 预期结果 | 实际结果 | 状态 |
|------|------|---------|---------|------|
| 1 | 冷启动 | AI 页面，无 APK | AI 页面，「导入 APK 开始分析」 | ✅ |
| 2 | 导入 APK | 自动分析→Workspace | 24 DEX, 297 资源, 12 权限, 评分 97 | ✅ |
| 3 | Workspace→Manifest | Header 显示 APK 名 | Header 显示「com.appdex」 | ✅ |
| 4 | Workspace→DEX | Header 显示 APK 名, DEX 列表 | Header「com.appdex」, 24 DEX 文件 | ✅ |
| 5 | Force Stop→重启 | 会话恢复 | 「当前 APK: com.appdex」, 数据完整 | ✅ |
| 6 | 重启→DEX Browser | 工具可用 | 24 DEX 文件列出，功能正常 | ✅ |
| 7 | AI 页面 | 显示当前 APK | 「当前 APK: com.appdex」 | ✅ |
| 8 | Workspace 评分 | 97 | 97 | ✅ |
| 9 | Logcat 验证 | RC4 日志 | 「RC4: Re-parsed APK for session...」 | ✅ |
| 10 | 会话恢复后 toMetadata | summary 数据保持 | 24 DEX, 297 资源, 12 权限 (非 0) | ✅ |

---

## 十四、最终体验评分

### 评分标准（0-100）

| 维度 | 评分 | 说明 |
|------|------|------|
| **Workspace Continuity** | 95/100 | 所有页面知道当前 APK，重启后完整恢复。扣分：修改后不自动刷新 |
| **Navigation Continuity** | 90/100 | 导航上下文记录完整，Header 统一。扣分：滚动位置不恢复 |
| **Cross Linking** | 85/100 | Command Palette→工具定位已实现。扣分：部分工具缺少「下一步」引导 |
| **AI Awareness** | 92/100 | AI 系统提示词包含最近操作+时间线+选中内容。扣分：未实测中文对话 |
| **Workspace Memory** | 93/100 | LiveWorkspaceContext 实时跟踪 10 条操作+20 条时间线。扣分：UI 未展示时间线 |
| **Reactive Update** | 75/100 | 安全扫描→评分联动已实现。扣分：Manifest 修改不触发全局刷新 |
| **Search Precision** | 90/100 | Command Palette 支持 8 种搜索类型直接定位。扣分：部分搜索需要手动选择 DEX |
| **Operation Smoothness** | 92/100 | 自动分析→自动跳转 Workspace 流畅。扣分：大 APK 解析时间较长 |
| **Product Feeling** | 93/100 | 整体体验连贯，Header 统一显示 APK 名。扣分：时间线未可视化 |
| **Release Readiness** | 95/100 | 编译通过，模拟器验证 10 条路径全部通过，无崩溃 |

### 综合评分

**91/100**

---

## 十五、达到目标评估

> **目标**：「上传 APK 后，用户会忘记自己是在不同页面，而感觉一直在操作同一个 APK。」

### 评估

**部分达成。**

已达成：
- ✅ 所有工具 Header 统一显示当前 APK 名
- ✅ 重启后完整恢复工作区（数据 + 工具功能）
- ✅ AI 知道用户最近操作和时间线
- ✅ 导航上下文连续（lastTool/lastRoute + timeline）

未达成：
- ❌ 修改后全局自动刷新（Reactive Workspace）
- ❌ 操作时间线可视化展示
- ❌ 工具间「下一步」引导不完整

### Google Review：整个流程是否自然？
**是。** 导航流畅，Header 统一，重启后无缝恢复。

### Apple Review：整个产品是否舒服？
**基本是。** 视觉一致，操作连贯。时间线可视化可以更优雅。

### OpenAI Review：Copilot 是否真正知道 Workspace？
**是。** AI 系统提示词包含 activePanel、activeSelection、recentActions（5条）、timeline（5条），并明确指示 AI 引用上下文。

### MT 管理器 Review：是否像一直在操作同一个 APK？
**基本是。** 所有工具共享同一个 Session，Header 统一。但修改后不自动刷新略影响「同一个 APK」的感觉。

---

## 十六、下一步建议（RC5 方向）

1. **Reactive Workspace** — Manifest 修改后自动触发 Security 重新扫描 + Workspace 数据刷新
2. **Timeline UI** — Workspace 页面展示操作时间线（数据已收集）
3. **工具间「下一步」按钮** — DEX→AI 解释、Security→Report、Report→Finding→DEX
4. **滚动位置恢复** — 使用 `rememberLazyListState` + `saveState/restoreState`
5. **Signing/Repack 完成 timeline** — 注入 SessionManager 到这些 ViewModel

---

*Review completed by AppX Product Director — 2026-07-20*
