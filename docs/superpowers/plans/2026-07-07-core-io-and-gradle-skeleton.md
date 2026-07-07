# appdex 子项目 #1a:`:core:io` 与 Gradle 骨架 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 搭好 KMP 多模块 Gradle 骨架,完成 `:core:io` 模块(commonMain 接口 + jvmMain 实现 + InMemory 测试实现),全部可在 Linux JVM 沙箱中跑单测验证。

**Architecture:** 根项目 Gradle Kotlin DSL + version catalog + Kotlin Multiplatform 插件;`:core:io` 用 KMP,commonMain 定义 `SeekableChannel` / `FileSystem` 接口与 `InMemoryFileSystem` 测试实现,jvmMain 用 `java.nio.channels.FileChannel` 适配真机文件。所有公共 API 通过 interface 暴露,实现类 `internal`。

**Tech Stack:** Kotlin 2.0.21,KMP,Gradle 8.14.4,Kotlin DSL,JUnit 5,version catalog。

**环境前置(已确认):**
- JDK 25 已装
- Gradle 8.14.4 已装(系统级,路径 `/root/.local/share/mise/installs/gradle/8.14.4/`)
- Android SDK **未装**,本计划不涉及 Android target
- 沙箱可联网,Gradle 能下载依赖

**范围(不包含):**
- `:core:ui` / `:feature:files` / `:app` 的 Android 代码(留给 Plan 1b)
- SAF 适配(留给 Plan 1b,需要 Android SDK)
- 任何 UI 渲染验证

---

## 文件结构总览

完成后工作区结构:

```
/workspace/
├── README.md                                    (修改:补充构建说明)
├── settings.gradle.kts                          (新建:多模块声明)
├── build.gradle.kts                             (新建:根项目配置)
├── gradle.properties                            (新建:JVM/Kotlin 选项)
├── gradle/
│   └── libs.versions.toml                       (新建:version catalog)
├── gradle/wrapper/
│   ├── gradle-wrapper.jar                       (新建:wrapper)
│   └── gradle-wrapper.properties                (新建:wrapper)
├── gradlew                                      (新建:wrapper 脚本)
├── gradlew.bat                                  (新建:wrapper 脚本)
├── .gitignore                                   (新建)
├── config/
│   └── detekt.yml                               (新建:detekt 配置)
└── core/
    └── io/
        ├── build.gradle.kts                     (新建:KMP 模块配置)
        └── src/
            ├── commonMain/kotlin/io/appdex/core/io/
            │   ├── SeekableChannel.kt           (接口)
            │   ├── FileSystem.kt                (接口 + Entry/Mode/异常)
            │   ├── InMemoryFileSystem.kt       (commonMain 测试实现)
            │   └── InMemorySeekableChannel.kt  (commonMain 测试实现)
            ├── commonTest/kotlin/io/appdex/core/io/
            │   ├── SeekableChannelTest.kt
            │   ├── InMemoryFileSystemTest.kt
            │   └── FileSystemContractTest.kt    (契约测试,所有实现都要过)
            ├── jvmMain/kotlin/io/appdex/core/io/
            │   └── NioFileChannel.kt            (jvmMain 适配 java.nio)
            └── jvmTest/kotlin/io/appdex/core/io/
                └── NioFileSystemTest.kt         (复用契约测试 + 真实临时文件)
```

每个文件单一职责:
- `SeekableChannel.kt` —— 字节读写契约(无实现)
- `FileSystem.kt` —— 文件系统契约 + 数据类(`Entry`/`Mode`) + 异常体系
- `InMemorySeekableChannel.kt` —— 内存实现(用于测试与未来 zip 流)
- `InMemoryFileSystem.kt` —— 内存 FS 实现
- `NioFileChannel.kt` —— JVM 平台真机文件实现

---

## Task 1:Gradle 多模块骨架

**Files:**
- Create: `/workspace/settings.gradle.kts`
- Create: `/workspace/build.gradle.kts`
- Create: `/workspace/gradle.properties`
- Create: `/workspace/gradle/libs.versions.toml`
- Create: `/workspace/.gitignore`

- [ ] **Step 1: 写 settings.gradle.kts**

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
```

- [ ] **Step 2: 写 gradle/libs.versions.toml**

```toml
[versions]
kotlin = "2.0.21"
junit-jupiter = "5.10.2"

[libraries]
junit-jupiter-api = { group = "org.junit.jupiter", name = "junit-jupiter-api", version.ref = "junit-jupiter" }
junit-jupiter-params = { group = "org.junit.jupiter", name = "junit-jupiter-params", version.ref = "junit-jupiter" }
junit-jupiter-engine = { group = "org.junit.jupiter", name = "junit-jupiter-engine", version.ref = "junit-jupiter" }

[plugins]
kotlin-multiplatform = { id = "org.jetbrains.kotlin.multiplatform", version.ref = "kotlin" }
```

- [ ] **Step 3: 写根 build.gradle.kts**

```kotlin
plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
}
```

- [ ] **Step 4: 写 gradle.properties**

```properties
org.gradle.jvmargs=-Xmx2g -Dfile.encoding=UTF-8
org.gradle.caching=true
org.gradle.configuration-cache=false
kotlin.code.style=official
```

- [ ] **Step 5: 写 .gitignore**

```gitignore
# Gradle
.gradle/
build/
!gradle/wrapper/gradle-wrapper.jar

