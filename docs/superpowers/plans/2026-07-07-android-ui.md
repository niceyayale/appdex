# appdex 子项目 #1b:Android UI 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现一个可运行的 Android APP,集成已完成的 `:core:apk`/`:core:axml`/`:core:dex` 能力,提供文件浏览、APK 查看、DEX 查看三个功能界面。能在沙箱编译验证 `assembleDebug`,真机 UI 由用户验证。

**Architecture:** `:app` 模块用 AGP + Compose,单 Activity + Navigation Compose。`:feature:files`/`:feature:apk`/`:feature:dex` 各为一个 Compose Screen + ViewModel。`:core:ui` 共享 M3 主题与组件。

**Tech Stack:** AGP 8.7,Kotlin 2.0.21,Compose BOM 2024.10,Material3,Navigation Compose,Activity Compose。

**环境前置(已就位):**
- JDK 17:`JAVA_HOME=/root/.local/share/mise/installs/java/17.0.2`
- Android SDK:`ANDROID_HOME=/opt/android-sdk`(android-34 + build-tools 34.0.0)
- 所有 gradle 命令:`JAVA_HOME=... ANDROID_HOME=... ./gradlew ...`
- 代理已配,git 提交后必须 push
- 工作目录 `/workspace`

**范围(MVP):**
- 单 Activity + Compose + 三个界面
- 文件浏览、APK 查看、DEX 查看
- **不包含**:编辑功能、smali 编辑、AXML 编辑器、十六进制编辑器(留给后续 plan)

---

## 文件结构

```
/workspace/
├── settings.gradle.kts                    (加 android/AGP 配置)
├── gradle/libs.versions.toml             (加 AGP/Compose 依赖)
├── build.gradle.kts                      (加 AGP plugin)
├── local.properties                      (新建:sdk.dir)
├── core/ui/
│   ├── build.gradle.kts                  (Android library)
│   └── src/main/kotlin/io/appdex/core/ui/
│       └── Theme.kt                      (M3 主题)
├── feature/files/
│   ├── build.gradle.kts
│   └── src/main/kotlin/io/appdex/feature/files/
│       └── FilesScreen.kt
├── feature/apk/
│   ├── build.gradle.kts
│   └── src/main/kotlin/io/appdex/feature/apk/
│       └── ApkScreen.kt
├── feature/dex/
│   ├── build.gradle.kts
│   └── src/main/kotlin/io/appdex/feature/dex/
│       └── DexScreen.kt
└── app/
    ├── build.gradle.kts                  (Android app)
    ├── src/main/AndroidManifest.xml
    └── src/main/kotlin/io/appdex/
        ├── MainActivity.kt
        └── AppNavigation.kt
```

---

## Task 1:Android Gradle Plugin 与 SDK 配置

**Files:**
- Modify: `/workspace/settings.gradle.kts`
- Modify: `/workspace/gradle/libs.versions.toml`
- Modify: `/workspace/build.gradle.kts`
- Create: `/workspace/local.properties`

- [ ] **Step 1: 在 settings.gradle.kts 加 AGP 配置**

完整替换 `/workspace/settings.gradle.kts`:

```kotlin
pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "appdex"

include(":core:io")
include(":core:apk")
include(":core:axml")
include(":core:arsc")
include(":core:dex")
include(":core:ui")
include(":feature:files")
include(":feature:apk")
include(":feature:dex")
include(":app")
```

- [ ] **Step 2: 在 libs.versions.toml 加 AGP/Compose 依赖**

在 `[versions]` 加:

```toml
agp = "8.7.3"
compose-bom = "2024.10.01"
activity-compose = "1.9.3"
navigation-compose = "2.8.4"
lifecycle-viewmodel-compose = "2.8.7"
```

在 `[libraries]` 加:

```toml
compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "compose-bom" }
compose-ui = { group = "androidx.compose.ui", name = "ui" }
compose-ui-graphics = { group = "androidx.compose.ui", name = "ui-graphics" }
compose-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
compose-material3 = { group = "androidx.compose.material3", name = "material3" }
compose-ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling" }
activity-compose = { group = "androidx.activity", name = "activity-compose", version.ref = "activity-compose" }
navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigation-compose" }
lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "lifecycle-viewmodel-compose" }
```

