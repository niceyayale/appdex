# appdex 全局架构设计

- 日期:2026-07-07
- 状态:已确认,作为后续子项目设计的总纲
- 适用范围:整个 appdex 项目

## 1. 项目定位

一个面向极客/逆向爱好者的开源 Android 工具,目标是做一款在逆向相关核心能力上比肩甚至超越 MT 管理器、架构支持多年演进的产品。

- 平台:Android 原生 APP,核心库 KMP 跨平台(Android + JVM)
- min SDK:26(Android 8.0,覆盖 95%+ 设备)
- target SDK:最新稳定版
- UI 技术栈:Jetpack Compose + Material 3
- 许可证:Apache 2.0
- 命名空间:`io.appdex.*`

## 2. 核心原则

1. **接口先行,实现可替换**:每个核心模块对外暴露 `interface`,实现类放 `internal` 包,通过 DI 注入。任何一块都能在不影响其他模块的前提下重写 —— 这是"多年后追上 MT"的根本机制。
2. **核心逻辑可独立测试**:所有 `:core:*` 模块必须能在 JVM 上跑单测,不依赖 Android 运行时。真机只验证 UI 与平台行为。
3. **依赖单向向下**:`core` 之间按图示顺序依赖,`feature` 依赖 `core`,`app` 是唯一可启动模块。禁止反向依赖。
4. **YAGNI**:不做当前用不到的抽象,但保留扩展点。
5. **质量基线强制**:单测、编译、lint 全绿才算完成;API 二进制兼容受控。

## 3. 模块结构

Gradle 多模块,KMP 核心 + Android UI。

```
:core:io            KMP      文件系统抽象(POSIX/SAF/Zip 内存映射),统一 SeekableChannel/FileSystem
:core:apk           KMP      APK 读写、ZIP、签名块读取(自研,依赖 :core:io)
:core:axml          KMP      二进制 AndroidManifest.xml 解析/编辑(自研,依赖 :core:io)
:core:arsc          KMP      resources.arsc 解析/编辑(自研,依赖 :core:io)
:core:dex           KMP      DEX 读写 + smali 桥(适配 dexlib2,仅 JVM/Android target)
:core:signing       KMP      v1/v2/v3 签名与校验(基于 BouncyCastle,仅 JVM/Android target)
:core:project       KMP      逆向工程项目模型:打开/修改跟踪/回编译编排,依赖上面各 core
:core:ui            Android  Compose 共享组件(代码编辑器、文件列表、树视图)、M3 主题
:feature:files      Android  文件浏览/操作
:feature:apk        Android  APK 详情/查看/重打包
:feature:dex        Android  DEX/Smali 编辑
:feature:hex        Android  十六进制编辑器
:feature:text       Android  文本编辑器(语法高亮)
:feature:project    Android  逆向项目工作台
:app                Android  入口、导航、DI 容器、权限申请
```

依赖方向:`:core:*` 单向向下;`:feature:*` 依赖 `:core:*`,不持有业务逻辑;`:app` 是唯一可启动模块。

## 4. 分层

```
┌─────────────────────────────────────────────┐
│  :app  (入口/导航/DI/权限)                    │
├─────────────────────────────────────────────┤
│  :feature:*  (Compose UI + ViewModel)        │  ← 只持有 UI 逻辑
├─────────────────────────────────────────────┤
│  :core:project  (逆向项目编排)                │  ← 工作流
├─────────────────────────────────────────────┤
│  :core:apk :axml :arsc :dex :signing :io     │  ← 核心能力,接口驱动
└─────────────────────────────────────────────┘
```

## 5. `:core:io` —— 地基抽象

定义与平台无关的 I/O 接口,所有其他 core 模块只通过它访问字节。

```kotlin
interface SeekableChannel : Closeable {
    val size: Long
    fun position(newPos: Long)
    fun read(buf: ByteArray, len: Int): Int
    fun write(buf: ByteArray, off: Int, len: Int)
}

interface FileSystem {
    fun open(path: String, mode: Mode): SeekableChannel
    fun list(dir: String): List<Entry>
    // stat / rename / delete / mkdir ...
}
```