# IDE
.idea/
*.iml
.vscode/

# OS
.DS_Store
Thumbs.db

# Kotlin
*.hprof
```

- [ ] **Step 6: 验证 Gradle 能识别项目结构**

Run: `cd /workspace && gradle projects`
Expected: 列出 root project `appdex` 和子项目 `:core:io`,无下载失败。第一次会下载 Kotlin 插件,耗时几分钟。

- [ ] **Step 7: 提交**

```bash
git -C /workspace add settings.gradle.kts build.gradle.kts gradle.properties gradle/libs.versions.toml .gitignore
git -C /workspace commit -m "build: 初始化 Gradle 多模块骨架与 version catalog"
```

---

## Task 2:Gradle Wrapper

**Files:**
- Create: `/workspace/gradle/wrapper/gradle-wrapper.properties`
- Create: `/workspace/gradle/wrapper/gradle-wrapper.jar`
- Create: `/workspace/gradlew`
- Create: `/workspace/gradlew.bat`

- [ ] **Step 1: 生成 wrapper**

Run: `cd /workspace && gradle wrapper --gradle-version 8.14.4 --distribution-type bin`
Expected: 生成上述 4 个文件。

- [ ] **Step 2: 验证 wrapper 可用**

Run: `cd /workspace && ./gradlew --version`
Expected: 输出 Gradle 8.14.4 版本信息。

- [ ] **Step 3: 提交**

```bash
git -C /workspace add gradle/wrapper/ gradlew gradlew.bat
git -C /workspace commit -m "build: 添加 Gradle Wrapper 8.14.4"
```

---

## Task 3:`:core:io` 模块骨架(空,但能 build)

**Files:**
- Create: `/workspace/core/io/build.gradle.kts`
- Create: `/workspace/core/io/src/commonMain/kotlin/io/appdex/core/io/.gitkeep`
- Create: `/workspace/core/io/src/commonTest/kotlin/io/appdex/core/io/.gitkeep`
- Create: `/workspace/core/io/src/jvmMain/kotlin/io/appdex/core/io/.gitkeep`
- Create: `/workspace/core/io/src/jvmTest/kotlin/io/appdex/core/io/.gitkeep`

- [ ] **Step 1: 写 core/io/build.gradle.kts**

```kotlin
plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

kotlin {
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "17"
        }
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
    }

    sourceSets {
        commonMain { dependencies { } }
        commonTest {
            dependencies {
                implementation(libs.junit.jupiter.api)
                implementation(libs.junit.jupiter.params)
                runtimeOnly(libs.junit.jupiter.engine)
            }
        }
        jvmMain { dependencies { } }
        jvmTest { dependencies { } }
    }
}
```

注:JUnit 5 在 KMP commonTest 中以 jvm-available 形式声明,commonTest 实际只在 jvm target 跑。

- [ ] **Step 2: 创建空目录占位**

为每个 source set 创建 `.gitkeep`,确保 Gradle 识别源集。

- [ ] **Step 3: 验证空模块可 build**

Run: `cd /workspace && ./gradlew :core:io:build`
Expected: BUILD SUCCESSFUL,无源文件也通过。

- [ ] **Step 4: 提交**

```bash
git -C /workspace add core/io/
git -C /workspace commit -m "build(core:io): 创建 KMP 模块骨架"
```

---

## Task 4:TDD — `SeekableChannel` 接口与最小测试

**Files:**
- Create: `/workspace/core/io/src/commonMain/kotlin/io/appdex/core/io/SeekableChannel.kt`
- Create: `/workspace/core/io/src/commonTest/kotlin/io/appdex/core/io/SeekableChannelContractTest.kt`

- [ ] **Step 1: 写失败测试(契约:可读可写、position 可寻址、size 正确)**

```kotlin
package io.appdex.core.io

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * SeekableChannel 契约测试。所有 SeekableChannel 实现都应通过此测试。
 * 子类通过抽象方法 channelWith(bytes) 提供实现。
 */
abstract class SeekableChannelContractTest {

    /** 用给定初始内容构造一个可读写的 SeekableChannel。 */
    protected abstract fun channelWith(initialBytes: ByteArray): SeekableChannel

    @Test
    fun `size returns initial content length`() {
        val ch = channelWith(byteArrayOf(1, 2, 3, 4))
        assertEquals(4L, ch.size)
        ch.close()
    }

    @Test
    fun `read returns bytes from start`() {
        val ch = channelWith(byteArrayOf(10, 20, 30))
        val buf = ByteArray(3)
        val n = ch.read(buf, 3)
        assertEquals(3, n)
        assertArrayEquals(byteArrayOf(10, 20, 30), buf)
        ch.close()
    }

    @Test
    fun `position moves read cursor`() {
        val ch = channelWith(byteArrayOf(1, 2, 3, 4, 5))
        ch.position(2L)
        val buf = ByteArray(2)
        val n = ch.read(buf, 2)
        assertEquals(2, n)
        assertArrayEquals(byteArrayOf(3, 4), buf)
        ch.close()
    }