在 `[plugins]` 加:

```toml
android-application = { id = "com.android.application", version.ref = "agp" }
android-library = { id = "com.android.library", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
```

- [ ] **Step 3: 在根 build.gradle.kts 加 AGP plugin**

完整替换 `/workspace/build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
}

allprojects {
    apply(plugin = "io.gitlab.arturbosch.detekt")
    detekt {
        config.setFrom("$rootDir/config/detekt.yml")
        buildUponDefaultConfig = true
    }
}
```

- [ ] **Step 4: 写 local.properties**

`/workspace/local.properties`:

```properties
sdk.dir=/opt/android-sdk
```

- [ ] **Step 5: 把 local.properties 加到 .gitignore(避免提交用户本地 SDK 路径)**

在 `.gitignore` 加 `local.properties`

- [ ] **Step 6: 验证 gradle 能识别所有模块**

Run: `cd /workspace && JAVA_HOME=/root/.local/share/mise/installs/java/17.0.2 ANDROID_HOME=/opt/android-sdk ./gradlew projects`
Expected: 列出所有模块(含 :app :core:ui :feature:files 等),首次下载 AGP 耗时几分钟。

- [ ] **Step 7: 提交 + push**

```bash
git -C /workspace add settings.gradle.kts gradle/libs.versions.toml build.gradle.kts .gitignore
git -C /workspace commit -m "build: 接入 Android Gradle Plugin 与 Compose 依赖"
git -C /workspace push
```

---

## Task 2:`:core:ui` Material3 主题

**Files:**
- Create: `/workspace/core/ui/build.gradle.kts`
- Create: `/workspace/core/ui/src/main/AndroidManifest.xml`(空,占位)
- Create: `/workspace/core/ui/src/main/kotlin/io/appdex/core/ui/Theme.kt`

- [ ] **Step 1: 写 build.gradle.kts**

```kotlin
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "io.appdex.core.ui"
    compileSdk = 34

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.15"
    }
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)
}
```

- [ ] **Step 2: 写空 AndroidManifest.xml**

`/workspace/core/ui/src/main/AndroidManifest.xml`:

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android" />
```

- [ ] **Step 3: 写 Theme.kt**

```kotlin
package io.appdex.core.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColors = darkColorScheme(
    primary = Color(0xFF82B1FF),
)
private val LightColors = lightColorScheme(
    primary = Color(0xFF1976D2),
)

@Composable
fun AppdexTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val ctx = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}
```

- [ ] **Step 4: 验证编译**

Run: `cd /workspace && JAVA_HOME=/root/.local/share/mise/installs/java/17.0.2 ANDROID_HOME=/opt/android-sdk ./gradlew :core:ui:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: 提交 + push**

```bash
git -C /workspace add core/ui/
git -C /workspace commit -m "feat(core:ui): Material3 主题"
git -C /workspace push
```

---

## Task 3:`:feature:files` 文件浏览

**Files:**
- Create: `/workspace/feature/files/build.gradle.kts`
- Create: `/workspace/feature/files/src/main/AndroidManifest.xml`
- Create: `/workspace/feature/files/src/main/kotlin/io/appdex/feature/files/FilesScreen.kt`

- [ ] **Step 1: 写 build.gradle.kts**

```kotlin
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "io.appdex.feature.files"
    compileSdk = 34
    defaultConfig { minSdk = 26 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.15" }
}

dependencies {
    implementation(project(":core:io"))
    implementation(project(":core:ui"))
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    debugImplementation(libs.compose.ui.tooling)
}
```

- [ ] **Step 2: 写空 AndroidManifest.xml**

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android" />
```

- [ ] **Step 3: 写 FilesScreen**

```kotlin
package io.appdex.feature.files

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import io.appdex.core.io.FileSystem
import io.appdex.core.io.NioFileSystem
import java.io.File

data class FileItem(val name: String, val isDirectory: Boolean, val size: Long)

