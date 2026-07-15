# AppDex Release Candidate Audit Report v1

> **审计日期**: 2026-07-13  
> **审计范围**: 全部 Kotlin 文件 · 全部 Compose Screen · Repository / ViewModel / Navigation / Storage  
> **审计方式**: 静态代码扫描（未修改任何代码）

---

## 总体评级

# **D — 不可发布**

当前代码库存在 **5 个编译阻断错误** 和 **4 个功能断裂问题**，项目无法编译通过，核心用户流程（文件管理、文本编辑、APK 分析、终端）均存在导航断裂。必须修复所有 P0 后重新审计。

---

## P0 — 阻断发布问题（共 9 项）

### 编译阻断（5 项）

| # | 文件 | 问题 | 详情 |
|---|------|------|------|
| P0-1 | `AppDexNavHost.kt` → `FileManagerScreen` | **签名不匹配** | NavHost 传入 `onOpenTextFile`、`onOpenHexFile`、`onNavigateToTab` 三个参数，但 `FileManagerScreen` 签名仅接受 `viewModel`。**编译错误。** |
| P0-2 | `AppDexNavHost.kt` → `EditorScreen` | **签名不匹配** | NavHost 传入 `filePath` 和 `onBack`，但 `EditorScreen` 签名仅接受 `viewModel`。**编译错误。** |
| P0-3 | `AppDexNavHost.kt` → `ApkAnalyzerScreen` | **签名不匹配** | NavHost 传入 `onOpenDetail`，但 `ApkAnalyzerScreen` 签名仅接受 `viewModel`。**编译错误。** |
| P0-4 | `AppDexNavHost.kt` → `TerminalScreen` | **签名不匹配** | NavHost 传入 `onBack`，但 `TerminalScreen` 签名仅接受 `workingDir: String`。**编译错误。** |
| P0-5 | `SigningScreen.kt` L132 | **未定义变量** | 使用 `apkFilePicker.launch(...)` 但 `apkFilePicker` 从未通过 `rememberLauncherForActivityResult` 定义。**编译错误。** |

### 功能断裂（4 项）

| # | 文件 | 问题 | 详情 |
|---|------|------|------|
| P0-6 | `EditorScreen.kt` | **文件不加载** | 即使修复签名添加 `filePath` 参数，`EditorScreen` 内部也**从未调用** `viewModel.openFileIfProvided(filePath)`。ViewModel 中已实现该方法但无调用方。用户打开文本文件将看到空白编辑器。 |
| P0-7 | `FileManagerScreen.kt` L269, L662-665 | **文本文件不打开内置编辑器** | `emitOpenEditor()` 函数体仅调用 `openFile(context, file)`，后者通过 `Intent.ACTION_VIEW` 打开外部应用。内置编辑器永远不会被使用。`onOpenTextFile` 回调未接入。 |
| P0-8 | `ApkAnalyzerScreen.kt` | **无法导航到详情页** | `ApkDetailScreen` 已实现且功能完整（含安全评分、Tab 切换、快捷入口），但 `ApkAnalyzerScreen` 没有任何 UI 元素或回调触发 `onOpenDetail` 导航。用户分析 APK 后无法进入详情页。 |
| P0-9 | `FileManagerScreen.kt` L667-680 | **`file://` URI 崩溃** | `openFile()` 使用 `Uri.parse(file.path)` 构造 `file://` URI 并通过 `Intent.ACTION_VIEW` 启动外部 Activity。Android 7+ (API 24+, minSdk=26) 会抛出 `FileUriExposedException`。虽然被 try-catch 包裹不会崩溃，但用户点击未知类型文件时**静默无响应**。应使用 FileProvider。 |

---

## P1 — 发布前必须修复（共 18 项）

### 生命周期与状态恢复（3 项）

| # | 范围 | 问题 | 影响 |
|---|------|------|------|
| P1-1 | 全局 ViewModel | **无 SavedStateHandle** | 全项目零处使用 `SavedStateHandle`。`BaseViewModel` 不支持进程死亡恢复。APK 分析状态、编辑器内容、文件管理器路径在进程被杀后全部丢失。 |
| P1-2 | `FileManagerViewModel` | **currentPath 不持久化** | 旋转屏幕可通过 `configChanges` 避免，但进程死亡后 `currentPath` 重置为 `/storage/emulated/0`。 |
| P1-3 | `EditorViewModel` | **编辑内容不持久化** | 编辑器内容存储在 `MutableStateFlow` 中，进程死亡后丢失。用户编辑大段文本后被系统杀进程，内容不可恢复。 |

