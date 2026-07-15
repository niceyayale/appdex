# AppDex 当前架构分析报告

> Phase 1 输出 — AppDex 2.0 产品方向更新

---

## 一、项目总览

| 属性 | 值 |
|---|---|
| applicationId | `com.appdex` |
| compileSdk | 35 (Android 15) |
| minSdk | 26 (Android 8.0) |
| targetSdk | 35 |
| versionCode | 7 |
| versionName | `1.0.0` |
| 架构模式 | MVI (Model-View-Intent) |
| DI 框架 | Hilt |
| UI 框架 | Jetpack Compose + Material3 |
| 序列化 | kotlinx.serialization |
| 持久化 | DataStore Preferences + Room |
| 导航 | Navigation Compose (Type-safe Routes) |

---

## 二、模块地图

### 2.1 App 模块 (`:app`)

入口模块，负责：
- `AppDexApplication` — Hilt Application，全局异常处理
- `AppDexActivity` — 单 Activity Compose 入口
- `AppDexNavHost` — 全部导航图（NavHost），27 条路由
- `Route.kt` — 类型安全路由定义（sealed interface）
- `HomeScreen.kt` — 首页（在 app 模块内）

### 2.2 Core 模块

| 模块 | 包路径 | 职责 |
|---|---|---|
| `core-arch` | `com.appdex.arch` | MVI 基础设施：`MviIntent`、`MviState`、`MviEffect`、`BaseViewModel`（含 SavedStateHandle 持久化） |
| `core-ui` | `com.appdex.ui` | 设计系统：主题（Color/Shape/Theme）、统一组件（DesignSystem.kt、StateComponents.kt、AppDexBottomNav.kt） |
| `core-data` | `com.appdex.data` | DataStore 设置持久化：`SettingsRepository`、`BookmarkRepository` |
| `core-model` | `com.appdex.model` | 共享数据模型：`FileItem`、`Bookmark`、`FileOperation` |
| `core-common` | `com.appdex.common` | 公共工具：`FormatUtil`、`MediaNavigationBus`、`Result.kt` |
| `core-plugin` | `com.appdex.plugin` | 插件系统：`PluginManager`、`PluginEntry`、`Plugin` 接口 |
| `core-database` | `com.appdex.db` | Room 数据库：`AppDexDatabase`、Schema 版本管理 |

### 2.3 核心解析引擎模块（KMP，纯 Kotlin）

| 模块 | 包路径 | 职责 |
|---|---|---|
| `core/apk` | `io.appdex.core.apk` | APK ZIP 读取、签名块解析、`ApkReader` |
| `core/arsc` | `io.appdex.core.arsc` | `resources.arsc` 二进制解析器 |
| `core/axml` | `io.appdex.core.axml` | Binary XML (AXML) 解码/编码 |
| `core/dex` | `io.appdex.core.dex` | DEX 文件解析器 |
| `core/io` | `io.appdex.core.io` | 底层 I/O 工具（KMP） |

### 2.4 Feature 模块（28 个 Screen，16 个 ViewModel）

| Feature 模块 | 主要 Screen | ViewModel | Repository | 依赖 |
|---|---|---|---|---|
| `feature-analyzer` | `ApkAnalyzerScreen`、`ApkDetailScreen` | `ApkAnalyzerViewModel` | (内联) | lib-apk |
| `feature-files` | `FileManagerScreen` | `FileManagerViewModel` | (内联) | core-model |
| `feature-editor` | `EditorScreen` | `EditorViewModel` | (内联) | lib-syntax |
| `feature-settings` | `SettingsScreen` | `SettingsViewModel` | `SettingsRepository` | core-data |
| `feature-tools` | `ToolsScreen`、`HashCalculatorScreen`、`DeviceInfoScreen`、`EncodingConverterScreen`、`PluginListScreen` | — | — | core-plugin |
| `feature-terminal` | `TerminalScreen` | `TerminalViewModel` | (内联) | — |
| `feature-remote` | `RemoteScreen`、`FtpClientScreen`、`WebServerScreen` | — | (内联) | — |
| `feature-player` | `AudioPlayerScreen`、`ImageViewerScreen`、`VideoPlayerScreen` | — | — | — |
| `feature-dex` | `DexBrowserScreen` | `DexBrowserViewModel` | `DexRepository` | core/dex |
| `feature-hex` | `HexEditorScreen` | `HexEditorViewModel` | `HexRepository` | — |
| `feature-signing` | `SigningScreen` | `SigningViewModel` | `SigningRepository` | core/apk |
| `feature-repack` | `RepackagingScreen` | `RepackagingViewModel` | `RepackagingRepository` | lib-apk, lib-archive |
| `feature-diff` | `ApkDiffScreen` | `ApkDiffViewModel` | `ApkDiffRepository` | lib-apk |
| `feature-security` | `SecurityScannerScreen` | `SecurityScannerViewModel` | `SecurityScannerRepository` | — |
| `feature-size` | `SizeAnalyzerScreen` | `SizeAnalyzerViewModel` | `SizeAnalyzerRepository` | — |
| `feature-axml` | `AxmlEditorScreen` | `AxmlEditorViewModel` | `AxmlEditorRepository` | core/axml |
| `feature-arsc` | `ArscEditorScreen` | `ArscEditorViewModel` | `ArscEditorRepository` | core/arsc |
| `feature-sqlite` | `SqliteViewerScreen` | `SqliteViewerViewModel` | `SqliteViewerRepository` | — |
| `feature-elf` | `ElfViewerScreen` | `ElfViewerViewModel` | `ElfViewerRepository` | — |