class FilesViewModel(
    private val startDir: File,
) : ViewModel() {
    private var currentDir: File = startDir
    private val _items = kotlinx.coroutines.flow.MutableStateFlow<List<FileItem>>(emptyList())
    val items: kotlinx.coroutines.flow.StateFlow<List<FileItem>> = _items

    init { refresh() }

    fun refresh() {
        _items.value = currentDir.listFiles()
            ?.sortedWith(compareBy({ !it.isDirectory }, { it.name }))
            ?.map { FileItem(it.name, it.isDirectory, it.length()) }
            ?: emptyList()
    }

    fun enter(name: String) {
        currentDir = File(currentDir, name)
        refresh()
    }

    fun goUp() {
        currentDir.parentFile?.let {
            currentDir = it
            refresh()
        }
    }

    fun currentPath(): String = currentDir.absolutePath
}

@Composable
fun FilesScreen(
    startDir: File,
    onBack: () -> Unit,
) {
    val vm: FilesViewModel = viewModel(factory = FilesVmFactory(startDir))
    val items by vm.items.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(vm.currentPath()) },
                navigationIcon = {
                    IconButton(onClick = { if (!vm.goUp()) onBack() else Unit }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            items(items) { item ->
                FileRow(item) { vm.enter(item.name) }
            }
        }
    }
}

@Composable
private fun FileRow(item: FileItem, onClick: () -> Unit) {
    androidx.compose.material3.ListItem(
        modifier = Modifier.padding(horizontal = 0.dp),
        headlineContent = { Text(item.name) },
        leadingContent = {
            Icon(
                if (item.isDirectory) Icons.Default.Folder else Icons.Default.InsertDriveFile,
                contentDescription = null,
            )
        },
        trailingContent = {
            if (!item.isDirectory) Text("${item.size} B")
        },
    )
    androidx.compose.foundation.clickable(onClick = onClick).let { }
}

import androidx.lifecycle.ViewModelProvider

class FilesVmFactory(private val startDir: File) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return FilesViewModel(startDir) as T
    }
}
```

注:`ListItem` 的点击需要在 modifier 上加,上面 FileRow 的 clickable 写法不对,Step 4 修正。

- [ ] **Step 4: 修正 FileRow 的点击**

把 `FileRow` 改为:

```kotlin
@Composable
private fun FileRow(item: FileItem, onClick: () -> Unit) {
    androidx.compose.material3.ListItem(
        modifier = Modifier.androidx.compose.foundation.clickable { onClick() },
        headlineContent = { Text(item.name) },
        leadingContent = {
            Icon(
                if (item.isDirectory) Icons.Default.Folder else Icons.Default.InsertDriveFile,
                contentDescription = null,
            )
        },
        trailingContent = {
            if (!item.isDirectory) Text("${item.size} B")
        },
    )
}
```

注:实际写法:`Modifier.clickable { onClick() }`,需要 import `androidx.compose.foundation.clickable`。Step 3+4 合并:写 FilesScreen 时直接用正确写法。

- [ ] **Step 5: 验证编译**

Run: `cd /workspace && JAVA_HOME=/root/.local/share/mise/installs/java/17.0.2 ANDROID_HOME=/opt/android-sdk ./gradlew :feature:files:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: 提交 + push**

```bash
git -C /workspace add feature/files/
git -C /workspace commit -m "feat(feature:files): 文件浏览界面"
git -C /workspace push
```

---

## Task 4:`:feature:apk` APK 查看

**Files:**
- Create: `/workspace/feature/apk/build.gradle.kts`
- Create: `/workspace/feature/apk/src/main/AndroidManifest.xml`
- Create: `/workspace/feature/apk/src/main/kotlin/io/appdex/feature/apk/ApkScreen.kt`

- [ ] **Step 1: 写 build.gradle.kts**

```kotlin
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "io.appdex.feature.apk"
    compileSdk = 34
    defaultConfig { minSdk = 26 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.15" }
}

dependencies {
    implementation(project(":core:apk"))
    implementation(project(":core:axml"))
    implementation(project(":core:io"))
    implementation(project(":core:ui"))
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.lifecycle.viewmodel.compose)
    debugImplementation(libs.compose.ui.tooling)
}
```