    @Test
    fun `write appends and size grows`() {
        val ch = channelWith(byteArrayOf(1, 2))
        ch.position(ch.size)
        ch.write(byteArrayOf(3, 4), 0, 2)
        assertEquals(4L, ch.size)
        ch.position(0L)
        val buf = ByteArray(4)
        ch.read(buf, 4)
        assertArrayEquals(byteArrayOf(1, 2, 3, 4), buf)
        ch.close()
    }

    @Test
    fun `position beyond size throws`() {
        val ch = channelWith(byteArrayOf(1))
        assertThrows<IllegalArgumentException> { ch.position(5L) }
        ch.close()
    }
}
```

- [ ] **Step 2: 跑测试,确认编译失败(接口未定义)**

Run: `cd /workspace && ./gradlew :core:io:jvmTest`
Expected: 编译失败,`SeekableChannel` 未解析。

- [ ] **Step 3: 写 SeekableChannel 接口**

```kotlin
package io.appdex.core.io

/**
 * 可随机寻址的字节通道。读 / 写共享一个游标。
 *
 * 所有方法非线程安全,调用方自行同步。
 */
interface SeekableChannel : Closeable {

    /** 内容总字节数。 */
    val size: Long

    /**
     * 将游标移到 [newPos]。负数或超出 [size] 抛 [IllegalArgumentException]。
     * 写入后调用方应自行决定是否重新读 size。
     */
    fun position(newPos: Long)

    /**
     * 从当前游标读取最多 [len] 字节到 [buf]。
     * @return 实际读取的字节数;到达末尾返回 0(EOF)。
     */
    fun read(buf: ByteArray, len: Int): Int

    /**
     * 从 [buf] 的 [off] 偏移写入 [len] 字节,游标后移。
     * 写入位置超过当前 size 时,文件按需扩展。
     */
    fun write(buf: ByteArray, off: Int, len: Int)
}
```

- [ ] **Step 4: 跑测试,确认仍失败(没有实现类,只有接口)**

Run: `cd /workspace && ./gradlew :core:io:jvmTest`
Expected: 测试不报编译错,但因为没有具体子类(下个 Task 才写),所有测试方法因抽象而"未跑"。实际此步会 PASS 0 tests,因为抽象类没有可执行用例 —— 这是正常的,继续下一步。

- [ ] **Step 5: 提交**

```bash
git -C /workspace add core/io/src/commonMain/kotlin/io/appdex/core/io/SeekableChannel.kt core/io/src/commonTest/kotlin/io/appdex/core/io/SeekableChannelContractTest.kt
git -C /workspace commit -m "feat(core:io): 定义 SeekableChannel 接口与契约测试"
```

---

## Task 5:TDD — `InMemorySeekableChannel` 实现

**Files:**
- Create: `/workspace/core/io/src/commonMain/kotlin/io/appdex/core/io/InMemorySeekableChannel.kt`
- Create: `/workspace/core/io/src/commonTest/kotlin/io/appdex/core/io/InMemorySeekableChannelTest.kt`

- [ ] **Step 1: 写失败测试(实现契约测试基类)**

```kotlin
package io.appdex.core.io

class InMemorySeekableChannelTest : SeekableChannelContractTest() {
    override fun channelWith(initialBytes: ByteArray): SeekableChannel =
        InMemorySeekableChannel(initialBytes)
}
```

- [ ] **Step 2: 跑测试,确认失败(InMemorySeekableChannel 不存在)**

Run: `cd /workspace && ./gradlew :core:io:jvmTest`
Expected: 编译失败,`InMemorySeekableChannel` 未解析。

- [ ] **Step 3: 写 InMemorySeekableChannel 实现**

```kotlin
package io.appdex.core.io

/**
 * 内存中的 [SeekableChannel] 实现。用 [ByteArray] 存储,可增长。
 *
 * 主要用途:测试、APK/DEX 在内存中编辑。不适合大文件(> 数 MB),
 * 大文件应使用平台对应的实现(jvmMain 用 FileChannel)。
 */