### 2.5 Library 模块

| 模块 | 职责 |
|---|---|
| `lib-apk` | `ApkFile`、`ApkModels`、`BinaryXmlDecoder` — 高层 APK 解析封装 |
| `lib-archive` | `ArchiveFactory` — ZIP/压缩文件处理 |
| `lib-syntax` | `SyntaxHighlighter`、`SyntaxModel`、`SyntaxEngine` — 代码语法高亮引擎 |

---

## 三、页面地图（当前导航结构）

### 3.1 底部导航（5 Tab）

| Tab | 路由 | 页面 |
|---|---|---|
| 主页 | `Route.Home` | `HomeScreen` — 工作台首页（Hero 卡片 + 快速工具 2×3 网格 + 最近分析空状态） |
| 分析 | `Route.Analyzer` | `ApkAnalyzerScreen` — APK 选择、分析结果展示 |
| 文件 | `Route.Files` | `FileManagerScreen` — 文件浏览器 |
| 工具 | `Route.Tools` | `ToolsScreen` — 工具列表（3 个分类） |
| 设置 | `Route.Settings` | `SettingsScreen` — 应用设置 |

### 3.2 子页面路由（17 条）

| 路由 | 页面 | 入口来源 |
|---|---|---|
| `Route.Editor(filePath)` | EditorScreen | Files/Tools |
| `Route.Terminal` | TerminalScreen | Files/Tools/Home |
| `Route.Remote` | RemoteScreen | Tools |
| `Route.ApkDetail` | ApkDetailScreen | Analyzer |
| `Route.DexBrowser(apkPath)` | DexBrowserScreen | Home/Tools/ApkDetail |
| `Route.HexEditor(filePath)` | HexEditorScreen | Files |
| `Route.ApkSigning(apkPath)` | SigningScreen | Home/Tools/ApkDetail |
| `Route.ApkRepack(apkPath)` | RepackagingScreen | ApkDetail |
| `Route.ApkDiff` | ApkDiffScreen | Tools |
| `Route.ApkSecurity(apkPath)` | SecurityScannerScreen | Home/Tools |
| `Route.ApkSizeAnalyzer(apkPath)` | SizeAnalyzerScreen | Tools |
| `Route.AxmlEditor(apkPath, entryName)` | AxmlEditorScreen | Tools |
| `Route.ArscViewer(apkPath)` | ArscEditorScreen | Tools |
| `Route.SqliteViewer(dbPath)` | SqliteViewerScreen | Tools |
| `Route.ElfViewer(filePath)` | ElfViewerScreen | Tools |

### 3.3 媒体弹窗（非路由，由 `MediaNavigationBus` 驱动）

| 类型 | 页面 | 触发来源 |
|---|---|---|
| Image | `ImageViewerScreen` | FileManager 点击图片 |
| Audio | `AudioPlayerScreen` | FileManager 点击音频 |
| Video | `VideoPlayerScreen` | FileManager 点击视频 |
| APK | 导航到 Analyzer | FileManager 点击 APK |

---

## 四、首页现状分析

