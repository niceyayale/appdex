# AppDex 2.0 UI 规格

> Phase 2 输出 — AppDex 2.0 产品方向更新

---

## 一、设计原则

1. **任务驱动** — 用户看到的是"做什么"，不是"用什么工具"
2. **渐进式暴露** — 普通用户走 3 步完成；专业用户一键进入高级模式
3. **AI 辅助** — AI 是入口和解释者，不是替代品
4. **保留专业能力** — 所有逆向工具完整保留，只是入口层级化
5. **复用设计系统** — 不修改现有 Color/Shape/Theme/组件

---

## 二、信息架构

### 2.1 底部导航（5 Tab）

| 序号 | 标签 | 图标 | 定位 |
|---|---|---|---|
| 1 | **首页** | Home | 核心入口：分析 APK、最近任务 |
| 2 | **任务** | Assignment/Checklist | 当前 APK 工作台：分析报告 + AI + 操作中心 |
| 3 | **文件** | Folder | 文件浏览器（不变） |
| 4 | **工具** | Build | 所有专业工具（分类展示） |
| 5 | **设置** | Settings | 含 AI 配置 |

### 2.2 导航树

```
首页
├── [分析 APK] → 文件选择器 → 自动分析 → 任务页
├── [扫描已安装应用] → 包列表 → 选择 → 自动分析 → 任务页
├── 最近任务列表
│   └── 点击 → 任务页（恢复会话）
└── 底部导航跳转

任务（当前 APK 工作台）
├── 普通模式
│   ├── 应用概览卡（图标/名称/版本/大小/包名）
│   ├── 安全评分卡
│   ├── 风险发现卡
│   ├── AI 总结卡
│   └── 操作中心
│       ├── 查看文件
│       ├── 查看代码结构
│       ├── 修改资源
│       ├── 重新签名
│       ├── 重新打包
│       └── 安全检查
├── 高级模式（折叠展开）
│   ├── Manifest 查看
│   ├── DEX 浏览
│   ├── 资源文件
│   ├── HEX 编辑
│   └── ELF 查看
└── AI 助手对话

文件（不变 — FileManagerScreen）
工具（分类 — ToolsScreen）
设置（扩展 — 新增 AI 配置）
```

---

## 三、页面规格

### 3.1 首页 `HomeScreen`

**定位**：唯一核心入口，用户打开 App 后第一眼看到的页面

**布局**（LazyColumn，自上而下）：

#### 3.1.1 顶部栏
- `AppDexBar(title = "AppDex")`
- 副标题移除 "多功能 Android 逆向工程工具箱"
- 改为不显示副标题，或显示日期

#### 3.1.2 欢迎卡片（Hero）
- 背景：`ScoreCardBg` + `BorderAccent`
- 内容：
  - 大号文字："你好，我可以帮你分析 APK"
  - 两个按钮（纵向排列或横排）：
    - `AppDexButton("选择 APK 文件")` — 打开文件选择器
    - `AppDexButton("扫描已安装应用")` — 打开包列表（暂未实现则隐藏）

#### 3.1.3 最近任务列表
- 标题：`AppDexSection(label = "最近任务")`
- 从 DataStore/Room 读取最近分析的 APK 记录
- 每条记录：
  ```
  [AppIcon] 应用名 / 包名
           安全评分 85 · 低风险
           2 分钟前
  ```
  → 点击导航到任务页
- 空状态：`EmptyState(icon = Clock, title = "暂无任务", subtitle = "点击上方按钮开始分析")`

#### 3.1.4 底部
- 无快速工具网格（移除）
- 快速工具入口由"工具"Tab 承担

#### 3.1.5 FAB
- 保留 FAB，功能改为"选择 APK 文件"

#### 3.1.6 移除项
- ❌ "快速工具" 2×3 网格
- ❌ "多功能 Android 逆向工程工具箱" 文案
- ❌ `v1.0.0` 硬编码版本号
- ❌ 技术术语入口（DEX/权限审计/签名验证/终端/编辑器）

---

### 3.2 任务页 `TaskWorkbenchScreen`

**定位**：当前 APK 的工作台，整合分析报告、AI 助手、操作中心

**路由**：`Route.TaskWorkbench`（替换原 `Route.ApkDetail`）

**数据源**：`ApkSession`（core-session 模块提供）

#### 3.2.1 顶部栏
- `AppDexBar(title = "任务", back = true, onBack = popBackStack)`
- 右侧 actions：`IconButton(AI 图标)` → 展开 AI 助手面板

