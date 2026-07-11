# APPDEX

> 深入理解你的 Android 设备

[English](./README.md) | [中文](./README_zh.md)

APPDEX 是一款 100% 开源、无广告的 Android 工具箱。  
无遥测、无 VIP 限制、无动态 DEX 加载 — 纯净原创代码。

## 功能特性

### 📁 文件管理
浏览、复制、移动、删除、压缩（ZIP/7Z/TAR）、解压文件，支持书签和历史记录。

### 💻 代码编辑器
支持 20+ 语言语法高亮，查找替换、行号显示，可自定义字体和缩进宽度。

### 📦 APK 分析器
检查 APK 内容、二进制 `AndroidManifest.xml` 解码、V2/V3 签名验证及 X.509 证书提取。

### 🖥️ 终端
内置终端模拟器，支持 ANSI 256 色解析、命令历史、快捷键栏（Ctrl+C、Tab、方向键）。

### 🔧 工具集
- **哈希计算器** — 文本/文件的 MD5 / SHA-1 / SHA-256
- **设备信息** — 硬件、系统、CPU、内存、存储详情
- **编码转换器** — Base64 / URL / Hex / Binary 编解码

### 🌐 远程管理
- **Web 服务器** — 内嵌 HTTP 服务器，二维码配对，浏览器端文件浏览/下载/上传
- **FTP 客户端** — 连接远程 FTP 服务器，浏览和下载文件

### 🔌 插件系统
编译时插件框架，内置插件：
- **JSON 格式化** — 验证并美化 JSON
- **文本统计** — 字数/字符/行数/句子统计 + 阅读时间估算
- **时间戳转换** — Unix 时间戳 ↔ 可读日期互转

### 🎵 媒体播放器
- **图片查看器** — 缩放、平移、旋转、多页导航
- **视频播放器** — 基于 ExoPlayer，支持手势控制
- **音频播放器** — 播放列表支持，后台播放

### ⚙️ 设置
主题（系统/浅色/深色）、语言（英文/中文）、信息密度、编辑器和终端自定义、缓存管理。

## 下载

预构建的 APK 可在 [Releases](https://github.com/niceyayale/appdex/releases) 页面下载。

## 技术栈

| 组件 | 技术 |
|------|------|
| 语言 | Kotlin 2.0 |
| UI | Jetpack Compose + Material 3 |
| 架构 | MVI (Model-View-Intent) |
| 依赖注入 | Hilt |
| 持久化 | DataStore + Room |
| 导航 | Compose Navigation (类型安全) |
| 媒体 | Media3 (ExoPlayer) |
| 图片 | Coil |
| FTP | Apache Commons Net |
| 二维码 | ZXing |
| APK 解析 | 纯 Kotlin (无第三方依赖) |

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
├── app/                        # 壳工程
├── core/
│   ├── core-arch/              # MVI 架构基础
│   ├── core-ui/                # 主题、组件、图标
│   ├── core-data/              # 数据层 (DataStore)
│   ├── core-database/          # Room 数据库
│   ├── core-model/             # 领域模型
│   ├── core-common/            # 通用工具
│   └── core-plugin/            # 插件系统框架
├── feature/
│   ├── feature-files/          # 文件管理器
│   ├── feature-editor/         # 代码编辑器
│   ├── feature-analyzer/       # APK 分析器
│   ├── feature-player/         # 媒体播放器 (图片/视频/音频)
│   ├── feature-terminal/       # 终端模拟器
│   ├── feature-tools/          # 实用工具 + 插件
│   ├── feature-remote/         # Web 服务器 + FTP 客户端
│   └── feature-settings/       # 设置
└── library/
    ├── lib-syntax/             # 语法高亮引擎
    ├── lib-archive/            # 压缩/解压
    └── lib-apk/                # APK 解析 (纯 Kotlin)
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

Release 构建:
```bash
./gradlew assembleRelease
```

## 隐私

APPDEX **不含分析、追踪和广告**。所有文件操作均在设备本地完成。Web 服务器仅监听局域网，可随时关闭。

## 贡献

详见 [CONTRIBUTING.md](./CONTRIBUTING.md)。

## 更新日志

详见 [CHANGELOG.md](./CHANGELOG.md)。

## 开源协议

Apache 2.0 — 详见 [LICENSE](LICENSE)