### 安全问题（3 项）

| # | 文件 | 问题 | 影响 |
|---|------|------|------|
| P1-4 | `SigningScreen.kt` L103, L108-111 | **密码 `rememberSaveable` 持久化** | `keystorePassword`、`newKeystorePassword`、`newKeyPassword` 使用 `rememberSaveable`，意味着密码明文写入 Bundle，可能被持久化到磁盘。应使用 `remember`（不持久化）或在 `onPause` 时清除。 |
| P1-5 | `SigningState.kt` L28-29 | **密码存储在 State 中** | `keystorePassword` 和 `keyPassword` 作为 State 字段，存在于 `MutableStateFlow` 中。虽然内存中不可避免，但应确保不被日志输出。 |
| P1-6 | `WebFileServer.kt` | **无身份认证** | 嵌入式 HTTP 服务器无任何认证机制。同一网络下的任何设备均可浏览、下载、上传文件。至少应添加基础密码认证。 |

### 设计系统不一致（3 项）

| # | 文件 | 问题 |
|---|------|------|
| P1-7 | `ApkAnalyzerScreen.kt` L64-69 | 使用 Material3 `TopAppBar` 而非 `AppDexBar`。与全局设计系统不一致。 |
| P1-8 | `TerminalScreen.kt` L113-145 | 使用 Material3 `TopAppBar` 而非 `AppDexBar`。无返回按钮。 |
| P1-9 | `FileManagerScreen.kt` L107-170 | 使用 Material3 `TopAppBar` / `SearchBar` 而非 `AppDexBar` + `AppDexSearchBar`。 |

### 性能问题（4 项）

| # | 文件 | 问题 | 影响 |
|---|------|------|------|
| P1-10 | `TerminalScreen.kt` L63 | **无 scrollback 限制** | `mutableStateListOf<TerminalLine>()` 无上限。`TerminalViewModel` 定义了 `scrollback` 设置（默认 1000），但 `TerminalScreen` 完全不使用 ViewModel，scrollback 配置无效。长时间运行 `find /` 等高输出命令可导致 OOM。 |
| P1-11 | `ApkAnalyzerScreen.kt` L153-230 | **大列表非 Lazy 渲染** | 权限（可能 30+）、Activity（可能 100+）、Service、Receiver、Provider 列表在 `LazyColumn` 的单个 `item {}` 中使用 `forEach` 渲染，全部一次性 compose。大 APK 可导致卡顿。 |
| P1-12 | `ElfViewerRepository.kt` L24 | **整文件读入内存** | `File(filePath).readBytes()` 无大小限制。大型 ELF 文件（如 `libflutter.so` 可达 50MB+）可导致 OOM。 |
| P1-13 | `HashCalculatorScreen.kt` L93 | **整文件读入内存** | `File(it).readBytes()` 用于哈希计算。应使用流式更新 `MessageDigest`。 |

### Crash 风险（2 项）

| # | 文件 | 问题 |
|---|------|------|
| P1-14 | `WebServerScreen.kt` L248 | `qrBitmap!!.asImageBitmap()` — 若 QR 码生成失败，`qrBitmap` 为 null，`!!` 将抛出 `NullPointerException`。 |
| P1-15 | `FileManagerScreen.kt` L362 | `renameTarget!!.path` — 若 `renameTarget` 在异步操作期间被置空，将崩溃。 |

### 错误处理缺失（3 项）

| # | 文件 | 问题 |
|---|------|------|
| P1-16 | `ApkAnalyzerScreen.kt` | 无 Snackbar 系统。错误仅在底部显示原始 `Text`，用户可能看不到。无重试按钮。 |
| P1-17 | `FileManagerScreen.kt` | ViewModel 发出 `FileManagerEffect.ShowToast` 但 Screen 端无 `SnackbarHost` 接收。Toast 消息丢失。 |
| P1-18 | `TerminalScreen.kt` | 无 `onBack` 参数且无返回按钮。用户进入终端后无法返回（依赖系统返回手势，但 `TopAppBar` 无返回箭头）。 |

