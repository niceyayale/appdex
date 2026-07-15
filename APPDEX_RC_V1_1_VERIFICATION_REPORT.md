# AppDex Release Candidate v1.1 Verification Report

**Date**: 2026-07-13  
**Verifier**: Automated Static + Build Verification  
**Scope**: Full RC Audit — Build / Lint / Static Scan / Flow / Lifecycle / Stress  

---

## 第一部分：Build 验证

### Debug Build

| Item | Result |
|------|--------|
| Command | `.\gradlew.bat assembleDebug` |
| Status | ❌ **FAILED** |
| Error | `TerminalScreen.kt:153:19 Expecting an element` |
| Task | `:feature:feature-terminal:kspDebugKotlin FAILED` |
| Root Cause | Line 152 `containerColor = MaterialTheme.colorScheme.surface` 缺少尾部逗号，导致 `bottomBar = {` 无法解析 |
| Time | 19s |

### Release Build

| Item | Result |
|------|--------|
| Command | `.\gradlew.bat assembleRelease` |
| Status | ❌ **FAILED** |
| Error | 同 Debug — `TerminalScreen.kt:153:19 Expecting an element` |
| Task | `:feature:feature-terminal:kspReleaseKotlin FAILED` |
| Warning | `FileManagerScreen.kt:676` — `Icons.Filled.InsertDriveFile` deprecated, 应使用 `Icons.AutoMirrored.Filled.InsertDriveFile` |
| Time | 1m 17s |

#### Release 配置检查

| Config | Status | Note |
|--------|--------|------|
| `isMinifyEnabled` | ✅ `true` | R8 代码混淆启用 |
| `isShrinkResources` | ✅ `true` | 资源压缩启用 |
| `proguardFiles` | ✅ | `proguard-android-optimize.txt` + `proguard-rules.pro`（规则完整，覆盖 Hilt/Serialization/Compose/Room/BouncyCastle 等） |
| `signingConfig` | ❌ **缺失** | Release build 未配置签名 → 即使编译通过也只会生成 unsigned APK，无法直接安装 |

---

## 第二部分：Lint 验证

| Item | Result |
|------|--------|
| Command | `.\gradlew.bat lint` |
| Status | ❌ **FAILED** |
| Error | 编译错误阻断 lint 执行 — `:feature:feature-terminal:kspDebugKotlin FAILED` |
| Note | Lint 无法运行，因 TerminalScreen.kt 编译失败。需先修复 P0 编译错误后重新执行。 |

---

## 第三部分：静态风险扫描

全仓 `.kt` 文件扫描结果：

### 扫描结果汇总

| Pattern | 命中数 | 分类 |
|---------|--------|------|
| `!!` (非空断言) | 7 (4 生产 / 3 测试) | 可以接受 |
| `lateinit` | 3 (1 生产 / 1 正则字符串 / 1 测试) | 误报 + 可以接受 |
| `throw RuntimeException` | 0 | — |
| `throw Exception` | 0 | — |
| `NotImplementedError` | 0 | — |
| `TODO` | 0 | — |
| `FIXME` | 0 | — |
| `catch(Exception){}` (空 catch) | 0 | — |
| `Color.White` | 1 | 可以接受 |
| `Color.Black` | 0 | — |
| `remember {` | 23 | 可以接受 |
| `collectAsState(` | 1 | 必须修复（应使用 `collectAsStateWithLifecycle`） |
| `catch(Exception)` (非空) | 28 | 可以接受（全部含 `Log.w` 或 error handling） |
| `printStackTrace()` | 0 | — |
| `file://` URI | 0 | — |
| `runBlocking` | 0 | — |
| `rememberSaveable.*password` | 0 | — |

### 分类详情

#### ✅ 可以接受

1. **`!!` 非空断言** (4 处生产代码)：
   - `ToolsScreen.kt:96` — `selectedPlugin!!`（UI 状态保证非空）
   - `ApkAnalyzerScreen.kt:131` — `state.apkInfo!!`（上方有 null 检查守卫）
   - `FileManagerScreen.kt:239` — `state.error!!`（在 `state.error != null` 分支内）
   - `EditorScreen.kt:178` — `state.error!!`（在 `state.error != null` 分支内）

2. **`lateinit`** (1 处生产代码)：
   - `AppDexActivity.kt:65` — `lateinit var settingsRepository`（Hilt `@Inject` 注入，安全）