class InMemorySeekableChannel(
    initialContent: ByteArray = ByteArray(0)
) : SeekableChannel {

    private var content: ByteArray = initialContent.copyOf()
    private var cursor: Long = 0L
    private var closed: Boolean = false

    override val size: Long
        get() = checkOpen().size.toLong()

    override fun position(newPos: Long) {
        checkOpen()
        require(newPos >= 0L) { "position cannot be negative: $newPos" }
        require(newPos <= content.size.toLong()) {
            "position beyond size: $newPos > ${content.size}"
        }
        cursor = newPos
    }

    override fun read(buf: ByteArray, len: Int): Int {
        checkOpen()
        require(len >= 0) { "len cannot be negative: $len" }
        require(len <= buf.size) { "len $len exceeds buffer size ${buf.size}" }
        if (cursor >= content.size) return 0
        val toRead = minOf(len, (content.size - cursor).toInt())
        System.arraycopy(content, cursor.toInt(), buf, 0, toRead)
        cursor += toRead
        return toRead
    }

    override fun write(buf: ByteArray, off: Int, len: Int) {
        checkOpen()
        require(off >= 0 && len >= 0 && off + len <= buf.size) {
            "invalid off=$off len=$len for buffer size ${buf.size}"
        }
        val writeEnd = cursor + len
        if (writeEnd > content.size) {
            val grown = ByteArray(writeEnd.toInt())
            System.arraycopy(content, 0, grown, 0, content.size)
            content = grown
        }
        System.arraycopy(buf, off, content, cursor.toInt(), len)
        cursor = writeEnd
    }

    /** 返回当前所有内容的快照(测试用)。 */
    fun toByteArray(): ByteArray = checkOpen().copyOf()

    override fun close() {
        closed = true
    }

    private fun checkOpen(): ByteArray {
        check(!closed) { "channel is closed" }
        return content
    }
}
```

- [ ] **Step 4: 跑测试,确认通过**

Run: `cd /workspace && ./gradlew :core:io:jvmTest`
Expected: BUILD SUCCESSFUL,所有 SeekableChannelContractTest 用例通过(InMemorySeekableChannelTest 继承得到 5 个用例)。

- [ ] **Step 5: 提交**

```bash
git -C /workspace add core/io/src/commonMain/kotlin/io/appdex/core/io/InMemorySeekableChannel.kt core/io/src/commonTest/kotlin/io/appdex/core/io/InMemorySeekableChannelTest.kt
git -C /workspace commit -m "feat(core:io): InMemorySeekableChannel 实现 + 契约测试通过"
```

---

## Task 6:TDD — `FileSystem` 接口与数据模型

**Files:**
- Create: `/workspace/core/io/src/commonMain/kotlin/io/appdex/core/io/FileSystem.kt`

- [ ] **Step 1: 写 FileSystem.kt(接口 + 数据类 + 异常,一次定义清楚)**

```kotlin
package io.appdex.core.io

/**
 * 文件系统抽象。所有路径使用 `/` 分隔符(POSIX 风格),根为 `/`。
 * 路径不允许 `..`(调用方应先规范化)。
 */
interface FileSystem {

    /** 列出 [dir] 下的条目。dir 必须存在且为目录。 */
    fun list(dir: String): List<Entry>

    /** 打开文件,按 [mode] 读写。文件不存在时,Write/Append 模式会创建。 */
    fun open(path: String, mode: Mode): SeekableChannel

    /** 返回条目元信息;不存在返回 null。 */
    fun stat(path: String): Entry?

    /** 创建目录(非递归)。父目录必须存在。 */
    fun mkdir(path: String)

    /** 删除文件或空目录。不存在抛 [NoSuchFileException]。 */
    fun delete(path: String)

    /** 重命名/移动。源不存在抛 [NoSuchFileException]。 */
    fun rename(from: String, to: String)
}

/** 文件打开模式。 */
enum class Mode {
    Read,
    Write,
    ReadWrite,
    Append,
}

/** 文件类型。 */
enum class EntryType {
    File,
    Directory,
}

/** 文件条目元信息。 */
data class Entry(
    val path: String,
    val name: String,
    val type: EntryType,
    val size: Long,
    val lastModified: Long,
)

/** 路径不存在。 */
class NoSuchFileException(path: String) : RuntimeException("no such file: $path")

/** 路径已存在,但操作要求它不存在。 */
class FileAlreadyExistsException(path: String) : RuntimeException("already exists: $path")

/** 路径是目录,但操作要求文件;反之亦然。 */
class NotFileException(path: String) : RuntimeException("not a file: $path")
class NotDirectoryException(path: String) : RuntimeException("not a directory: $path")
```

- [ ] **Step 2: 跑 build,确认编译通过(此步无测试,只为验证接口定义无误)**

Run: `cd /workspace && ./gradlew :core:io:build`
Expected: BUILD SUCCESSFUL。

- [ ] **Step 3: 提交**

```bash
git -C /workspace add core/io/src/commonMain/kotlin/io/appdex/core/io/FileSystem.kt
git -C /workspace commit -m "feat(core:io): FileSystem 接口 + Entry/Mode/异常模型"
```

---

## Task 7:TDD — `InMemoryFileSystem` 实现

**Files:**
- Create: `/workspace/core/io/src/commonMain/kotlin/io/appdex/core/io/InMemoryFileSystem.kt`
- Create: `/workspace/core/io/src/commonTest/kotlin/io/appdex/core/io/FileSystemContractTest.kt`
- Create: `/workspace/core/io/src/commonTest/kotlin/io/appdex/core/io/InMemoryFileSystemTest.kt`

- [ ] **Step 1: 写 FileSystemContractTest(所有 FileSystem 实现都要过的契约)**

```kotlin
package io.appdex.core.io

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * FileSystem 契约测试。所有 FileSystem 实现都应通过。
 * 子类通过 fs() 提供一个空的新实例。
 */
abstract class FileSystemContractTest {

    protected abstract fun fs(): FileSystem

    @Test
    fun `stat on missing returns null`() {
        val fs = fs()
        assertNull(fs.stat("/missing"))
    }

    @Test
    fun `mkdir creates directory entry`() {
        val fs = fs()
        fs.mkdir("/dir")
        val e = fs.stat("/dir")
        assertNotNull(e)
        assertEquals(EntryType.Directory, e!!.type)
        assertEquals("dir", e.name)
    }