---

## P2 — 后续优化（共 14 项）

| # | 范围 | 问题 | 建议 |
|---|------|------|------|
| P2-1 | `AndroidManifest.xml` | `MANAGE_EXTERNAL_STORAGE` 权限 | Google Play 政策限制，需提交使用说明或改用 SAF |
| P2-2 | `AndroidManifest.xml` | `QUERY_ALL_PACKAGES` 权限 | Google Play 政策限制，需提供合规声明 |
| P2-3 | `AndroidManifest.xml` | `largeHeap="true"` | Google Play 审核触发点，应优化内存使用后移除 |
| P2-4 | `AndroidManifest.xml` | `requestLegacyExternalStorage="true"` | 已废弃（targetSdk=35），应移除 |
| P2-5 | `SigningScreen.kt` L280, L591 | 硬编码 `/sdcard/` 占位文本 | 应使用 `Environment` 动态获取路径 |
| P2-6 | `ApkDiffScreen.kt` L181, L200 | 硬编码 `/sdcard/` 占位文本 | 同上 |
| P2-7 | `SecurityScannerScreen.kt` L165 | 硬编码 `/sdcard/` 占位文本 | 同上 |
| P2-8 | `DesignSystem.kt` L487 | `Color.White` 硬编码 | Toggle 拇指颜色应使用设计 token |
| P2-9 | `VideoPlayerScreen.kt` L85 | `Color.Black` 硬编码 | 视频播放器背景应使用设计 token |
| P2-10 | `FileManagerViewModel.kt` L40-41 | 硬编码 `/storage/emulated/0` | 应使用 `Environment.getExternalStorageDirectory()` |
| P2-11 | `ArchiveFactory.kt` | 全部方法返回 `Result.failure` | 存档读取/写入功能未实现，为 stub |
| P2-12 | `AnsiColorParser.kt` L15-30 | 16 个硬编码颜色 | 可接受 — ANSI 标准色，不适合设计 token 化 |
| P2-13 | `SyntaxHighlighter.kt` L33 | 1 个硬编码颜色 `Color(0xFFB794F6)` | 可接受 — 语法高亮专用色 |
| P2-14 | `FileManagerScreen.kt` | 多处 `remember { mutableStateOf }` 未用 `rememberSaveable` | 旋转屏幕（若 `configChanges` 未覆盖）状态丢失 |

---

## 已验证通过项

| 类别 | 状态 | 说明 |
|------|------|------|
| `collectAsStateWithLifecycle` | ✅ 通过 | 全项目零处使用 `collectAsState`（无 WithLifecycle） |
| `Toast` 清除 | ✅ 通过 | 全项目零处使用 `Toast.makeText` |
| `Log.d` / `Log.v` 清除 | ✅ 通过 | 无 debug 级别日志输出 |
| `Build.Config.DEBUG` 守卫 | ✅ 通过 | 无条件日志输出 |
| `throw Exception` | ✅ 通过 | 零处直接 throw |
| `TODO` / `NotImplemented` | ✅ 通过 | 零处 |
| 空 catch 块 | ✅ 通过 | 零处空 catch |
| `runBlocking` | ✅ 通过 | 零处主线程阻塞 |
| `Thread.sleep` | ✅ 通过 | 零处 |
| ProGuard / R8 配置 | ✅ 通过 | 规则完整，覆盖所有关键库 |
| 网络安全配置 | ✅ 通过 | 默认 HTTPS，仅本地网络允许明文 |
| Zip Slip 防护 | ✅ 通过 | `FileManagerViewModel.extractArchive()` 使用 `canonicalPath` 校验 |
| Web 服务器路径遍历防护 | ✅ 通过 | `WebFileServer.resolvePath()` 使用 `canonicalPath.startsWith()` 校验 |
| 全局异常处理器 | ✅ 通过 | `AppDexApplication` 设置 `UncaughtExceptionHandler` |
| Splash Screen | ✅ 通过 | 正确配置 `Theme.SplashScreen` |
| FileProvider | ✅ 通过 | 正确配置 `file_paths.xml` |
| Hilt 依赖注入 | ✅ 通过 | `@HiltAndroidApp` + `@AndroidEntryPoint` 正确配置 |
| 主题系统 | ✅ 通过 | Dark/Light/System 三模式完整实现 |
| MVI 架构 | ✅ 通过 | `BaseViewModel<I, S, E>` 架构清晰 |