3. **`Color.White`** (1 处)：
   - `AnsiColorParser.kt:118` — `colorMap[n] ?: Color.White`（终端 ANSI 颜色回退，合理）

4. **`catch(Exception)` 非空** (28 处)：全部包含 `Log.w("AppDex", "Suppressed exception", e)` 或 error state 更新

5. **`remember {`** (23 处)：标准 Compose 模式，用于 `SnackbarHostState`、`mutableStateOf` 等

#### ❌ 误报

1. **`lateinit` in SyntaxHighlighter.kt:49** — 出现在正则表达式字符串中，非实际 `lateinit` 关键字使用

#### 🔧 必须修复

1. **`collectAsState(`** — `TerminalScreen.kt:65`：
   ```kotlin
   val scrollbackLimit by viewModel.scrollback.collectAsState()
   ```
   应改为 `collectAsStateWithLifecycle()` 以与 Lifecycle 绑定，避免后台不必要的收集

---

## 第四部分：核心用户流程测试（静态验证）

> **注意**：由于编译失败（P0），无法生成 APK 安装到模拟器。以下为深度静态代码审查结果。

### Flow 1：首页

| 检查项 | 状态 | 说明 |
|--------|------|------|
| 启动流程 | ✅ | `AppDexActivity` → 权限检查 → `AppDexApp()` |
| 权限处理 | ✅ | `MANAGE_EXTERNAL_STORAGE` 请求 + `PermissionScreen` |
| EmptyState | ✅ | `HomeScreen` 支持空状态显示 |
| FAB / 导航 | ✅ | 4 tab 底部导航 + 子路由导航 |
| 主题切换 | ✅ | `SettingsRepository.themeMode` → `AppDexTheme` |

### Flow 2：APK 分析

| 检查项 | 状态 | 说明 |
|--------|------|------|
| 导入 APK | ✅ | `OpenDocument` launcher → `ApkAnalyzerIntent.OpenApk(uri)` |
| 分析流程 | ✅ | 复制到 cache → `ApkFile.parse()` → `enrichWithPackageManager` |
| 详情导航 | ✅ | `onOpenDetail()` → `Route.ApkDetail`，共享 ViewModel |
| Manifest | ✅ | `info.manifest` 显示包名、版本、SDK 等 |
| 权限列表 | ✅ | `LazyColumn` + `items(permissions)` 懒加载 |
| 签名信息 | ✅ | `info.signatures` 显示算法、证书、有效期 |
| DEX / 资源 | ✅ | `info.entries` 文件列表懒加载 |
| 错误处理 | ✅ | `ErrorState` + `SnackbarHost` |

### Flow 3：文件管理

| 检查项 | 状态 | 说明 |
|--------|------|------|
| 目录浏览 | ✅ | `NavigateTo` / `NavigateUp` 正确实现 |
| 搜索 | ✅ | `walkTopDown().maxDepth(5)` + 200 结果限制 |
| FileProvider | ✅ | `FileProvider.getUriForFile()` 正确使用，authority 匹配 |
| Snackbar 反馈 | ✅ | `LaunchedEffect` 收集 effects → `snackbarHostState.showSnackbar` |
| 书签 | ✅ | Room 持久化 + `LazyRow` 展示 |
| 文件操作 | ✅ | 复制/移动/压缩/解压/重命名/删除 |
| Zip Slip 防护 | ✅ | `canonicalPath.startsWith()` 检查 |

### Flow 4：编辑器

| 检查项 | 状态 | 说明 |
|--------|------|------|
| 打开文件 | ✅ | `EditorScreen(filePath)` → `openFileIfProvided(filePath)` |
| 导航路径 | ✅ | `FileManagerScreen.onOpenTextFile` → `Route.Editor(filePath)` |
| 修改保存 | ✅ | `EditorIntent.Save` → `File.writeText()` |
| 文件大小限制 | ✅ | 5MB 上限检查 |
| 语法高亮 | ✅ | `SyntaxHighlightTransformation` |
| 错误展示 | ✅ | `state.error` 显示在状态栏 |
| 保存反馈 | ✅ | Snackbar "已保存" |

### Flow 5：签名

