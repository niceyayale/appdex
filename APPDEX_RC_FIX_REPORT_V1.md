# AppDex RC Fix Report v1

**Date**: 2026-07-13  
**Sprint**: AppDex Release Candidate Fix Sprint  
**Goal**: 从 NOT READY 推进到可 Build、可安装、可测试的 RC 状态  

---

## 1. 修改文件列表

| # | 文件 | 修改类型 |
|---|------|----------|
| 1 | `feature/feature-terminal/.../TerminalScreen.kt` | P0 语法修复 + import 补充 + collectAsState 替换 |
| 2 | `feature/feature-remote/.../WebServerScreen.kt` | P1 authToken 认证激活 |
| 3 | `app/build.gradle.kts` | P1 Release signingConfig 配置 |
| 4 | `core/core-arch/.../BaseViewModel.kt` | P1 SavedStateHandle 安全化 |
| 5 | `feature/feature-files/.../FileManagerViewModel.kt` | P1 SavedStateHandle 注入 |
| 6 | `feature/feature-editor/.../EditorViewModel.kt` | P1 SavedStateHandle 注入 |
| 7 | `feature/feature-analyzer/.../ApkAnalyzerViewModel.kt` | P1 SavedStateHandle 注入 |
| 8 | `feature/feature-files/.../FileManagerScreen.kt` | P1 废弃 Icon 替换 |

---

## 2. 每个问题的修改详情

### P0-1: TerminalScreen.kt:152 — containerColor 缺少逗号

**原问题**:  
`containerColor = MaterialTheme.colorScheme.surface` 后缺少逗号，导致 `bottomBar = {` 无法解析，编译报错 `Expecting an element`。

**修改方式**:  
在 `containerColor = MaterialTheme.colorScheme.surface` 后添加逗号。

**影响范围**: `TerminalScreen.kt` — Scaffold 参数语法。不改变 UI 行为。

---

### P0-2: TerminalScreen.kt:227 — 缺少 HorizontalDivider import

**原问题**:  
代码中使用 `HorizontalDivider` 但未 import `androidx.compose.material3.HorizontalDivider`，修复 P0-1 后会暴露此错误。

**修改方式**:  
添加 `import androidx.compose.material3.HorizontalDivider`。

**影响范围**: `TerminalScreen.kt` — import 声明。无行为变化。

---

### P1-3: WebServerScreen.kt:90 — WebFileServer 未传入 authToken

**原问题**:  
`WebFileServer` 构造函数支持 `authToken: String?` 参数且包含 Bearer Token 认证逻辑，但 `WebServerScreen` 创建实例时未传入 token，导致认证被完全跳过。

**修改方式**:
- 添加 `enableAuth` 开关（`rememberSaveable`，默认 `true`）
- 添加 `authToken` 状态变量
- 在 `startServer()` 中，若 `enableAuth` 为 true，使用 `UUID.randomUUID()` 生成 16 字符 token 并传入 `WebFileServer`
- 添加 UI：认证开关 + 运行中 token 显示卡片（含复制按钮）
- 添加 `import androidx.compose.material3.Switch`

**影响范围**: `WebServerScreen.kt` — 新增认证 UI 和逻辑。不影响已有功能。认证默认开启，用户可手动关闭。

---

### P1-4: app/build.gradle.kts — Release 无 signingConfig

**原问题**:  
Release buildType 未配置 `signingConfig`，导致只能生成 unsigned APK，无法直接安装。

**修改方式**:
- 添加 `signingConfigs { create("release") { ... } }` 块
- 支持环境变量覆盖：`APPDEX_STORE_FILE`、`APPDEX_STORE_PASSWORD`、`APPDEX_KEY_ALIAS`、`APPDEX_KEY_PASSWORD`
- 未设置时回退到 debug keystore（`~/.android/debug.keystore`），确保开发环境可直接生成签名 APK
- 在 `release` buildType 中添加 `signingConfig = signingConfigs.getByName("release")`

**影响范围**: `app/build.gradle.kts` — 签名配置。**生产环境需替换为真实 keystore**。

---

### P1-5: 3 个核心 ViewModel 未注入 SavedStateHandle

**原问题**:  
`FileManagerViewModel`、`EditorViewModel`、`ApkAnalyzerViewModel` 均未注入 `SavedStateHandle`，进程死亡后核心用户状态丢失。

**修改方式**:

1. **BaseViewModel.kt**: `update()` 方法中 `savedStateHandle?.set()` 调用添加 `try-catch`，防止非序列化 state 导致崩溃。

2. **FileManagerViewModel.kt**:
   - 构造函数添加 `savedStateHandle: SavedStateHandle` 参数，传递给 `BaseViewModel`
   - `navigateTo()` 中调用 `saveState("current_path", path)` 保存路径
   - `init` 中当 `rememberLastPath` 为 false 时从 `restoreState("current_path", ...)` 恢复

3. **EditorViewModel.kt**:
   - 构造函数添加 `savedStateHandle: SavedStateHandle` 参数，传递给 `BaseViewModel`
   - `openFile()` 中调用 `saveState("editor_file_path", path)` 保存路径
   - `openFileIfProvided()` 中当 `filePath` 为 null 时从 `restoreState("editor_file_path", "")` 恢复