---

## 各部分审计详情

### 第一部分：首次用户体验

| 检查项 | 结果 | 说明 |
|--------|------|------|
| 首次启动 Crash | ⚠️ 无法验证 | 代码无法编译，无法生成 APK |
| Splash Screen | ✅ | `installSplashScreen()` + 主题配置正确 |
| 权限请求 | ✅ 合理 | `MANAGE_EXTERNAL_STORAGE` 权限页设计专业，有清晰说明 |
| HomeScreen 内容 | ✅ | Hero 卡片 + 快速工具 + 最近分析空状态，无开发者测试文案 |
| FAB 合理性 | ✅ | FAB 指向"分析"功能，合理 |
| 快捷入口可理解性 | ✅ | 6 个快捷工具，文案清晰 |

### 第二部分：功能闭环

| 模块 | 导入 | 分析 | 浏览 | 编辑 | 状态 |
|------|------|------|------|------|------|
| APK Analyzer | ✅ | ✅ | ❌ P0-8 | N/A | **断裂** — 无法进入详情页 |
| File Manager | ✅ | N/A | ✅ | ❌ P0-7 | **断裂** — 文本文件不打开内置编辑器 |
| Editor | ❌ P0-6 | N/A | N/A | ✅ | **断裂** — 文件不加载 |
| Terminal | ✅ | N/A | ✅ | N/A | **断裂** — 无法返回 (P1-18) |
| Signing | ❌ P0-5 | ✅ | N/A | N/A | **断裂** — 无法选择 APK |
| Repackaging | ✅ | ✅ | ✅ | N/A | ✅ 通过 |
| Diff | ✅ | ✅ | ✅ | N/A | ✅ 通过 |
| Security Scanner | ✅ | ✅ | ✅ | N/A | ✅ 通过 |
| Media (图片/音频/视频) | ✅ | N/A | ✅ | N/A | ✅ 通过 |

### 第三部分：Android 生命周期

| 检查项 | 结果 | 说明 |
|--------|------|------|
| Activity 重建 (旋转) | ⚠️ | `configChanges="orientation\|screenSize\|keyboardHidden"` 避免重建，但非所有场景覆盖 |
| `rememberSaveable` 使用 | ✅ | SettingsScreen、ToolsScreen、TerminalScreen (input/history)、SigningScreen 均使用 |
| `remember` (非 Saveable) | ⚠️ P2-14 | 多处 UI 状态使用 `remember` 而非 `rememberSaveable` |
| ViewModel State | ⚠️ P1-1 | `MutableStateFlow` 在进程死亡后丢失 |
| `SavedStateHandle` | ❌ P1-1 | 全项目零使用 |
| APK 路径恢复 | ❌ | 进程死亡后丢失 |
| 编辑内容恢复 | ❌ P1-3 | 进程死亡后丢失 |
| 分析状态恢复 | ❌ | 进程死亡后丢失 |

### 第四部分：权限与存储

| 权限 | 风险等级 | 说明 |
|------|----------|------|
| `INTERNET` | ✅ 合理 | 远程管理功能需要 |
| `ACCESS_NETWORK_STATE` | ✅ 合理 | 网络状态检测 |
| `ACCESS_WIFI_STATE` | ✅ 合理 | Web 服务器 IP 显示 |
| `READ_EXTERNAL_STORAGE` (maxSdk=32) | ✅ 合理 | 旧版兼容 |
| `WRITE_EXTERNAL_STORAGE` (maxSdk=29) | ✅ 合理 | 旧版兼容 |
| `MANAGE_EXTERNAL_STORAGE` | ⚠️ P2-1 | Google Play 政策限制 |
| `FOREGROUND_SERVICE` | ✅ 合理 | Web 服务器前台服务 |
| `FOREGROUND_SERVICE_DATA_SYNC` | ✅ 合理 | Android 14+ 前台服务类型 |
| `WAKE_LOCK` | ✅ 合理 | 服务器运行时保持唤醒 |
| `REQUEST_INSTALL_PACKAGES` | ⚠️ | Google Play 需审核 |
| `QUERY_ALL_PACKAGES` | ⚠️ P2-2 | Google Play 政策限制 |
| `VIBRATE` | ✅ 合理 | 轻量反馈 |
| `POST_NOTIFICATIONS` | ✅ 合理 | Android 13+ |