`HomeScreen.kt`（位于 app 模块）当前结构：

1. **AppDexBar** — 标题 "工作台"
2. **Hero 卡片** — AppDex 品牌标识 + 版本号 + "开始分析" 按钮 → 导航到 `Route.Analyzer`
3. **快速工具**（2×3 网格）：
   - 快速扫描 → "分析"（跳 Analyzer）
   - DEX 浏览器 → "DEX"（跳 DexBrowser）
   - 权限审计 → "权限"（跳 ApkSecurity）
   - 签名验证 → "签名"（跳 ApkSigning）
   - 终端 → "终端"（跳 Terminal）
   - 文本编辑器 → "编辑器"（跳 Editor）
4. **最近分析** — 空状态 "暂无分析记录"
5. **FAB** — 浮动按钮 → 跳 Analyzer

### 首页问题

- 标题文案 "多功能 Android 逆向工程工具箱" 暴露技术定位
- 快速工具直接使用技术术语（DEX、权限审计、签名验证）
- 无任务驱动流程
- "最近分析" 无数据持久化，永远为空
- 版本号 `v1.0.0` 硬编码在 UI 中

---

## 五、APK 分析流程现状

### 5.1 分析入口

用户通过以下方式进入分析：
1. 首页 "开始分析" / FAB → Analyzer Tab
2. 文件管理器点击 APK → `MediaNavigationBus` → `pendingApkPath` → Analyzer
3. 工具页 "快速扫描" → Analyzer Tab

### 5.2 ApkAnalyzerViewModel

- **Intent**: `OpenApk(uri)`、`OpenApkPath(path)`、`Clear`
- **State**: `ApkAnalyzerState(apkInfo, appIcon, apkFilePath, isLoading, error)`
- **Effect**: `Error(message)`

### 5.3 分析流程

```
用户选择 APK URI
  ↓
copyUriToCache → temp_analysis.apk
  ↓
ApkFile(path).parse() → ApkInfo(manifest, signatures, entries, fileSize)
  ↓
如果 manifest.packageName 为空 → enrichWithPackageManager() 补充
  ↓
loadAppIcon() → PackageManager.getPackageArchiveInfo().loadIcon()
  ↓
update State(apkInfo, appIcon, apkFilePath)
  ↓
saveState("apk_file_path", path)  ← SavedStateHandle 持久化
```

### 5.4 ApkDetailScreen（APK 详情页）

**Tab 结构**（6 Tab）：
1. **概览** — 安全评分（计算逻辑内联）+ 快捷入口（权限/DEX/签名/重打包/文件列表）
2. **清单** — Manifest 信息（包名/版本/SDK）+ 权限列表（标记危险权限）
3. **DEX** — DEX 文件列表 + 点击跳转 DexBrowser
4. **资源** — res/ assets/ resources.arsc 文件列表
5. **签名** — V1/V2/V3 签名详情（算法/证书/摘要/有效期）
6. **文件** — APK 内全部文件列表（前 100 条）

### 5.5 安全评分算法

当前在 `ApkDetailScreen.kt` 中内联实现：
- 危险权限数量（-4/个，max -32）
- 签名方案（无签名 -25，无 V2/V3 -10，有 V3 +5）
- 多签名惩罚（-5/个超额，max -15）
- minSdk/targetSdk 过低惩罚
- 权限总数过多惩罚（>30 -8，>20 -4）

---

## 六、数据模型现状

### 6.1 APK 核心模型（`lib-apk/ApkModels.kt`）

```
ApkInfo
├── manifest: ApkManifest
│   ├── packageName, versionName, versionCode
│   ├── minSdk, targetSdk, compileSdk
│   ├── permissions: List<String>
│   ├── activities, services, receivers, providers: List<String>
│   └── metaData: Map<String, String>
├── signatures: List<ApkSignature>
│   ├── version (1/2/3), algorithm
│   ├── certificateSubject, certificateIssuer, serialNumber
│   ├── sha256, sha1, md5
│   └── validFrom, validTo
├── entries: List<ApkEntry>
│   ├── name, size, compressedSize, isDirectory
└── fileSize: Long
```

### 6.2 文件模型（`core-model/FileItem.kt`）

```
FileItem
├── name, path, isDirectory, size, lastModified
├── permissions, mimeType, extension
├── isHidden, isApk, isArchive, isText (计算属性)
```