| 检查项 | 状态 | 说明 |
|--------|------|------|
| APK 选择 | ✅ | `OpenDocument` launcher |
| Keystore 加载 | ✅ | `SigningIntent.LoadKeystore` |
| 密码输入 | ✅ | `PasswordVisualTransformation` + `KeyboardType.Password` |
| 签名方案 | ✅ | V1/V2/V3 可选切换 |
| 错误 Snackbar | ✅ | `SigningEffect.ShowError` → `snackbarHostState.showSnackbar` |
| 密码持久化 | ✅ | 使用 `remember`（非 `rememberSaveable`）— 安全实践 |
| **⚠️ keystorePath** | ⚠️ | 使用 `remember` 而非 `rememberSaveable`，进程死亡后丢失 |

### Flow 6：Terminal

| 检查项 | 状态 | 说明 |
|--------|------|------|
| **编译** | ❌ | `TerminalScreen.kt:152` 缺少逗号 — **阻断编译** |
| **HorizontalDivider** | ❌ | `TerminalScreen.kt:227` 使用 `HorizontalDivider` 但缺少 `import androidx.compose.material3.HorizontalDivider` |
| ViewModel 集成 | ✅ | `TerminalViewModel` 提供 `scrollback` 配置 |
| scrollback 限制 | ✅ | `while (lines.size > limit) { lines.removeAt(0) }` (lines 99-102) |
| 滚动节流 | ✅ | 50ms delay 批量滚动 (lines 104-111) |
| 命令历史 | ✅ | ↑/↓ 浏览，`rememberSaveable` 持久化 `historyIndex` |
| Ctrl+C / Ctrl+Z | ✅ | `session.kill()` 正确实现 |
| 输出流式读取 | ✅ | `BufferedReader.readLine()` 逐行读取 |

### Flow 7：WebFileServer

| 检查项 | 状态 | 说明 |
|--------|------|------|
| 服务启动 | ✅ | `ServerSocket` + 线程池 |
| Bearer Token 代码 | ✅ | `WebFileServer.kt:94-101` 实现了认证检查 |
| **Token 传递** | ❌ | `WebServerScreen.kt:90` 创建 `WebFileServer(context, rootPath, actualPort)` **未传入 authToken** → 认证被跳过 |
| 路径遍历防护 | ✅ | `resolvePath()` 检查 `canonicalPath.startsWith(root.canonicalPath)` |
| HTML 转义 | ✅ | `escapeHtml()` 处理 `& < > " '` |
| QR 码生成 | ✅ | `QrCodeGenerator.generate()` |
| 生命周期清理 | ✅ | `DisposableEffect { onDispose { serverInstance?.stop() } }` |

---

## 第五部分：生命周期测试（静态验证）

| 检查项 | 状态 | 说明 |
|--------|------|------|
| Activity configChanges | ✅ | `orientation\|screenSize\|keyboardHidden` — 旋转不重建 Activity |
| BaseViewModel SavedStateHandle | ✅ | 架构支持，`saveState()` / `restoreState()` 方法已实现 |
| **ViewModel 实际注入** | ❌ | `ApkAnalyzerViewModel`、`FileManagerViewModel`、`EditorViewModel` 均未注入 `SavedStateHandle` → 进程死亡后状态丢失 |
| Terminal input 持久化 | ✅ | `rememberSaveable { mutableStateOf("") }` |
| Terminal historyIndex | ✅ | `rememberSaveable { mutableStateOf(-1) }` |
| WebServer port/status | ✅ | `rememberSaveable` |
| Signing 密码 | ✅ | `remember`（安全，不持久化密码） |
| **Signing keystorePath** | ⚠️ | `remember` 而非 `rememberSaveable` — 进程死亡后丢失 |

---

## 第六部分：大文件压力测试（静态验证）

| 模块 | 流式处理 | 大小限制 | 状态 |
|------|----------|----------|------|
| HashCalculator | ✅ 8192-byte buffer 流式读取 | 500MB 上限 | ✅ |
| EditorViewModel | ❌ `file.readText()` 全量读取 | 5MB 上限 | ✅ (限制保护) |
| TerminalSession | ✅ `BufferedReader.readLine()` 逐行 | 无限制 | ⚠️ (scrollback 限制保护) |
| ApkAnalyzer | ✅ 复制到 cache 后解析 | 无限制 | ⚠️ (大 APK 可能 OOM) |
| FileManager 搜索 | ✅ `walkTopDown().maxDepth(5)` | 200 结果上限 | ✅ |
| FileManager 压缩 | ✅ 8192-byte buffer | 无限制 | ✅ |
| FileManager 解压 | ✅ 8192-byte buffer + Zip Slip 防护 | 无限制 | ✅ |
| WebFileServer 下载 | ✅ `inputStream().use { it.copyTo(output) }` | 无限制 | ✅ |