    @Test
    fun `mkdir on existing throws`() {
        val fs = fs()
        fs.mkdir("/dir")
        assertThrows<FileAlreadyExistsException> { fs.mkdir("/dir") }
    }

    @Test
    fun `mkdir without parent throws`() {
        val fs = fs()
        assertThrows<NoSuchFileException> { fs.mkdir("/no/parent") }
    }

    @Test
    fun `open write creates file`() {
        val fs = fs()
        val ch = fs.open("/file", Mode.Write)
        ch.write(byteArrayOf(1, 2, 3), 0, 3)
        ch.close()
        val e = fs.stat("/file")!!
        assertEquals(EntryType.File, e.type)
        assertEquals(3L, e.size)
    }

    @Test
    fun `open read returns content`() {
        val fs = fs()
        fs.open("/file", Mode.Write).use { it.write(byteArrayOf(10, 20), 0, 2) }
        val ch = fs.open("/file", Mode.Read)
        val buf = ByteArray(2)
        val n = ch.read(buf, 2)
        assertEquals(2, n)
        assertArrayEquals(byteArrayOf(10, 20), buf)
        ch.close()
    }

    @Test
    fun `append mode keeps existing`() {
        val fs = fs()
        fs.open("/file", Mode.Write).use { it.write(byteArrayOf(1, 2), 0, 2) }
        fs.open("/file", Mode.Append).use { it.write(byteArrayOf(3, 4), 0, 2) }
        val ch = fs.open("/file", Mode.Read)
        val buf = ByteArray(4)
        assertEquals(4, ch.read(buf, 4))
        assertArrayEquals(byteArrayOf(1, 2, 3, 4), buf)
        ch.close()
    }

    @Test
    fun `list lists directory children`() {
        val fs = fs()
        fs.mkdir("/d")
        fs.open("/d/a", Mode.Write).use { it.write(byteArrayOf(1), 0, 1) }
        fs.open("/d/b", Mode.Write).use { it.write(byteArrayOf(2), 0, 1) }
        val entries = fs.list("/d").sortedBy { it.name }
        assertEquals(2, entries.size)
        assertEquals("a", entries[0].name)
        assertEquals("b", entries[1].name)
    }

    @Test
    fun `delete removes file`() {
        val fs = fs()
        fs.open("/f", Mode.Write).use { it.write(byteArrayOf(1), 0, 1) }
        fs.delete("/f")
        assertNull(fs.stat("/f"))
    }

    @Test
    fun `delete on missing throws`() {
        val fs = fs()
        assertThrows<NoSuchFileException> { fs.delete("/missing") }
    }

    @Test
    fun `rename moves file`() {
        val fs = fs()
        fs.open("/a", Mode.Write).use { it.write(byteArrayOf(1, 2, 3), 0, 3) }
        fs.rename("/a", "/b")
        assertNull(fs.stat("/a"))
        val e = fs.stat("/b")!!
        assertEquals(3L, e.size)
    }

    @Test
    fun `open read on missing throws`() {
        val fs = fs()
        assertThrows<NoSuchFileException> { fs.open("/missing", Mode.Read) }
    }
}
```

注:`kotlin.use` 在 commonMain 已存在,测试中可直接使用,无需自定义。

- [ ] **Step 2: 写 InMemoryFileSystemTest(空实现即可,继承契约)**

```kotlin
package io.appdex.core.io

class InMemoryFileSystemTest : FileSystemContractTest() {
    override fun fs(): FileSystem = InMemoryFileSystem()
}
```

- [ ] **Step 3: 跑测试,确认失败(InMemoryFileSystem 不存在)**

Run: `cd /workspace && ./gradlew :core:io:jvmTest`
Expected: 编译失败,`InMemoryFileSystem` 未解析。

- [ ] **Step 4: 写 InMemoryFileSystem 实现**

```kotlin
package io.appdex.core.io

/**
 * 内存中的 [FileSystem]。所有数据存内存,关闭后丢失。
 *
 * 用途:测试 / DEX / APK 内存编辑场景。线程不安全。
 */
class InMemoryFileSystem : FileSystem {

    private sealed class Node {
        abstract val name: String
        abstract var lastModified: Long

        class Dir(override val name: String) : Node() {
            override var lastModified: Long = 0L
            val children: MutableMap<String, Node> = LinkedHashMap()
        }

        class File(override val name: String) : Node() {
            override var lastModified: Long = 0L
            var content: ByteArray = ByteArray(0)
        }
    }

    private val root = Node.Dir("")

    override fun list(dir: String): List<Entry> {
        val d = requireDir(dir)
        return d.children.values.map { (path, name, type, size, lastModified) ->
            Entry("$dir/$name".normalizePath(), name, type, size, lastModified)
        }
    }

