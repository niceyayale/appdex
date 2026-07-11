# APPDEX

> Your Android, Understood.

APPDEX is an open-source Android file manager and application analysis tool.

## Features

- **File Manager** — Browse, copy, move, delete, compress, and extract files
- **Code Editor** — Syntax-highlighted text editor with search/replace
- **APK Analyzer** — Inspect APK contents, manifest, signatures, and DEX files
- **Terminal** — Built-in terminal emulator (planned)
- **Remote Management** — Web-based file access via embedded server (planned)

## Tech Stack

| Component | Technology |
|-----------|-----------|
| Language | Kotlin 2.x |
| UI | Jetpack Compose + Material 3 |
| Architecture | MVI (Model-View-Intent) |
| DI | Hilt |
| Persistence | DataStore + Room |
| Navigation | Compose Navigation (Type-safe) |
| Network | Ktor |
| Media | Media3 (ExoPlayer) |

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
├── app/                    # Shell application
├── core/
│   ├── core-arch/          # MVI architecture base
│   ├── core-ui/            # Theme, components, icons
│   ├── core-data/          # Data layer (DataStore)
│   ├── core-database/      # Room database
│   ├── core-model/         # Domain models
│   └── core-common/        # Shared utilities
├── feature/
│   ├── feature-files/      # File manager
│   ├── feature-editor/     # Code editor
│   ├── feature-analyzer/   # APK analyzer
│   └── feature-settings/   # Settings
└── library/
    ├── lib-syntax/         # Syntax highlighting engine
    ├── lib-archive/        # Compression/decompression
    └── lib-apk/            # APK parsing (pure Kotlin)
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

## License

Apache 2.0 — See [LICENSE](LICENSE)
