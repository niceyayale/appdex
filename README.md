# APPDEX

> Your Android, Understood.

[English](./README.md) | [中文](./README_zh.md)

APPDEX is a 100% open-source, ad-free Android toolkit for power users.  
No telemetry, no VIP gates, no dynamic DEX loading — just clean, original code.

## Features

### 📁 File Manager
Browse, copy, move, delete, compress (ZIP/7Z/TAR), and extract files with bookmarks and history.

### 💻 Code Editor
Syntax-highlighted editor supporting 20+ languages with search/replace, line numbers, and customizable font/tab width.

### 📦 APK Analyzer
Inspect APK contents, binary `AndroidManifest.xml` decoding, V2/V3 signature verification with X.509 certificate extraction.

### 🖥️ Terminal
Built-in terminal emulator with ANSI 256-color parsing, command history, and quick keys (Ctrl+C, Tab, arrows).

### 🔧 Tools
- **Hash Calculator** — MD5 / SHA-1 / SHA-256 for text or files
- **Device Info** — Hardware, OS, CPU, memory, storage details
- **Encoding Converter** — Base64 / URL / Hex / Binary encode & decode

### 🌐 Remote Management
- **Web Server** — Embedded HTTP server with QR code pairing, file browse/download/upload from any browser
- **FTP Client** — Connect to remote FTP servers, browse and download files

### 🔌 Plugin System
Compile-time plugin framework with built-in plugins:
- **JSON Formatter** — Validate and pretty-print JSON
- **Text Statistics** — Word/character/line/sentence count with reading time
- **Timestamp Converter** — Unix timestamp ↔ human-readable date

### 🎵 Media Player
- **Image Viewer** — Zoom, pan, rotate, multi-page navigation
- **Video Player** — ExoPlayer-based with gesture controls
- **Audio Player** — Playlist support with background playback

### ⚙️ Settings
Theme (System/Light/Dark), language (English/Chinese), info density, editor & terminal customization, cache management.

## Download

Pre-built APKs are available on the [Releases](https://github.com/niceyayale/appdex/releases) page.

## Tech Stack

| Component | Technology |
|-----------|-----------|
| Language | Kotlin 2.0 |
| UI | Jetpack Compose + Material 3 |
| Architecture | MVI (Model-View-Intent) |
| DI | Hilt |
| Persistence | DataStore + Room |
| Navigation | Compose Navigation (Type-safe) |
| Media | Media3 (ExoPlayer) |
| Images | Coil |
| FTP | Apache Commons Net |
| QR Code | ZXing |
| APK Parsing | Pure Kotlin (no third-party) |

## Design

- **Design Language**: "Spectrum Design"
- **Color Palette**: "Deep Space" — inspired by astrophotography
  - Primary: Amber Gold `#E8B547`
  - Background: Deep Space Blue `#0B1426`
  - Secondary: Nebula Blue `#5B9BD5`
  - Tertiary: Aurora Green `#7DD3C0`

## Project Structure

```
appdex/
├── app/                        # Shell application
├── core/
│   ├── core-arch/              # MVI architecture base
│   ├── core-ui/                # Theme, components, icons
│   ├── core-data/              # Data layer (DataStore)
│   ├── core-database/          # Room database
│   ├── core-model/             # Domain models
│   ├── core-common/            # Shared utilities
│   └── core-plugin/            # Plugin system framework
├── feature/
│   ├── feature-files/          # File manager
│   ├── feature-editor/         # Code editor
│   ├── feature-analyzer/       # APK analyzer
│   ├── feature-player/         # Media player (image/video/audio)
│   ├── feature-terminal/       # Terminal emulator
│   ├── feature-tools/          # Utility tools + plugins
│   ├── feature-remote/         # Web server + FTP client
│   └── feature-settings/       # Settings
└── library/
    ├── lib-syntax/             # Syntax highlighting engine
    ├── lib-archive/            # Compression/decompression
    └── lib-apk/                # APK parsing (pure Kotlin)
```

## Building

Requirements:
- JDK 17
- Android SDK (API 35)
- Gradle 8.x (wrapper included)

```bash
git clone https://github.com/niceyayale/appdex.git
cd appdex
./gradlew assembleDebug
```

For release build:
```bash
./gradlew assembleRelease
```

## Privacy

APPDEX contains **no analytics, no trackers, no ads**. All file operations happen locally on your device. The web server only listens on your local network and can be stopped at any time.

## Contributing

See [CONTRIBUTING.md](./CONTRIBUTING.md) for guidelines.

## Changelog

See [CHANGELOG.md](./CHANGELOG.md) for version history.

## License

Apache 2.0 — See [LICENSE](LICENSE)