#### 3.2.2 应用概览卡
```
┌─────────────────────────────────────┐
│ [AppIcon 48dp]  应用名 (12sp Bold)    │
│                 版本 · 大小 · API xx+│
│ ─────────────────────────────────── │
│ 安全评分                            │
│   85           低风险                │
│   ████████████░░░░  (进度条)         │
└─────────────────────────────────────┘
```
- 复用现有 `ApkDetailScreen` 概览 Tab 的评分逻辑
- 评分算法提取到 `core-session` 的 `SecurityScoreCalculator`

#### 3.2.3 风险发现卡
```
AppDexSection(label = "风险发现")
┌─────────────────────────────────────┐
│ ✓ 无病毒特征                        │
│ ⚠ 申请 32 个权限                    │
│ ⚠ 包含广告 SDK                      │
│ ⚠ 请求后台运行                      │
│ ✓ 签名有效                          │
└─────────────────────────────────────┘
```
- 数据来源：`ApkInfo` + `SecurityScannerRepository` 的扫描结果合并

#### 3.2.4 AI 总结卡（新增）
```
AppDexSection(label = "AI 分析")
┌─────────────────────────────────────┐
│ 🤖 这个 APK 申请了摄像头、位置等     │
│    敏感权限，可能用于广告推送。       │
│    签名为 V2+V3，证书有效。          │
│                                      │
│    [询问 AI 更多问题]                │
└─────────────────────────────────────┘
```
- 如果用户未配置 AI → 显示 "配置 AI 助手以获取智能分析" + 跳转设置
- 如果已配置 → 调用 AI 生成总结

#### 3.2.5 操作中心
```
AppDexSection(label = "操作")
┌─────────────────────────────────────┐
│ 📁 查看文件        >  APK 内文件列表 │
│ 📝 查看代码结构    >  DEX 类浏览     │
│ ✏ 修改资源        >  ARSC 编辑器    │
│ 📄 编辑清单        >  AXML 编辑器    │
│ 🔏 重新签名        >  签名工具       │
│ 🔄 重新打包        >  重打包工具     │
│ 🔒 安全检查        >  安全扫描器     │
│ 📊 对比版本        >  APK Diff       │
│ 📏 体积分析        >  体积分析器     │
└─────────────────────────────────────┘
```
- 每项使用 `AppDexRow`
- 路由目标：复用现有所有 feature 模块的 Screen
- 点击时传递 `apkPath` 参数

#### 3.2.6 高级模式（折叠）
```
AppDexSection(label = "高级分析")
┌─────────────────────────────────────┐
│ ▸ Manifest 查看                     │
│ ▸ DEX 浏览                          │
│ ▸ 资源文件                          │
│ ▸ HEX 编辑                          │
│ ▸ ELF 查看                          │
│ ▸ SQLite 数据库                     │
└─────────────────────────────────────┘
```
- 默认折叠
- 展开后每项也是 `AppDexRow`，路由目标同现有
- 技术术语在此层暴露，普通用户不需要展开

#### 3.2.7 AI 助手面板（底部弹出或侧拉）
- 输入框 + 对话气泡
- AI 读取当前 `ApkSession` 的上下文（Manifest、权限、签名、安全扫描结果）
- 示例问题快捷按钮：
  - "这个 APK 为什么申请摄像头权限？"
  - "这个 APK 安全吗？"
  - "我想修改应用名称"

---

### 3.3 工具页 `ToolsScreen`（重构）

**定位**：所有专业工具的集中入口，按任务分类

**布局**（LazyColumn）：

#### 3.3.1 标题
- `AppDexBar(title = "工具")`

#### 3.3.2 分类 1：分析工具
```
AppDexSection(label = "分析工具")
├── 权限检测     >  SecurityScannerScreen
├── 安全扫描     >  SecurityScannerScreen
├── 包信息       >  (需要 APK 上下文 → 引导到分析流程)
└── 体积分析     >  SizeAnalyzerScreen
```

#### 3.3.3 分类 2：修改工具
```
AppDexSection(label = "修改工具")
├── 文件编辑     >  EditorScreen
├── 资源修改     >  ArscEditorScreen
├── 清单编辑     >  AxmlEditorScreen
├── 重打包       >  RepackagingScreen
└── 版本对比     >  ApkDiffScreen
```