### 6.3 设置模型（`core-data/SettingsRepository.kt`）

```
ThemeMode: SYSTEM / LIGHT / DARK
DensityMode: COMPACT / STANDARD / COMFORTABLE
LanguageMode: ENGLISH / CHINESE / SYSTEM
editorFontSize, editorTabWidth, defaultEncoding
terminalFontSize, terminalScrollback
showHiddenFiles, rememberLastPath, lastPath
```

---

## 七、设计系统现状

### 7.1 颜色体系（`Color.kt`）

自定义深色主题，不使用 Material3 默认色板：
- 背景：`DeepSpaceBlue`（深蓝）
- 表面：`SurfaceDeep`、`SurfaceAlt`、`SurfaceInput`
- 主色：`AmberGold`（琥珀金）、`AmberGoldDark`、`AmberGoldHighlight`
- 辅色：`IconBlue`、`IconBlueBright`、`AuroraGreen`、`NebulaBlue`、`RedSupergiant`
- 文本：`TextPrimary`、`TextSecondary`、`TextTertiary`、`TextMuted`
- 边框：`BorderLight`、`BorderMedium`、`BorderAccent`、`BorderDefault`

### 7.2 组件清单

| 组件 | 用途 | 状态 |
|---|---|---|
| `AppDexBar` | 顶部标题栏（支持返回、副标题、actions slot） | ✅ 可复用 |
| `AppDexRow` | 列表行（图标 + 标题 + 详情 + 徽章 + 箭头） | ✅ 可复用 |
| `AppDexSection` | 分区标题 | ✅ 可复用 |
| `AppDexButton` | 主按钮（AmberGold 背景） | ✅ 可复用 |
| `AppDexCard` | 边框卡片容器 | ✅ 可复用 |
| `AppDexSearchBar` | 搜索输入栏 | ✅ 可复用 |
| `AppDexFAB` | 浮动操作按钮 | ✅ 可复用 |
| `AppDexDivider` | 分割线 | ✅ 可复用 |
| `AppDexToggle` | 开关切换 | ✅ 可复用 |
| `AppDexIconBox` | 图标方块 | ✅ 可复用 |
| `AppDexTabRow` | Tab 栏 | ✅ 可复用 |
| `AppDexSnackbarHost` | Snackbar 宿主 | ✅ 可复用 |
| `LoadingState` | 加载中状态 | ✅ 可复用 |
| `EmptyState` | 空状态 | ✅ 可复用 |
| `ErrorState` | 错误状态（含重试） | ✅ 可复用 |
| `InfoRow` | 键值信息行 | ✅ 可复用 |
| `AppDexBottomNav` | 底部导航栏 | ✅ 可复用（需调标签） |
| `AppDexIcons` | 图标映射对象 | ✅ 可复用 |

---

## 八、可复用模块评估

### 8.1 直接复用（无需修改）

| 模块/文件 | 理由 |
|---|---|
| `core-arch` (BaseViewModel, MVI 接口) | 架构基础，完全稳定 |
| `core-model` (FileItem, Bookmark) | 纯数据模型，稳定 |
| `core-common` (FormatUtil, MediaNavigationBus) | 工具类，稳定 |
| `core-data` (SettingsRepository) | DataStore 持久化，稳定（需扩展 AI 配置字段） |
| `core/apk`, `core/arsc`, `core/axml`, `core/dex`, `core/io` | KMP 解析引擎，稳定 |
| `lib-apk` (ApkFile, ApkModels, BinaryXmlDecoder) | 高层解析封装，稳定 |
| `lib-archive` (ArchiveFactory) | 压缩文件处理，稳定 |
| `lib-syntax` (SyntaxHighlighter) | 语法高亮，稳定 |
| 所有 Feature 模块的 Repository 层 | 业务逻辑层，稳定 |
| 所有 Feature 模块的 ViewModel 层 | 状态管理，稳定 |
| `core-ui` 主题和组件库 | 设计系统，稳定 |

### 8.2 需要轻度修改

| 模块/文件 | 需要修改 |
|---|---|
| `Route.kt` | 新增 AI 相关路由、任务页路由；将 "分析" Tab 改为 "任务" |
| `AppDexNavHost.kt` | 更新导航图，新增路由注册；调整底部导航标签 |
| `HomeScreen.kt` | 完全重写首页 UI（任务驱动、移除技术术语） |
| `ApkDetailScreen.kt` | 重构为普通模式/高级模式双层 |
| `SettingsScreen.kt` | 新增 AI 配置入口 |
| `AppDexBottomNav` | 更新导航项标签：首页/任务/文件/工具/设置 |

