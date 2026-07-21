# Changelog

All notable changes to APPDEX will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [v3.0.0] - 2026-07-21

### Workspace Intelligence - Workspace OS Architecture
- **WorkspaceObject**: Single source of truth for entire app state
- **WorkspaceEventBus**: Central event system for cross-tool communication (SharedFlow-based)
- **WorkspaceController**: Brain that processes all events and updates state
- **WorkspaceInspector**: Global sidebar showing APK/Tool/Selection/Breadcrumbs/AI Insights/Timeline
- **Cross-Tool Intelligence**: Any object can navigate to any tool (web not tree)
- **AI Workspace Awareness**: Proactive insights without user asking
  - Auto-detects Flutter/Unity engines from class names and native libs
  - Auto-warns on dangerous permissions and exported components
  - Auto-explains tool purpose when entering each view
- **Live Report**: Real-time updates via reportRevision StateFlow
- **Workspace Memory**: AI remembers recent actions via buildWorkspaceMemory()
- **Event sources**: Security/Signing/Repack/Editor all emit WorkspaceEvents
- **Phase 7 Polish**: AutoMirrored icons, unified design system, empty states

## [v2.0.0] - 2026-07-15

### AI Native APK Analysis Platform
- **AI Integration**: Streaming chat with OpenRouter API, session persistence
- **Session Management**: Multi-session support with Room database
- **Tool Bridge**: Structured AI responses for actionable insights
- **Search**: Global search across manifest, DEX, resources
- **Theme Optimization**: Refined color scheme and typography
- **Risk Score Calculator**: Automated security risk scoring

## [v1.0.0] - 2026-07-12

### Project Completion
- Updated README (EN/ZH) with full feature documentation
- Added CONTRIBUTING.md with development guidelines
- Added CHANGELOG.md for version tracking
- Enhanced ProGuard/R8 rules for release builds
- Updated version code and name for production release
- Added multi-language string resources (EN/ZH)
- Verified release APK build

## [v0.7.0] - 2026-07-11

### Added
- **Plugin System**: `AppDexPlugin` interface + `PluginManager` registry (compile-time, no DEX)
- **Built-in Plugins**: JSON Formatter, Text Statistics, Timestamp Converter
- **Settings**: Language switch (English/Chinese/System)
- **Settings**: Terminal font size and scrollback line controls
- **Settings**: Cache management (view size + clear)
- **Settings**: Enhanced About section with version, license, architecture info
- Plugin list UI with enable/disable toggle and category chips

## [v0.6.0] - 2026-07-11

### Added
- **Web Server**: Embedded HTTP server (ServerSocket) with file browse/download/upload
- **Web Server**: QR code generation (ZXing) for easy mobile-to-PC pairing
- **FTP Client**: Connect, browse, and download from remote FTP servers (Apache Commons Net)
- **Remote tab** with Web Server / FTP Client tab switcher
- Added `commons-net` and `zxing-core` dependencies

## [v0.5.0] - 2026-07-11

### Added
- **Terminal**: ProcessBuilder-based session with async output streaming
- **Terminal**: ANSI 16-color + 256-color + RGB true color parsing
- **Terminal**: Quick keys bar (Ctrl+C, Tab, history navigation, Enter)
- **Terminal**: Command history with up/down arrow navigation
- **Tools**: Hash Calculator (MD5/SHA-1/SHA-256 for text & files)
- **Tools**: Device Info viewer (hardware/OS/CPU/memory/storage)
- **Tools**: Encoding Converter (Base64/URL/Hex/Binary encode & decode)
- Added Terminal and Tools tabs to bottom navigation

## [v0.4.0] - 2026-07-10

### Added
- **Image Viewer**: Coil-based with zoom, pan, rotate, multi-page navigation
- **Video Player**: ExoPlayer (Media3) with playback controls
- **Audio Player**: Playlist support with background playback
- **APK Signatures**: V2/V3 signature block parsing with X.509 certificate extraction
- `MediaNavigationBus` for cross-module media routing

## [v0.3.0] - 2026-07-09

### Added
- **APK Analyzer**: APK file inspection, content listing
- **Binary XML Decoder**: Pure Kotlin `AndroidManifest.xml` binary decoding
- **APK Models**: Package info, permissions, activities, services, providers
- Navigation integration for APK analyzer tab

## [v0.2.0] - 2026-07-08

### Added
- **Code Editor**: `BasicTextField` with `VisualTransformation` real-time syntax highlighting
- **Syntax Engine**: 20+ language support (Kotlin, Java, Python, JS, TS, HTML, CSS, XML, JSON, etc.)
- **Editor Features**: Search/replace, line numbers, font size & tab width customization
- **File Manager**: Bookmarks, directory picker, copy/move/delete operations

## [v0.1.0] - 2026-07-07

### Added
- Project skeleton with multi-module Gradle architecture
- **MVI Architecture**: `BaseViewModel`, `MviState`, `MviIntent`, `MviEffect`
- **Theme System**: "Deep Space" color palette, Material 3 dynamic color support
- **File Manager**: Browse directories, file listing with icons
- **Settings**: Theme mode, info density, show hidden files, remember last path
- Hilt dependency injection setup
- Compose Navigation with type-safe routes
- DataStore preferences for settings persistence
- Room database for bookmarks
- GitHub Actions CI/CD for automatic release builds