    override fun open(path: String, mode: Mode): SeekableChannel {
        val (parent, name) = splitParent(path)
        val parentNode = requireDir(parent)
        val existing = parentNode.children[name]
        val file = when (mode) {
            Mode.Read -> {
                val f = existing as? Node.File ?: throw NoSuchFileException(path)
                f
            }
            Mode.Write, Mode.ReadWrite -> {
                if (existing is Node.File) existing
                else {
                    val f = Node.File(name)
                    parentNode.children[name] = f
                    f
                }
            }
            Mode.Append -> {
                val f = existing as? Node.File ?: Node.File(name).also {
                    parentNode.children[name] = it
                }
                f
            }
        }
        return when (mode) {
            Mode.Read -> InMemorySeekableChannel(file.content)
            Mode.Write -> {
                file.content = ByteArray(0)
                InMemorySeekableChannel(ByteArray(0)).also {
                    // 写回需要包装,见下
                }
            }
            Mode.ReadWrite, Mode.Append -> InMemorySeekableChannel(file.content)
        }.let { channel ->
            // 包装为可写回的 channel:close 时把内容写回 Node
            WriteBackChannel(channel, file)
        }
    }

    override fun stat(path: String): Entry? {
        if (path == "/" || path.isEmpty()) {
            return Entry("/", "/", EntryType.Directory, 0L, root.lastModified)
        }
        val node = lookup(path) ?: return null
        val parent = parentOf(path)
        val name = nameOf(path)
        return Entry(
            path = path,
            name = name,
            type = when (node) {
                is Node.Dir -> EntryType.Directory
                is Node.File -> EntryType.File
            },
            size = if (node is Node.File) node.content.size.toLong() else 0L,
            lastModified = node.lastModified,
        )
    }

    override fun mkdir(path: String) {
        val (parent, name) = splitParent(path)
        val parentNode = requireDir(parent)
        if (parentNode.children.containsKey(name)) {
            throw FileAlreadyExistsException(path)
        }
        parentNode.children[name] = Node.Dir(name).also { it.lastModified = System.currentTimeMillis() }
    }

    override fun delete(path: String) {
        val (parent, name) = splitParent(path)
        val parentNode = requireDir(parent)
        if (!parentNode.children.remove(name).let { it != null }) {
            throw NoSuchFileException(path)
        }
    }

    override fun rename(from: String, to: String) {
        val (fromParent, fromName) = splitParent(from)
        val (toParent, toName) = splitParent(to)
        val fromNode = requireDir(fromParent).children.remove(fromName)
            ?: throw NoSuchFileException(from)
        requireDir(toParent).children[toName] = fromNode
    }

    // ---- helpers ----

    private fun requireDir(path: String): Node.Dir {
        val n = lookup(path) ?: throw NoSuchFileException(path)
        return n as? Node.Dir ?: throw NotDirectoryException(path)
    }

    private fun lookup(path: String): Node? {
        val parts = path.normalizePath().split("/").filter { it.isNotEmpty() }
        var current: Node = root
        for (p in parts) {
            current = when (current) {
                is Node.Dir -> current.children[p] ?: return null
                is Node.File -> return null
            }
        }
        return current
    }

    private fun splitParent(path: String): Pair<String, String> {
        val p = path.normalizePath()
        val idx = p.lastIndexOf('/')
        val parent = if (idx <= 0) "/" else p.substring(0, idx)
        val name = p.substring(idx + 1)
        return parent to name
    }

    private fun parentOf(path: String): String {
        val p = path.normalizePath()
        val idx = p.lastIndexOf('/')
        return if (idx <= 0) "/" else p.substring(0, idx)
    }

    private fun nameOf(path: String): String {
        val p = path.normalizePath()
        return p.substring(p.lastIndexOf('/') + 1)
    }

    private fun String.normalizePath(): String =
        if (endsWith('/')) dropLast(1) else this

    /** 写回包装:close 时把 channel 内容同步到 Node.File。 */
    private class WriteBackChannel(
        private val inner: InMemorySeekableChannel,
        private val file: Node.File,
    ) : SeekableChannel by inner {
        override fun close() {
            file.content = inner.toByteArray()
            file.lastModified = System.currentTimeMillis()
            inner.close()
        }
    }
}
```

注:上方 `by inner` 委托需要 SeekableChannel 是接口 —— 它确实是接口,可用类委托。

- [ ] **Step 5: 跑测试,确认通过**

Run: `cd /workspace && ./gradlew :core:io:jvmTest`
Expected: BUILD SUCCESSFUL,FileSystemContractTest 全部 12 个用例通过(由 InMemoryFileSystemTest 继承)。

如果失败:常见是 `Write` 模式后 size 没刷新或 Append 错位,看测试名定位。

- [ ] **Step 6: 提交**

```bash
git -C /workspace add core/io/src/commonMain/kotlin/io/appdex/core/io/InMemoryFileSystem.kt core/io/src/commonTest/kotlin/io/appdex/core/io/FileSystemContractTest.kt core/io/src/commonTest/kotlin/io/appdex/core/io/InMemoryFileSystemTest.kt
git -C /workspace commit -m "feat(core:io): InMemoryFileSystem 实现 + FileSystem 契约测试"
```

---

## Task 8:TDD — `NioFileChannel`(jvmMain 真机文件适配)

**Files:**
- Create: `/workspace/core/io/src/jvmMain/kotlin/io/appdex/core/io/NioFileChannel.kt`
- Create: `/workspace/core/io/src/jvmMain/kotlin/io/appdex/core/io/NioFileSystem.kt`
- Create: `/workspace/core/io/src/jvmTest/kotlin/io/appdex/core/io/NioFileSystemTest.kt`

- [ ] **Step 1: 写 NioFileSystemTest(复用契约 + 真实临时目录)**

```kotlin
package io.appdex.core.io

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class NioFileSystemTest : FileSystemContractTest() {

    @TempDir
    lateinit var tempDir: Path

    override fun fs(): FileSystem = NioFileSystem(tempDir.toFile())

    @Test
    fun `stat on real file returns size`() {
        val fs = fs()
        fs.open("/real", Mode.Write).use { it.write(byteArrayOf(1, 2, 3, 4, 5), 0, 5) }
        val e = fs.stat("/real")!!
        assertEquals(5L, e.size)
    }
}
```

- [ ] **Step 2: 跑测试,确认失败(NioFileSystem 不存在)**

Run: `cd /workspace && ./gradlew :core:io:jvmTest`
Expected: 编译失败,`NioFileSystem` / `NioFileChannel` 未解析。

- [ ] **Step 3: 写 NioFileChannel**

```kotlin
package io.appdex.core.io

