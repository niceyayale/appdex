# Contributing to APPDEX

Thank you for your interest in contributing to APPDEX! This document outlines the process for contributing to the project.

## Code of Conduct

Be respectful, constructive, and welcoming. We are all here to build something great together.

## Getting Started

1. **Fork** the repository
2. **Clone** your fork: `git clone https://github.com/<your-username>/appdex.git`
3. **Create a branch**: `git checkout -b feature/your-feature-name`
4. **Build** the project: `./gradlew assembleDebug`
5. **Test** your changes thoroughly
6. **Commit** with clear messages (see format below)
7. **Push** and submit a Pull Request

## Development Setup

### Requirements
- JDK 17
- Android SDK API 35
- Android Studio (latest stable recommended)

### Architecture
APPDEX follows **MVI (Model-View-Intent)** architecture:
- **Model**: State held in `StateFlow`
- **View**: Jetpack Compose UI
- **Intent**: User actions dispatched via sealed classes

### Module Structure
- `core/` — Shared infrastructure (arch, ui, data, database, model, common, plugin)
- `feature/` — Feature modules (files, editor, analyzer, player, terminal, tools, remote, settings)
- `library/` — Reusable libraries (syntax, archive, apk)

## Coding Standards

### Kotlin
- Follow [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)
- Use `data class` for state models
- Use `sealed interface` for MVI intents and effects
- Prefer `StateFlow` over `LiveData`
- Use coroutines for async operations

### Compose
- Keep composables small and focused
- Use `remember` for state that survives recomposition
- Use `rememberSaveable` for state that survives configuration changes
- Extract reusable composables into separate functions

### Git Commit Format
```
<type>(<scope>): <subject>

<body>
```

Types: `feat`, `fix`, `docs`, `style`, `refactor`, `test`, `chore`

Example:
```
feat(terminal): add ANSI 256-color support

Implemented full 256-color and RGB true color parsing for terminal
output, including bold text detection.
```

## Pull Request Process

1. Ensure your code compiles: `./gradlew assembleDebug`
2. Update documentation if needed (README, CHANGELOG)
3. Add your change to the appropriate section in `CHANGELOG.md`
4. Submit the PR with a clear description of what and why

## Plugin Development

APPDEX supports compile-time plugins. To create a new plugin:

1. Implement the `AppDexPlugin` interface
2. Register it in `PluginManager` at app startup
3. Place the plugin in `feature/feature-tools/src/main/java/com/appdex/tools/plugins/`

```kotlin
class MyPlugin : AppDexPlugin {
    override val id = "my_plugin"
    override val name = "My Plugin"
    override val description = "Does something useful"
    override val author = "Your Name"
    override val version = "1.0.0"
    override val category = PluginCategory.UTILITY

    @Composable
    override fun Content() {
        // Your Compose UI here
    }
}
```

## Reporting Issues

- Use GitHub Issues
- Include device model, Android version, and APPDEX version
- Attach screenshots or logs if possible
- Describe steps to reproduce

## License

By contributing, you agree that your contributions will be licensed under the Apache 2.0 License.