- [ ] **Step 2: 写空 AndroidManifest.xml**

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android" />
```

- [ ] **Step 3: 写 ApkScreen**

```kotlin
package io.appdex.feature.apk

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import io.appdex.core.apk.ApkEntry
import io.appdex.core.apk.BinaryApkReader
import io.appdex.core.axml.BinaryAxmlReader
import io.appdex.core.io.NioSeekableChannel
import java.io.File

class ApkViewModel : ViewModel() {
    private val _entries = kotlinx.coroutines.flow.MutableStateFlow<List<ApkEntry>>(emptyList())
    val entries: kotlinx.coroutines.flow.StateFlow<List<ApkEntry>> = _entries
    private val _manifest = kotlinx.coroutines.flow.MutableStateFlow("")
    val manifest: kotlinx.coroutines.flow.StateFlow<String> = _manifest

    fun loadApk(file: File) {
        val channel = NioSeekableChannel(java.nio.channels.FileChannel.open(file.toPath()))
        val reader = BinaryApkReader()
        _entries.value = reader.listEntries(channel)
        // 找 AndroidManifest.xml 并解析
        val manifestEntry = _entries.value.find { it.name == "AndroidManifest.xml" }
        if (manifestEntry != null) {
            val ch2 = NioSeekableChannel(java.nio.channels.FileChannel.open(file.toPath()))
            val bytes = reader.readEntry(ch2, manifestEntry)
            _manifest.value = BinaryAxmlReader().read(bytes).xml
        }
        channel.close()
    }
}

@Composable
fun ApkScreen(apkFile: File?, onBack: () -> Unit) {
    val vm: ApkViewModel = viewModel()
    val entries by vm.entries.collectAsState()
    val manifest by vm.manifest.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("APK 查看") }) }
    ) { padding ->
        if (apkFile == null) {
            Column(modifier = Modifier.padding(padding).padding(16.dp)) {
                Text("未选择 APK 文件")
            }
        } else {
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                Text("文件: ${apkFile.name}", modifier = Modifier.padding(8.dp))
                Text("条目数: ${entries.size}", modifier = Modifier.padding(8.dp))
                if (manifest.isNotEmpty()) {
                    Text("AndroidManifest:", modifier = Modifier.padding(8.dp))
                    Text(
                        manifest,
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(8.dp),
                    )
                }
            }
        }
    }
}

import androidx.compose.ui.unit.dp
```

注:NioSeekableChannel 的构造需要确认 —— Task 1a 实现的是 `NioFileChannel`(不是 NioSeekableChannel)。Step 4 修正:用 `NioFileChannel.open(path, Mode.Read)` 或直接构造。

- [ ] **Step 4: 修正 channel 构造**

实际用 `io.appdex.core.io.NioFileChannel.open(path, Mode.Read)` 打开 channel:

```kotlin
import io.appdex.core.io.Mode
import io.appdex.core.io.NioFileChannel
import java.nio.file.Paths

fun loadApk(file: File) {
    val channel = NioFileChannel.open(file.toPath(), Mode.Read)
    // ...
}
```

Step 3+4 合并:写 ApkScreen 时直接用正确的 NioFileChannel.open API。

- [ ] **Step 5: 验证编译**

Run: `cd /workspace && JAVA_HOME=/root/.local/share/mise/installs/java/17.0.2 ANDROID_HOME=/opt/android-sdk ./gradlew :feature:apk:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: 提交 + push**

```bash
git -C /workspace add feature/apk/
git -C /workspace commit -m "feat(feature:apk): APK 查看界面"
git -C /workspace push
```

---

## Task 5:`:feature:dex` DEX 查看

**Files:**
- Create: `/workspace/feature/dex/build.gradle.kts`
- Create: `/workspace/feature/dex/src/main/AndroidManifest.xml`
- Create: `/workspace/feature/dex/src/main/kotlin/io/appdex/feature/dex/DexScreen.kt`

- [ ] **Step 1: 写 build.gradle.kts**

同 feature:apk,但 `implementation(project(":core:dex"))` 替换 `:core:apk`/`:core:axml`。

- [ ] **Step 2: 写空 AndroidManifest.xml**