### 8.3 需要新增

| 新模块 | 职责 |
|---|---|
| `core-ai` | AI Provider 管理（OpenAI/Anthropic/Custom）、Prompt 模板、上下文组装、API 调用 |
| `core-session` | APK 分析会话状态（`Analyzing`/`Analyzed`/`Failed`/`Modified`/`Signed`） |
| `feature-home`（可选） | 将 HomeScreen 从 app 模块提取为独立 feature 模块 |
| `feature-ai-assistant`（可选） | AI 助手对话界面 |
| `feature-apk-detail`（可选） | 将 ApkDetailScreen 从 feature-analyzer 提取为独立模块 |

---

## 九、现有问题总结

### 9.1 产品层面

| 问题 | 影响 |
|---|---|
| 首页暴露技术术语（DEX、权限审计、签名验证） | 普通用户无法理解 |
| 无任务驱动流程 | 用户不知道从哪里开始 |
| "最近分析"无持久化 | 功能形同虚设 |
| 工具页是扁平列表 | 功能多但没有分类引导 |
| 无 AI 辅助 | 用户面对原始数据无法解读 |
| APK 详情 Tab 直接暴露 Manifest/DEX/资源 | 普通用户不知所云 |

### 9.2 架构层面

| 问题 | 影响 |
|---|---|
| `HomeScreen` 在 app 模块内 | 不利于独立维护和测试 |
| `ApkDetailScreen` 在 `feature-analyzer` 内 | 详情页和分析入口耦合 |
| 安全评分算法内联在 Screen 中 | 无法跨页面复用 |
| `ApkAnalyzerState` 仅在 ViewModel 内持有 | 无全局 Session 状态，跨页面无法共享 |
| `pendingApkPath` 通过 NavHost 变量传递 | 脆弱的跨页面数据传递 |

### 9.3 导航层面

| 问题 | 影响 |
|---|---|
| 底部 Tab "分析" 暗示是一个页面 | 分析应是流程而非页面 |
| Home 的 `onNavigate` 使用 String 匹配 | 类型不安全 |
| 部分工具从 Home 跳转时缺少 APK 上下文 | DexBrowser/Signing/Security 从 Home 进入时 apkPath 为空 |
| `ApkDetail` 路由无参数 | 依赖父级 ViewModel 的 SavedStateHandle，非显式依赖 |

---

## 十、复用策略建议

### 原则

1. **不删除任何 feature 模块** — 所有现有 Screen/ViewModel/Repository 保留
2. **不修改 MVI 架构** — `BaseViewModel`、`MviIntent`、`MviState`、`MviEffect` 保持不变
3. **不修改 Design System** — 颜色、组件、主题保持不变
4. **不修改 KMP 解析引擎** — `core/apk`、`core/arsc`、`core/axml`、`core/dex` 保持不变
5. **新增 `core-ai` + `core-session`** — 为 AI 辅助和会话管理提供基础设施
6. **重写导航入口层** — `Route.kt`、`AppDexNavHost.kt`、`HomeScreen.kt`
7. **重构 APK 详情页** — `ApkDetailScreen.kt` 增加普通模式/高级模式切换
8. **扩展设置页** — `SettingsScreen.kt` 新增 AI 配置分区

### 改动范围

| 层级 | 改动程度 | 文件数（估计） |
|---|---|---|
| 新增模块 | 全新 | ~6-8 个新文件 |
| 导航层 | 中等重构 | 2 个文件 |
| 首页 | 完全重写 | 1 个文件 |
| APK 详情页 | 中等重构 | 1 个文件 |
| 设置页 | 轻度扩展 | 1-2 个文件 |
| 底部导航 | 轻度修改 | 1 个文件 |
| Feature Screen | 不变 | 0 |
| Feature ViewModel | 不变 | 0 |
| Feature Repository | 不变 | 0 |
| Core KMP | 不变 | 0 |
| Design System | 不变 | 0 |

---

*本报告为 AppDex 2.0 产品方向更新的第一阶段输出。下一阶段将基于此分析输出 `APPDEX_2_UI_SPEC.md` UI 规格。*