4. **ApkAnalyzerViewModel.kt**:
   - 构造函数添加 `savedStateHandle: SavedStateHandle` 参数，传递给 `BaseViewModel`
   - `openApk()` 成功后调用 `saveState("apk_file_path", ...)` 保存路径
   - `Clear` intent 中调用 `savedStateHandle?.remove<String?>("apk_file_path")` 清除

**影响范围**: 3 个 ViewModel + BaseViewModel。仅保存核心路径状态，不迁移全部 state。不影响已有功能逻辑。

---

### P1-6: TerminalScreen.kt:65 — collectAsState 替换

**原问题**:  
`viewModel.scrollback.collectAsState()` 应使用 `collectAsStateWithLifecycle()` 以绑定 Lifecycle。

**修改方式**:
- 替换 import：`androidx.compose.runtime.collectAsState` → `androidx.lifecycle.compose.collectAsStateWithLifecycle`
- 替换调用：`collectAsState()` → `collectAsStateWithLifecycle()`

**影响范围**: `TerminalScreen.kt` — Flow 收集方式。不改变状态逻辑。

---

### P1-7: FileManagerScreen.kt:676 — 废弃 Icons.Filled.InsertDriveFile

**原问题**:  
`Icons.Default.InsertDriveFile` 已废弃，应使用 `Icons.AutoMirrored.Filled.InsertDriveFile`。

**修改方式**:
- 添加 import：`androidx.compose.material.icons.automirrored.filled.InsertDriveFile`
- 替换调用：`Icons.Default.InsertDriveFile` → `Icons.AutoMirrored.Filled.InsertDriveFile`

**影响范围**: `FileManagerScreen.kt` — 图标引用。视觉一致。

---

## 3. Build 结果

### Debug Build

| Item | Result |
|------|--------|
| Command | `.\gradlew.bat assembleDebug` |
| Status | ✅ **BUILD SUCCESSFUL** |
| Time | 1m 6s |
| Tasks | 976 actionable (131 executed, 845 up-to-date) |
| Output | `app/build/outputs/apk/debug/app-debug.apk` |

### Release Build

| Item | Result |
|------|--------|
| Command | `.\gradlew.bat assembleRelease` |
| Status | ✅ **BUILD SUCCESSFUL** |
| Output | `app/build/outputs/apk/release/app-release.apk` (12.7 MB) |
| R8/Proguard | ✅ `isMinifyEnabled = true` |
| Resource Shrinking | ✅ `isShrinkResources = true` |
| Signing | ✅ Debug keystore fallback (生产需替换) |

---

## 4. Lint 结果

| Item | Result |
|------|--------|
| Command | `.\gradlew.bat lintDebug` |
| Status | ⚠️ 预存问题导致 lint 失败（非本次修复引入） |
| Errors | 4 (全部预存) |
| Warnings | 39 (全部预存) |
| **新增 errors** | **0** ✅ |
| **新增 warnings** | **0** ✅ |
| Unused imports | 0 ✅ |
| Unresolved references | 0 ✅ |

### 预存 Lint Errors（非本次引入）

| File | Issue | Type |
|------|-------|------|
| `AppDexNavHost.kt:116,121,327,361` | `popUpTo` restricted API (4处) | RestrictedApi |
| `ApkAnalyzerViewModel.kt:130` | `longVersionCode` requires API 28 (minSdk 26) | NewApi |

### 预存 Lint Warnings（部分）

| Category | Count | Description |
|----------|-------|-------------|
| ObsoleteLintCustomCheck | 3 | Navigation lint checks out of date |
| OldTargetApi | 1 | targetSdk 35 (not latest) |
| TrustAllX509TrustManager | 3 | BouncyCastle / commons-net |
| ObsoleteSdkInt | 1 | mipmap-anydpi-v26 unnecessary |
| UnusedResources | 8+ | Unused string resources |

---

## 5. 剩余风险

| Priority | Issue | Risk | Recommendation |
|----------|-------|------|----------------|
| P1 (预存) | `popUpTo` restricted API (4处) | Lint 阻断 | 添加 `@SuppressLint("RestrictedApi")` 或迁移到非受限 API |
| P1 (预存) | `longVersionCode` API 28 | minSdk 26 崩溃风险 | 添加 `Build.VERSION.SDK_INT >= 28` 检查 |
| P2 (预存) | ObsoleteLintCustomCheck | Navigation lint checks 跳过 | 升级 navigation 依赖版本 |
| P2 (预存) | TrustAllX509TrustManager | BouncyCastle/commons-net TLS 信任 | 第三方库内部实现，无法直接修复 |
| P2 (预存) | UnusedResources | 8+ 未使用字符串资源 | 清理或保留 |
| **生产部署** | Release signing 使用 debug keystore | 安全风险 | 设置 `APPDEX_STORE_FILE` 等环境变量指向真实 keystore |

---

## 结论

✅ **Debug Build**: BUILD SUCCESSFUL  
✅ **Release Build**: BUILD SUCCESSFUL (signed APK 12.7MB)  
✅ **本次修复**: 0 新增 errors, 0 新增 warnings, 0 unused imports  
⚠️ **Lint**: 预存 4 errors 阻断 lint 任务（非本次引入）  

**状态**: AppDex 已从 NOT READY 推进到 **可 Build、可安装、可测试** 的 RC 状态。  
**下一步**: 在 Android Emulator 上安装 APK 执行真实用户流程测试（RC v1.2 Verification）。