- [ ] **Step 3: 写 DexScreen**

```kotlin
package io.appdex.feature.dex

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import io.appdex.core.dex.BinaryDexReader
import io.appdex.core.dex.DexClass
import java.io.File

class DexViewModel : ViewModel() {
    private val _classes = kotlinx.coroutines.flow.MutableStateFlow<List<DexClass>>(emptyList())
    val classes: kotlinx.coroutines.flow.StateFlow<List<DexClass>> = _classes
    private val _smali = kotlinx.coroutines.flow.MutableStateFlow("")
    val smali: kotlinx.coroutines.flow.StateFlow<String> = _smali

    fun loadDex(file: File) {
        val bytes = file.readBytes()
        val reader = BinaryDexReader()
        _classes.value = reader.listClasses(bytes)
    }

    fun showSmali(file: File, classType: String) {
        val bytes = file.readBytes()
        _smali.value = BinaryDexReader().toSmali(bytes, classType)
    }
}

@Composable
fun DexScreen(dexFile: File?, onBack: () -> Unit) {
    val vm: DexViewModel = viewModel()
    val classes by vm.classes.collectAsState()
    val smali by vm.smali.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("DEX 查看") }) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (dexFile == null) {
                Text("未选择 DEX 文件", modifier = Modifier.padding(16.dp))
            } else {
                Text("类数: ${classes.size}", modifier = Modifier.padding(8.dp))
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(classes) { cls ->
                        ListItem(
                            headlineContent = { Text(cls.name) },
                            supportingContent = { Text("${cls.methods.size} 方法 / ${cls.fields.size} 字段") },
                        )
                    }
                }
                if (smali.isNotEmpty()) {
                    Text(
                        smali,
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(8.dp),
                    )
                }
            }
        }
    }
}
```

- [ ] **Step 4: 验证编译**

Run: `cd /workspace && JAVA_HOME=/root/.local/share/mise/installs/java/17.0.2 ANDROID_HOME=/opt/android-sdk ./gradlew :feature:dex:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: 提交 + push**

```bash
git -C /workspace add feature/dex/
git -C /workspace commit -m "feat(feature:dex): DEX 查看界面"
git -C /workspace push
```

---

## Task 6:`:app` 入口与导航

**Files:**
- Create: `/workspace/app/build.gradle.kts`
- Create: `/workspace/app/src/main/AndroidManifest.xml`
- Create: `/workspace/app/src/main/kotlin/io/appdex/MainActivity.kt`
- Create: `/workspace/app/src/main/kotlin/io/appdex/AppNavigation.kt`
- Create: `/workspace/app/src/main/res/values/strings.xml`
- Create: `/workspace/app/src/main/res/values/themes.xml`

- [ ] **Step 1: 写 build.gradle.kts**

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "io.appdex"
    compileSdk = 34

    defaultConfig {
        applicationId = "io.appdex"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.15" }
}

dependencies {
    implementation(project(":core:io"))
    implementation(project(":core:apk"))
    implementation(project(":core:axml"))
    implementation(project(":core:arsc"))
    implementation(project(":core:dex"))
    implementation(project(":core:ui"))
    implementation(project(":feature:files"))
    implementation(project(":feature:apk"))
    implementation(project(":feature:dex"))

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.activity.compose)
    implementation(libs.navigation.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    debugImplementation(libs.compose.ui.tooling)
}
```

- [ ] **Step 2: 写 AndroidManifest.xml**

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"
        android:maxSdkVersion="32" />
    <uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
    <uses-permission android:name="android.permission.READ_MEDIA_VIDEO" />
    <uses-permission android:name="android.permission.READ_MEDIA_AUDIO" />

    <application
        android:label="appdex"
        android:theme="@style/Theme.Appdex"
        android:supportsRtl="true">

        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

- [ ] **Step 3: 写 MainActivity**

```kotlin
package io.appdex

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.appdex.core.ui.AppdexTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppdexTheme {
                AppNavigation()
            }
        }
    }
}
```

- [ ] **Step 4: 写 AppNavigation**