#### 3.3.4 分类 3：开发工具
```
AppDexSection(label = "开发工具")
├── DEX 浏览     >  DexBrowserScreen
├── HEX 编辑     >  HexEditorScreen
├── ELF 查看     >  ElfViewerScreen
├── SQLite 查看  >  SqliteViewerScreen
├── 签名验证     >  SigningScreen
├── 终端         >  TerminalScreen
├── 远程管理     >  RemoteScreen
├── 哈希计算     >  HashCalculatorScreen
├── 编码转换     >  EncodingConverterScreen
├── 设备信息     >  DeviceInfoScreen
└── 插件中心     >  PluginListScreen
```

#### 3.3.5 移除项
- ❌ "快速扫描"（已有首页入口）
- ❌ "分析" 导航入口（分析已变为任务流程）

---

### 3.4 设置页 `SettingsScreen`（扩展）

**新增分区**：

#### 3.4.1 AI 服务配置
```
AppDexSection(label = "AI 服务")
┌─────────────────────────────────────┐
│ Provider:    [OpenAI] [Anthropic] [自定义] │
│ Base URL:    https://api.openai.com/v1     │
│ API Key:     ••••••••••••••••               │
│ Model:       gpt-4o-mini                    │
│                                      │
│ [测试连接]                           │
└─────────────────────────────────────┘
```

- Provider 选项：`OpenAI` / `Anthropic` / `Custom`
- Base URL：根据 Provider 预填默认值，可修改
- API Key：密码输入框，本地 DataStore 加密存储
- Model：文本输入
- 测试连接按钮：发送一条测试消息，显示成功/失败

#### 3.4.2 现有分区保留
- 配置（文件管理 / 外观 / 高级选项）— 不变
- 语言 — 不变
- 编辑器 — 不变
- 终端 — 不变
- 关于 — 不变

---

### 3.5 分析流程页 `ApkAnalyzerScreen`（保持）

**定位**：保留现有分析器页面作为"选择 APK"后的分析执行页

- 用户在首页点击"选择 APK" → 文件选择器 → `ApkAnalyzerViewModel.OpenApk`
- 分析完成后自动导航到任务页
- Analyzer Tab 隐藏，从底部导航移除

---

### 3.6 APK 详情页 `ApkDetailScreen`（废弃/重构）

**决策**：将 `ApkDetailScreen` 的内容合并到任务页 `TaskWorkbenchScreen`

- 概览 Tab → 任务页普通模式
- 清单/DEX/资源/签名/文件 Tab → 任务页高级模式
- 签名详情 → 操作中心的"重新签名"入口
- 文件列表 → 操作中心的"查看文件"入口

---

## 四、路由变更

### 4.1 路由变更表

| 操作 | 原路由 | 新路由 |
|---|---|---|
| 底部 Tab 改名 | `Route.Analyzer`（分析） | `Route.TaskWorkbench`（任务） |
| 详情页合并 | `Route.ApkDetail` | 废弃，内容合并到 `Route.TaskWorkbench` |
| 新增 | — | `Route.AiAssistant` |
| 新增 | — | `Route.AiConfig` |
| 保留不变 | `Route.Editor` | `Route.Editor` |
| 保留不变 | `Route.Terminal` | `Route.Terminal` |
| 保留不变 | `Route.Files` | `Route.Files` |
| 保留不变 | `Route.Tools` | `Route.Tools` |
| 保留不变 | `Route.Settings` | `Route.Settings` |
| 保留不变 | `Route.DexBrowser` | `Route.DexBrowser` |
| 保留不变 | `Route.HexEditor` | `Route.HexEditor` |
| 保留不变 | `Route.ApkSigning` | `Route.ApkSigning` |
| 保留不变 | `Route.ApkRepack` | `Route.ApkRepack` |
| 保留不变 | `Route.ApkDiff` | `Route.ApkDiff` |
| 保留不变 | `Route.ApkSecurity` | `Route.ApkSecurity` |
| 保留不变 | `Route.ApkSizeAnalyzer` | `Route.ApkSizeAnalyzer` |
| 保留不变 | `Route.AxmlEditor` | `Route.AxmlEditor` |
| 保留不变 | `Route.ArscViewer` | `Route.ArscViewer` |
| 保留不变 | `Route.SqliteViewer` | `Route.SqliteViewer` |
| 保留不变 | `Route.ElfViewer` | `Route.ElfViewer` |
| 保留不变 | `Route.Remote` | `Route.Remote` |

### 4.2 新增路由定义

```kotlin
// 底部导航
@Serializable
data object TaskWorkbench : Route  // 替换 Route.Analyzer

// AI 相关
@Serializable
data object AiAssistant : Route

@Serializable
data object AiConfig : Route
```

### 4.3 底部导航变更