| 存储方式 | 状态 |
|----------|------|
| Storage Access Framework | ❌ 未使用 — 全部使用直接文件路径 |
| `/sdcard/` 硬编码 | ⚠️ P2-5/6/7 — 仅占位文本 |
| `Environment.getExternalStorageDirectory()` | ✅ 使用正确 |
| FileProvider | ✅ 配置正确 |
| DataStore (Preferences) | ✅ 设置持久化 |

### 第五部分：性能扫描

| 检查项 | 结果 | 文件 |
|--------|------|------|
| 大列表非 Lazy | ⚠️ P1-11 | `ApkAnalyzerScreen` — 权限/Activity 列表在 `item {}` 中 `forEach` |
| `LineNumberColumn` | ⚠️ | `EditorScreen` — 使用 `Column` + `for` 循环，非 `LazyColumn`（5MB 限制下可接受） |
| 不必要 recomposition | ⚠️ | `HomeScreen` 快捷工具使用 `chunked(2).forEach` 而非 `LazyRow`（固定 6 项，可接受） |
| 主线程 IO | ✅ 通过 | 所有文件操作在 `Dispatchers.IO` |
| 大 APK (100MB+) | ✅ | `ApkAnalyzerViewModel` 流式复制到 cache，无全量加载 |
| 大文件读取 | ⚠️ P1-12/13 | ELF 和 Hash 计算整文件读入内存 |
| Terminal 大输出 | ⚠️ P1-10 | 无 scrollback 限制 |
| `largeHeap` | ⚠️ P2-3 | 掩盖内存泄漏风险 |

### 第六部分：Crash 风险扫描

| 模式 | 数量 | 详情 |
|------|------|------|
| `throw Exception` | 0 | ✅ |
| `RuntimeException` | 0 | ✅ |
| `!!.` (生产代码) | 2 | P1-14 (`qrBitmap!!`), P1-15 (`renameTarget!!`) |
| `!!.` (测试代码) | 1 | 可接受 |
| `lateinit` (生产) | 1 | `AppDexActivity.settingsRepository` — Hilt 注入，安全 |
| `lateinit` (测试) | 1 | 可接受 |
| `catch(Exception)` | ~25 | 全部有 `Log.w` 日志 ✅ |
| 空 catch | 0 | ✅ |
| `TODO` / `NotImplemented` | 0 | ✅ |
| `runBlocking` | 0 | ✅ |
| `Thread.sleep` | 0 | ✅ |

### 第七部分：安全审计

| 检查项 | 状态 | 说明 |
|--------|------|------|
| Zip Slip | ✅ 通过 | `FileManagerViewModel` 使用 canonicalPath 校验 |
| 路径遍历 (Web) | ✅ 通过 | `WebFileServer.resolvePath()` 校验 |
| 任意文件读取 | ✅ 通过 | Web 服务器限制在 rootPath 内 |
| 临时文件泄漏 | ✅ 通过 | `RepackagingRepository` 在 `finally` 中 `deleteRecursively()` |
| 明文密码 | ⚠️ P1-4 | `rememberSaveable` 将密码持久化到 Bundle |
| Keystore 密码处理 | ⚠️ P1-5 | 密码在 State 中，使用 `PasswordVisualTransformation` 遮蔽 |
| Web 服务器认证 | ❌ P1-6 | 无任何认证 |
| `file://` URI 泄露 | ⚠️ P0-9 | `FileManagerScreen.openFile()` 使用 file:// URI |
| 网络安全配置 | ✅ 通过 | 默认 HTTPS，仅本地允许明文 |
| SQL 注入 | ✅ 通过 | 使用 Room + 参数化查询 |