import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.OpenOption
import java.nio.file.Path
import java.nio.file.StandardOpenOption

/**
 * JVM 平台 [SeekableChannel] 实现,基于 [FileChannel]。
 */
class NioFileChannel(
    private val channel: FileChannel,
    private val onClose: () -> Unit = {},
) : SeekableChannel {

    override val size: Long
        get() = channel.size()

    override fun position(newPos: Long) {
        require(newPos in 0..channel.size()) {
            "position $newPos out of [0, ${channel.size()}]"
        }
        channel.position(newPos)
    }

    override fun read(buf: ByteArray, len: Int): Int {
        require(len >= 0 && len <= buf.size) { "invalid len=$len" }
        val bb = ByteBuffer.wrap(buf, 0, len)
        return channel.read(bb)
    }

    override fun write(buf: ByteArray, off: Int, len: Int) {
        require(off >= 0 && len >= 0 && off + len <= buf.size) {
            "invalid off=$off len=$len size=${buf.size}"
        }
        channel.write(ByteBuffer.wrap(buf, off, len))
    }

    override fun close() {
        channel.close()
        onClose()
    }

    companion object {
        fun open(path: Path, mode: Mode): NioFileChannel {
            val options = mutableSetOf<OpenOption>()
            when (mode) {
                Mode.Read -> options += StandardOpenOption.READ
                Mode.Write -> options += listOf(
                    StandardOpenOption.READ,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                )
                Mode.ReadWrite -> options += listOf(
                    StandardOpenOption.READ,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.CREATE,
                )
                Mode.Append -> options += listOf(
                    StandardOpenOption.READ,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND,
                )
            }
            val ch = FileChannel.open(path, *options.toTypedArray())
            if (mode == Mode.Append) ch.position(ch.size())
            return NioFileChannel(ch)
        }
    }
}
```

- [ ] **Step 4: 写 NioFileSystem**

```kotlin
package io.appdex.core.io

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import kotlin.io.path.*

/**
 * JVM 平台 [FileSystem] 实现,基于 java.nio.file。
 *
 * [root] 是文件系统根目录,所有路径都相对于此解析。
 * 这样测试可以用临时目录,生产可以用 `/sdcard/` 或 SAF 缓存目录。
 */