```kotlin
val navItems = listOf(
    AppDexNavItem("首页", NavHomeIcon),        // 不变
    AppDexNavItem("任务", NavTaskIcon),        // 替换 "分析" → "任务"
    AppDexNavItem("文件", NavFolderIcon),      // 不变
    AppDexNavItem("工具", NavToolsIcon),        // 不变
    AppDexNavItem("设置", NavSettingsIcon)     // 不变
)
```

---

## 五、新增模块设计

### 5.1 `core-ai` 模块

**包路径**：`com.appdex.ai`

**职责**：
- AI Provider 抽象与实现
- Prompt 模板管理
- 上下文组装（将 ApkInfo 转为 AI 可读文本）
- API 调用（HTTP）

**核心类**：

```
com.appdex.ai/
├── AiProvider.kt          // sealed interface: OpenAI, Anthropic, Custom
├── AiConfig.kt            // 数据类: provider, baseUrl, apiKey, model
├── AiConfigRepository.kt  // DataStore 持久化 AI 配置
├── AiClient.kt            // HTTP 调用封装（使用 OkHttp 或 Ktor）
├── PromptTemplates.kt     // 预定义 prompt 模板
├── ApkContextBuilder.kt   // 将 ApkInfo 转为 AI 上下文文本
└── AiService.kt           // 高层 API: ask(question, apkInfo): Flow<String>
```

**AiProvider 接口**：
- `fun buildRequest(messages: List<ChatMessage>): HttpRequest`
- `fun parseResponse(body: String): String`
- `fun buildTestRequest(): HttpRequest`

**Prompt 模板**：
- `APK_SUMMARY` — "请分析以下 APK 信息，给出安全评估和风险提示..."
- `PERMISSION_EXPLAIN` — "这个 APK 申请了以下权限，请解释每条权限的用途和风险..."
- `MODIFY_GUIDE` — "用户想要修改这个 APK 的 [目标]，请指导修改位置和推荐工具..."

**ApkContextBuilder**：
```
输入: ApkInfo
输出文本:
  "应用名: xxx
   包名: com.xxx
   版本: 1.0 (1)
   SDK: minSdk=21 targetSdk=35
   权限: 32 个（含 5 个危险权限: CAMERA, ...）
   组件: 12 Activities, 5 Services, 3 Receivers, 1 Provider
   签名: V2 (SHA-256: ...), 有效期至 2049 年
   文件: 1,243 个（classes.dex 2.3MB, resources.arsc 1.2MB, ...）
   ..."
```

### 5.2 `core-session` 模块

**包路径**：`com.appdex.session`

**职责**：
- APK 分析会话状态管理
- 跨页面共享 APK 分析结果
- 安全评分计算（从 ApkDetailScreen 提取）
- 最近任务记录持久化

**核心类**：

```
com.appdex.session/
├── ApkSession.kt               // sealed class: Idle, Analyzing, Analyzed, Failed, Modified, Signed
├── ApkSessionManager.kt        // @Singleton, 持有当前 ApkSession StateFlow
├── SecurityScoreCalculator.kt  // 安全评分算法（从 ApkDetailScreen 提取）
├── RiskAssessor.kt             // 风险发现生成器
├── RecentTaskRepository.kt     // 最近分析任务持久化（Room/DataStore）
└── RecentTask.kt               // data class: appIcon, appName, packageName, score, timestamp, apkPath
```

**ApkSession 状态机**：
```
Idle → (选择 APK) → Analyzing → Analyzed
                                ↘ Failed
Analyzed → (修改资源) → Modified
Modified → (签名) → Signed
Signed → (重新分析) → Analyzing
```

**ApkSessionManager**：
- `@Singleton`，通过 Hilt 注入
- `val session: StateFlow<ApkSession>`
- `fun setAnalyzing(apkPath: String)`
- `fun setAnalyzed(apkInfo: ApkInfo)`
- `fun setFailed(error: String)`
- `fun setModified()`
- `fun setSigned()`
- `fun reset()`

---

## 六、新增 UI 组件

### 6.1 `AppDexPrimaryAction`

```
大号主按钮，用于首页"分析 APK"
- 高度 56dp
- AmberGold 背景
- 圆角 12dp
- Icon + Text 居中
```

### 6.2 `AppDexStatusCard`

```
状态卡片，用于任务页安全评分
- BorderAccent + ScoreCardBg
- 左侧大号评分数字 + 风险等级
- 右侧进度条
```

### 6.3 `AppDexReportCard`