```kotlin
package io.appdex

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.appdex.feature.apk.ApkScreen
import io.appdex.feature.dex.DexScreen
import io.appdex.feature.files.FilesScreen
import java.io.File

object Routes {
    const val HOME = "home"
    const val FILES = "files"
    const val APK = "apk"
    const val DEX = "dex"
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    var selectedFile by remember { mutableStateOf<File?>(null) }

    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                onFiles = { navController.navigate(Routes.FILES) },
                onApk = { navController.navigate(Routes.APK) },
                onDex = { navController.navigate(Routes.DEX) },
            )
        }
        composable(Routes.FILES) {
            FilesScreen(
                startDir = File("/sdcard/"),
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.APK) {
            ApkScreen(
                apkFile = selectedFile,
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.DEX) {
            DexScreen(
                dexFile = selectedFile,
                onBack = { navController.popBackStack() },
            )
        }
    }
}

@Composable
private fun HomeScreen(
    onFiles: () -> Unit,
    onApk: () -> Unit,
    onDex: () -> Unit,
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("appdex") }) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Button(onClick = onFiles) { Text("文件浏览") }
            Button(onClick = onApk) { Text("APK 查看") }
            Button(onClick = onDex) { Text("DEX 查看") }
        }
    }
}
```

- [ ] **Step 5: 写 strings.xml**

`/workspace/app/src/main/res/values/strings.xml`:

```xml
<resources>
    <string name="app_name">appdex</string>
</resources>
```

- [ ] **Step 6: 写 themes.xml**

`/workspace/app/src/main/res/values/themes.xml`:

```xml
<resources>
    <style name="Theme.Appdex" parent="android:Theme.Material.Light.NoActionBar" />
</resources>
```

- [ ] **Step 7: 验证完整 APP 编译**

Run: `cd /workspace && JAVA_HOME=/root/.local/share/mise/installs/java/17.0.2 ANDROID_HOME=/opt/android-sdk ./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL,生成 `app/build/outputs/apk/debug/app-debug.apk`

如果编译失败,根据错误修:
- 缺 import → 补
- API 不存在 → 查 Compose 版本对应 API
- 命名冲突 → 调整

- [ ] **Step 8: 提交 + push**

```bash
git -C /workspace add app/
git -C /workspace commit -m "feat(app): Android APP 入口与导航"
git -C /workspace push
```

---

## Task 7:detekt + README 最终更新

- [ ] **Step 1: 跑全量 build + test + detekt**

Run:
```bash
cd /workspace && JAVA_HOME=/root/.local/share/mise/installs/java/17.0.2 ANDROID_HOME=/opt/android-sdk ./gradlew assembleDebug detekt
```
Expected: 全绿

- [ ] **Step 2: detekt 修复(最小)**

Android 模块加 `@Suppress` 或 detekt.yml 配置。不改业务逻辑。

- [ ] **Step 3: 更新 README**

完整状态:
```markdown
## 状态

- 子项目 #1a: `:core:io` KMP 文件系统抽象 —— ✅ 完成
- 子项目 #2: `:core:apk` / `:core:axml` / `:core:arsc` 只读解析 —— ✅ 完成(MVP)
- 子项目 #4: `:core:dex` DEX 只读解析(smali 反汇编) —— ✅ 完成(MVP)
- 子项目 #1b: Android UI —— ✅ 完成(MVP)

## 构建 APK

```bash
./gradlew :app:assembleDebug
# 输出:app/build/outputs/apk/debug/app-debug.apk
```
```

- [ ] **Step 4: 提交 + push**

```bash
git -C /workspace add README.md config/detekt.yml
git -C /workspace commit -m "quality: Plan 1b 完成 + README 最终更新"
git -C /workspace push
```

---

## 完成标准

- [ ] `:app:assembleDebug` 成功生成 APK
- [ ] 所有 core 模块测试仍通过
- [ ] detekt 无 error
- [ ] APP 能在 Android 8.0+ 真机运行(用户验证)
- [ ] 三个界面可访问:文件浏览、APK 查看、DEX 查看

## 后续

- 用户装机测试,反馈 bug
- 编辑功能(AXML/ARSC/DEX 编辑)
- 十六进制编辑器、文本编辑器
- APK 重打包与签名