class NioFileSystem(
    val root: java.io.File,
) : FileSystem {

    private val basePath: Path = root.toPath()

    override fun list(dir: String): List<Entry> {
        val p = resolve(dir)
        if (!Files.isDirectory(p)) throw NotDirectoryException(dir)
        return Files.list(p).use { stream ->
            stream.map { it.toEntry(dir) }.toList()
        }
    }

    override fun open(path: String, mode: Mode): SeekableChannel {
        val p = resolve(path)
        when (mode) {
            Mode.Read -> if (!Files.exists(p)) throw NoSuchFileException(path)
            else -> Files.createDirectories(p.parent)
        }
        return NioFileChannel.open(p, mode)
    }

    override fun stat(path: String): Entry? {
        val p = resolve(path)
        if (!Files.exists(p)) return null
        return p.toEntry(parentOf(path))
    }

    override fun mkdir(path: String) {
        val p = resolve(path)
        if (Files.exists(p)) throw FileAlreadyExistsException(path)
        Files.createDirectory(p)
    }

    override fun delete(path: String) {
        val p = resolve(path)
        if (!Files.exists(p)) throw NoSuchFileException(path)
        Files.delete(p)
    }

    override fun rename(from: String, to: String) {
        val fromP = resolve(from)
        val toP = resolve(to)
        if (!Files.exists(fromP)) throw NoSuchFileException(from)
        if (Files.exists(toP)) throw FileAlreadyExistsException(to)
        Files.move(fromP, toP)
    }

    // ---- helpers ----

    private fun resolve(path: String): Path =
        basePath.resolve(path.removePrefix("/"))

    private fun Path.toEntry(parentPath: String): Entry {
        val attrs = readAttributes<BasicFileAttributes>()
        return Entry(
            path = "$parentPath/${name}".removePrefix("//"),
            name = name,
            type = if (attrs.isDirectory) EntryType.Directory else EntryType.File,
            size = attrs.size(),
            lastModified = attrs.lastModifiedTime().toMillis(),
        )
    }

    private fun parentOf(path: String): String {
        val idx = path.lastIndexOf('/')
        return if (idx <= 0) "/" else path.substring(0, idx)
    }
}
```

- [ ] **Step 5: 跑测试,确认通过**

Run: `cd /workspace && ./gradlew :core:io:jvmTest`
Expected: BUILD SUCCESSFUL,FileSystemContractTest 12 个用例 + NioFileSystemTest 1 个用例全过。

如果失败:常见原因
- 临时目录 `/` 解析错位 → 检查 resolve
- Append 模式位置不对 → 检查 NioFileChannel.open 的 APPEND 分支
- stat 返回的 path 拼接多余 `/` → 检查 toEntry

- [ ] **Step 6: 提交**

```bash
git -C /workspace add core/io/src/jvmMain/ core/io/src/jvmTest/
git -C /workspace commit -m "feat(core:io): NioFileSystem/NioFileChannel JVM 平台实现"
```

---

## Task 9:detekt 静态检查

**Files:**
- Modify: `/workspace/gradle/libs.versions.toml`
- Modify: `/workspace/build.gradle.kts`
- Create: `/workspace/config/detekt.yml`

- [ ] **Step 1: 在 version catalog 加 detekt**

修改 `/workspace/gradle/libs.versions.toml`,在 `[versions]` 加:

```toml
detekt = "1.23.7"
```

在 `[plugins]` 加:

```toml
detekt = { id = "io.gitlab.arturbosch.detekt", version.ref = "detekt" }
```

- [ ] **Step 2: 在根 build.gradle.kts 应用 detekt**

修改 `/workspace/build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.detekt) apply false
}

allprojects {
    apply(plugin = "io.gitlab.arturbosch.detekt")
    detekt {
        config.setFrom("$rootDir/config/detekt.yml")
        buildUponDefaultConfig = true
    }
}
```

- [ ] **Step 3: 写最小 detekt 配置**

`/workspace/config/detekt.yml`:

```yaml
config:
  validation: true

complexity:
  LongMethod:
    threshold: 80
  LongParameterList:
    functionThreshold: 6
  ComplexCondition:
    threshold: 5

style:
  ReturnCount:
    max: 4
  WildcardImport:
    active: false  # Kotlin 测试代码常用 wildcard
```

- [ ] **Step 4: 跑 detekt**

Run: `cd /workspace && ./gradlew detekt`
Expected: BUILD SUCCESSFUL,可能有少量 style 警告。如果有 error,根据提示修。

- [ ] **Step 5: 提交**

```bash
git -C /workspace add gradle/libs.versions.toml build.gradle.kts config/detekt.yml
git -C /workspace commit -m "quality: 接入 detekt 静态检查"
```

---

## Task 10:README 与最终验证

**Files:**
- Modify: `/workspace/README.md`

- [ ] **Step 1: 更新 README**

完整替换 `/workspace/README.md`:

```markdown
# appdex

面向极客的 Android 工具集,目标是在逆向相关核心能力上比肩 MT 管理器、架构支持多年演进。

## 状态

子项目 #1a: `:core:io` KMP 文件系统抽象 —— 进行中

## 构建

要求:JDK 17+(当前用 JDK 25)、Gradle 8.14.4(已包含 wrapper)。

```bash
./gradlew :core:io:build          # 全部构建 + 测试
./gradlew :core:io:jvmTest        # 仅 JVM 单测
./gradlew detekt                 # 静态检查
```

## 模块结构

详见 `docs/superpowers/specs/2026-07-07-appdex-global-architecture-design.md`。

## 测试分工

- 核心逻辑:本项目自测,JVM 单测覆盖
- Android UI:需用户装机验证(后续子项目)
```

- [ ] **Step 2: 跑完整 build + 测试 + 静态检查**

Run: `cd /workspace && ./gradlew :core:io:build detekt`
Expected: BUILD SUCCESSFUL,所有测试通过,detekt 无 error。

- [ ] **Step 3: 提交**

```bash
git -C /workspace add README.md
git -C /workspace commit -m "docs: 更新 README,记录 #1a 状态与构建命令"
```

---

## 完成标准

- [ ] `./gradlew :core:io:build` 全绿
- [ ] `./gradlew detekt` 无 error
- [ ] 所有 task 已提交到 git
- [ ] `:core:io` 暴露 `SeekableChannel` / `FileSystem` / `Entry` / `Mode` / 异常体系
- [ ] `InMemoryFileSystem` 与 `NioFileSystem` 均通过 `FileSystemContractTest`
- [ ] `InMemorySeekableChannel` 与 `NioFileChannel` 均通过 `SeekableChannelContractTest`

## 后续(下一份 Plan 1b)

- `:core:ui` Compose 共享组件、M3 主题
- `:feature:files` 文件浏览 UI
- `:app` 入口、导航、DI、权限申请
- 需要 Android SDK,在用户本地 build