```
报告卡片，用于风险发现
- 列表形式
- 每行: 状态图标(✓/⚠/✗) + 描述文本
- 不同状态对应不同颜色
```

### 6.4 `AppDexEmptyState`（扩展现有 EmptyState）

```
现有 EmptyState 增加可选 action 按钮
- icon + title + subtitle + [可选 actionButton]
```

### 6.5 `AppDexAiPanel`

```
AI 助手面板
- 底部弹出 BottomSheet 或侧边面板
- 对话气泡列表
- 输入框 + 发送按钮
- 快捷问题按钮列表
```

### 6.6 `AppDexAiConfigCard`

```
AI 配置卡片（设置页内）
- Provider 选择器
- Base URL 输入框
- API Key 密码输入框
- Model 输入框
- 测试连接按钮
```

---

## 七、用户体验检查清单

### 7.1 首页
- [ ] 用户第一眼看到"选择 APK"按钮
- [ ] 无技术术语
- [ ] 最近任务可点击恢复
- [ ] 空状态有引导文案

### 7.2 任务页
- [ ] 普通模式先展示（安全评分 + 风险 + AI 总结 + 操作中心）
- [ ] 高级模式默认折叠
- [ ] 所有操作入口传递 apkPath
- [ ] AI 未配置时显示引导

### 7.3 工具页
- [ ] 三分类清晰
- [ ] 所有工具可进入
- [ ] 无死路

### 7.4 设置页
- [ ] AI 配置入口明显
- [ ] 测试连接有反馈
- [ ] API Key 不明文显示

### 7.5 导航
- [ ] 底部 5 Tab 全部可进入
- [ ] 返回栈正确（popBackStack 不跳过层级）
- [ ] 旋转屏幕不丢失状态
- [ ] 后台恢复正常

---

## 八、数据流变更

### 8.1 APK 分析流程（新）

```
首页 [选择 APK]
  ↓
FilePicker → Uri
  ↓
ApkSessionManager.setAnalyzing(apkPath)
  ↓
ApkAnalyzerViewModel.handleIntent(OpenApk(uri))
  ↓
ApkFile.parse() → ApkInfo
  ↓
ApkSessionManager.setAnalyzed(apkInfo)
RecentTaskRepository.save(RecentTask(...))
  ↓
navigate(Route.TaskWorkbench)
  ↓
TaskWorkbenchScreen 读取 ApkSessionManager.session
  ↓
展示安全评分 + 风险 + 操作中心
  ↓
（可选）AiService.ask(question, apkInfo)
```

### 8.2 状态共享

```
ApkSessionManager (Singleton, @Singleton)
    ↓ StateFlow<ApkSession>
    ├── TaskWorkbenchScreen → 读取 Analyzed 状态
    ├── AiService → 读取 ApkInfo 构建上下文
    ├── DexBrowserScreen → 读取 apkPath
    ├── SigningScreen → 读取 apkPath
    └── SecurityScannerScreen → 读取 apkPath
```

---

## 九、设置项扩展

### AI 配置（DataStore 新增 Key）

| Key | 类型 | 默认值 |
|---|---|---|
| `ai_provider` | String | "" (未配置) |
| `ai_base_url` | String | "" |
| `ai_api_key` | String | "" |
| `ai_model` | String | "" |

### 最近任务（Room 新增表）

```sql
CREATE TABLE recent_tasks (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    apk_path TEXT NOT NULL,
    package_name TEXT,
    app_name TEXT,
    version_name TEXT,
    security_score INTEGER DEFAULT 0,
    risk_level TEXT,
    icon_path TEXT,
    created_at INTEGER NOT NULL
);
```

---

## 十、不变项确认

以下内容**完全不变**，确保现有功能不受影响：

1. **所有 Feature 模块的 Screen 文件** — 不修改
2. **所有 ViewModel** — 不修改
3. **所有 Repository** — 不修改
4. **所有 KMP 解析引擎** — 不修改
5. **Design System**（Color.kt, Shape.kt, Theme.kt）— 不修改
6. **现有组件库**（DesignSystem.kt, StateComponents.kt, AppDexBottomNav.kt）— 不修改，仅新增组件
7. **MVI 架构**（BaseViewModel, MviIntent, MviState, MviEffect）— 不修改
8. **Hilt DI 配置** — 仅新增模块的 DI
9. **lib-apk, lib-archive, lib-syntax** — 不修改

---

*本规格为 AppDex 2.0 产品方向更新的第二阶段输出。下一阶段将基于此规格拆分 Sprint 任务。*
