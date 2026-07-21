pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "AppX"

// ── App ──
include(":app")

// ── Core ──
include(":core:core-arch")
include(":core:core-ui")
include(":core:core-data")
include(":core:core-database")
include(":core:core-model")
include(":core:core-common")
include(":core:core-plugin")

// ── Feature ──
include(":feature:feature-files")
include(":feature:feature-editor")
include(":feature:feature-analyzer")
include(":feature:feature-settings")
include(":feature:feature-player")
include(":feature:feature-terminal")
include(":feature:feature-tools")
include(":feature:feature-remote")
include(":feature:feature-dex")
include(":feature:feature-hex")
include(":feature:feature-signing")
include(":feature:feature-repack")
include(":feature:feature-diff")
include(":feature:feature-security")
include(":feature:feature-size")
include(":feature:feature-axml")
include(":feature:feature-arsc")
include(":feature:feature-sqlite")
include(":feature:feature-elf")

// ── Library ──
include(":library:lib-syntax")
include(":library:lib-archive")
include(":library:lib-apk")
