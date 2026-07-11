# APPDEX

> 深入理解你的 Android 设备

[English](./README.md) | [中文](./README_zh.md)

APPDEX 是一款开源的 Android 文件管理与应用分析工具。

## 功能特性

- **文件管理** — 浏览、复制、移动、删除、压缩、解压文件
- **代码编辑器** — 支持语法高亮的文本编辑器，支持查找替换
- **APK 分析器** — 检查 APK 内容、清单文件、签名信息和 DEX 文件
- **终端** — 内置终端模拟器（规划中）
- **远程管理** — 通过内嵌服务器实现 Web 端文件访问（规划中）

## 下载

预构建的 APK 可在 [Releases](https://github.com/niceyayale/appdex/releases) 页面下载。

## 技术栈

| 组件 | 技术 |
|------|------|
| 语言 | Kotlin 2.x |
| UI | Jetpack Compose + Material 3 |
| 架构 | MVI (Model-View-Intent) |
| 依赖注入 | Hilt |
| 持久化 | DataStore + Room |
| 导航 | Compose Navigation (类型安全) |
| 网络 | Ktor |
| 媒体 | Media3 (ExoPlayer) |

## 设计

- **设计语言**: "Spectrum Design"（光谱设计）
- **配色方案**: "Deep Space"（深空系列）— 灵感来自天文摄影
  - 主色: 琥珀金 `#E8B547`
  - 背景: 深空蓝 `#0B1426`
  - 次色: 星云蓝 `#5B9BD5`
  - 第三色: 极光绿 `#7DD3C0`

## 项目结构

```
appdex/
├── app/                    # 壳工程
├── core/
│   ├── core-arch/          # MVI 架构基础
│   ├── core-ui/            # 主题、组件、图标
│   ├── core-data/          # 数据层 (DataStore)
│   ├── core-database/      # Room 数据库
│   ├── core-model/         # 领域模型
│   └── core-common/        # 通用工具
├── feature/
│   ├── feature-files/      # 文件管理器
│   ├── feature-editor/     # 代码编辑器
│   ├── feature-analyzer/   # APK 分析器
│   └── feature-settings/   # 设置
└── library/
    ├── lib-syntax/         # 语法高亮引擎
    ├── lib-archive/        # 压缩/解压
    └── lib-apk/            # APK 解析 (纯 Kotlin)
```

## 编译

环境要求:
- JDK 17
- Android SDK (API 35)
- Gradle 8.x (已包含 wrapper)

```bash
git clone https://github.com/niceyayale/appdex.git
cd appdex
./gradlew assembleDebug
```

## 开源协议

Apache 2.0 — 详见 [LICENSE](LICENSE)
