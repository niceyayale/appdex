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

rootProject.name = "APPDEX"

// ── App ──
include(":app")

// ── Core ──
include(":core:core-arch")
include(":core:core-ui")
include(":core:core-data")
include(":core:core-database")
include(":core:core-model")
include(":core:core-common")

// ── Feature ──
include(":feature:feature-files")
include(":feature:feature-editor")
include(":feature:feature-analyzer")
include(":feature:feature-settings")
include(":feature:feature-player")
include(":feature:feature-terminal")
include(":feature:feature-tools")
include(":feature:feature-remote")

// ── Library ──
include(":library:lib-syntax")
include(":library:lib-archive")
include(":library:lib-apk")