- `commonMain`:接口 + `InMemoryFileSystem`(测试用)
- `androidMain`:`RandomAccessFile` + SAF 适配
- `jvmMain`:`java.nio.channels.FileChannel`

好处:任何 core 模块都只面向 `FileSystem`,不关心字节来自真机文件、SAF Uri 还是内存。单测可在 JVM 上用内存 FS 跑,无需 Android。

## 6. 各核心模块的接口约定

每个 core 模块至少有这两个文件:

```kotlin
// api/<Name>Api.kt   —— 公共契约
interface ApkReader {
    fun readManifest(channel: SeekableChannel): AndroidManifest
    fun listEntries(channel: SeekableChannel): List<ApkEntry>
}

// internal/<Name>ApiImpl.kt  —— 默认实现
internal class ApkReaderImpl : ApkReader { ... }
```

模块通过 `expect fun / class` 提供工厂,Android/JVM 注入实现。**未来某块要重写(比如自研 DEX 替换 dexlib2),只换 Impl,API 不变。**

## 7. 演进与版本策略

- 每个 core 模块独立 `version.txt`(语义化),实现大改时 bump
- DI 容器绑定 `interface -> impl@version`,可并存多版本实现(用于灰度/对照)
- `:core:project` 通过接口调用各 core,新增能力走 capability 探测,不强耦合
- API 二进制兼容受 `binary-compatibility-validator` 控制,意外破坏会编译失败

## 8. 解析器来源(混合策略)

| 能力 | 来源 |
|------|------|
| APK 结构 | 自研(按 ZIP + APK 规范) |
| AXML | 自研(按 Android 源码 `frameworks/base`) |
| ARSC | 自研(按 Android 源码) |
| DEX / Smali | dexlib2(开源,Apache 2.0) |
| 签名 v1/v2/v3 | BouncyCastle + 自研 |

## 9. 质量与可测试性基线

- 所有 `:core:*` 必须有 JVM 单测,样本放 `:test-resources/`
- core 代码覆盖率门槛:初版 60%,逐步提到 80%
- 编译即测试:`./gradlew assembleDebug :core:*:test` 必须全绿
- detekt + ktlint 强制
- API 兼容:`apiDump` 受版本控制

## 10. 测试分工(明确边界)

| 类别 | 责任方 |
|------|--------|
| 核心逻辑单测 | 我(JVM) |
| 编译/lint/覆盖率 | 我 |
| 真机 UI 渲染 | 用户 |
| 真机权限/SAF 行为 | 用户 |
| 性能/内存/ANR/闪退 | 用户 |
| 交互手感/动画 | 用户 |

我无法自测的:真机/模拟器上的 UI 实际渲染、文件权限申请、SAF 在不同 ROM 上的行为、性能、闪退。这部分必须用户验证。

## 11. 子项目拆分(后续各自单独设计)

| # | 子项目 | 内容 | 依赖 |
|---|--------|------|------|
| 1 | 文件管理核心 + 应用骨架 | `:core:io` `:core:ui` `:feature:files` `:app` | 无 |
| 2 | APK 解析库 | `:core:apk` `:core:axml` `:core:arsc` | #1 |
| 3 | APK 打包 + 签名 | `:core:signing` | #2 |
| 4 | DEX / Smali 编辑 | `:core:dex` `:feature:dex` | #1 |
| 5 | AXML / ARSC 编辑器 | `:feature:apk` 中的编辑视图 | #2 |
| 6 | 文本 / 十六进制 / Smali 编辑器 | `:feature:text` `:feature:hex` | #1 |
| 7 | 逆向工作流编排 | `:core:project` `:feature:project` | #2-6 |

## 12. 本次设计范围

本设计为全局架构总纲,**不**包含:
- 每个核心模块的具体 API 形状(留给子项目设计)
- UI 界面交互细节(留给各 `:feature:*` 子项目)
- 回编译工作流细节(留给 `:core:project` 子项目)

后续每个子项目走独立的设计 → 计划 → 实现循环,以本总纲为约束。