### 第八部分：Google Play 发布检查

| 检查项 | 状态 | 值 |
|--------|------|-----|
| Application ID | ✅ | `com.appdex` |
| VersionCode | ✅ | `7` |
| VersionName | ✅ | `1.0.0` |
| compileSdk | ✅ | `35` |
| minSdk | ✅ | `26` |
| targetSdk | ✅ | `35` |
| Release Build (R8) | ✅ | `isMinifyEnabled = true`, `isShrinkResources = true` |
| ProGuard 规则 | ✅ | 完整覆盖所有依赖 |
| Debug 代码 | ✅ | 无 `Log.d`/`Log.v`，无 `BuildConfig.DEBUG` 条件 |
| App Icon | ✅ | `@mipmap/ic_launcher` + `@mipmap/ic_launcher_round` |
| Splash Screen | ✅ | `Theme.AppDex.Splash` 配置正确 |
| `largeHeap` | ⚠️ P2-3 | `true` — 审核触发点 |
| `requestLegacyExternalStorage` | ⚠️ P2-4 | `true` — 已废弃 |
| `MANAGE_EXTERNAL_STORAGE` | ⚠️ P2-1 | 需提交使用说明 |
| `QUERY_ALL_PACKAGES` | ⚠️ P2-2 | 需提供合规声明 |
| `allowBackup` | ✅ | `true` — 可接受 |

---

## 推荐修复优先级

### 阶段一：修复编译（P0-1 ~ P0-5）— 阻断
1. 为 `FileManagerScreen` 添加 `onOpenTextFile`、`onOpenHexFile`、`onNavigateToTab` 参数
2. 为 `EditorScreen` 添加 `filePath`、`onBack` 参数 + `LaunchedEffect` 调用 `openFileIfProvided`
3. 为 `ApkAnalyzerScreen` 添加 `onOpenDetail` 参数 + UI 触发按钮
4. 为 `TerminalScreen` 添加 `onBack` 参数 + 返回按钮
5. 为 `SigningScreen` 添加 `rememberLauncherForActivityResult` 定义 `apkFilePicker`

### 阶段二：修复功能断裂（P0-6 ~ P0-9）
6. `EditorScreen` 添加 `LaunchedEffect(filePath)` 调用 `viewModel.openFileIfProvided(filePath)`
7. `FileManagerScreen.emitOpenEditor()` 改为调用 `onOpenTextFile(file.path)`
8. `ApkAnalyzerScreen` 添加 "查看详情" 按钮
9. `FileManagerScreen.openFile()` 改用 FileProvider URI

### 阶段三：修复安全与状态（P1-1 ~ P1-6）
10. `BaseViewModel` 集成 `SavedStateHandle`
11. `SigningScreen` 密码改用 `remember`（非 Saveable）
12. `WebFileServer` 添加基础认证

### 阶段四：性能与一致性（P1-7 ~ P1-18）
13. 统一 `TopAppBar` → `AppDexBar`
14. `TerminalScreen` 接入 `TerminalViewModel` + scrollback 限制
15. `ApkAnalyzerScreen` 权限列表改用 `LazyColumn` items
16. 修复 `!!.` crash 风险
17. 补充 Snackbar 系统

---

## Release Checklist

- [ ] 修复全部 P0-1 ~ P0-9（9 项）
- [ ] 项目可成功编译 `./gradlew assembleDebug`
- [ ] 项目可成功编译 `./gradlew assembleRelease`
- [ ] 修复全部 P1（18 项）
- [ ] APK 安装后首次启动无 Crash
- [ ] APK 分析完整流程可走通（导入 → 分析 → 详情 → DEX → 签名）
- [ ] 文件管理器可打开文本文件到内置编辑器
- [ ] 终端可返回上一页
- [ ] 签名流程可选择 APK 文件
- [ ] 进程死亡后关键状态可恢复
- [ ] 无明文密码持久化
- [ ] Google Play 政策权限有合规声明
- [ ] 重新审计通过（评级 ≥ B）

---

*本报告由静态代码扫描生成，未修改任何代码。等待下一轮修复指令。*