---

## 第七部分：Remaining Issues

### P0 — 必须修复（阻断发布）

| # | 文件 | 行号 | 问题 | 影响 |
|---|------|------|------|------|
| 1 | `TerminalScreen.kt` | 152 | `containerColor = MaterialTheme.colorScheme.surface` 缺少尾部逗号 | 编译失败，整个项目无法构建 |
| 2 | `TerminalScreen.kt` | 227 | 使用 `HorizontalDivider` 但未 import `androidx.compose.material3.HorizontalDivider` | 编译失败（修复 #1 后会暴露） |

### P1 — 应当修复（影响功能/安全）

| # | 文件 | 行号 | 问题 | 影响 |
|---|------|------|------|------|
| 1 | `WebServerScreen.kt` | 90 | `WebFileServer` 创建时未传入 `authToken` | Bearer Token 认证代码存在但从未激活，服务器无认证运行 |
| 2 | `app/build.gradle.kts` | 24-31 | Release build 未配置 `signingConfig` | 即使编译通过，也只能生成 unsigned APK，无法直接安装 |
| 3 | `ApkAnalyzerViewModel.kt` | 26-29 | 未注入 `SavedStateHandle` | 进程死亡后分析结果丢失 |
| 4 | `FileManagerViewModel.kt` | 30-34 | 未注入 `SavedStateHandle` | 进程死亡后文件浏览位置丢失 |
| 5 | `EditorViewModel.kt` | 21-24 | 未注入 `SavedStateHandle` | 进程死亡后编辑内容丢失 |
| 6 | `TerminalScreen.kt` | 65 | 使用 `collectAsState()` 而非 `collectAsStateWithLifecycle()` | 后台不必要的状态收集 |
| 7 | `FileManagerScreen.kt` | 676 | `Icons.Filled.InsertDriveFile` 已废弃 | Lint warning，应使用 `Icons.AutoMirrored.Filled.InsertDriveFile` |

### P2 — 可以优化（次要问题）

| # | 文件 | 行号 | 问题 | 影响 |
|---|------|------|------|------|
| 1 | `SigningScreen.kt` | 114 | `keystorePath` 使用 `remember` 而非 `rememberSaveable` | 进程死亡后路径丢失（旋转不受影响） |
| 2 | `TerminalScreen.kt` | 72 | `isRunning` 使用 `remember` 而非 `rememberSaveable` | 进程死亡后运行状态重置 |
| 3 | 多处 | — | `!!` 非空断言 (4 处生产代码) | 虽有 null 检查守卫，但可使用更安全的 `let {}` 模式 |
| 4 | `AnsiColorParser.kt` | 118 | `Color.White` 作为终端颜色回退 | 终端场景可接受，但硬编码颜色不适配暗色主题 |

---

## Release Recommendation

### 结论：❌ NOT READY

**阻断原因**：

1. **P0 编译错误**：`TerminalScreen.kt` 存在 2 个编译错误（缺少逗号 + 缺少 import），导致整个项目无法构建。Debug/Release APK 均无法生成。
2. **P1 安全缺陷**：`WebFileServer` 的 Bearer Token 认证虽已实现但从未被激活，Web 服务器在无认证状态下运行。
3. **P1 发布配置**：Release build 缺少 `signingConfig`，无法生成可安装的签名 APK。
4. **Lint 无法执行**：因编译失败，Lint 检查完全无法运行。

**建议下一步**：

开启 **AppDex RC Fix Sprint**，按以下顺序修复：

1. 修复 `TerminalScreen.kt` P0 编译错误（2 处）
2. 修复 `WebServerScreen.kt` authToken 传递
3. 配置 Release `signingConfig`
4. 为核心 ViewModel 注入 `SavedStateHandle`
5. 修复 `collectAsState` → `collectAsStateWithLifecycle`
6. 修复 deprecated icon 警告
7. 重新执行全量验证（Build + Lint + Static Scan）
8. 安装到模拟器执行真实用户流程测试

---

*Report generated at 2026-07-13T13:45:00Z*
