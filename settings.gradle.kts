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

rootProject.name = "MapMe"

// Module map — see docs/ARCHITECTURE.md for why these boundaries exist and
// which modules future sprints are expected to add.
include(":app")
include(":core:design")
include(":core:location")
include(":core:map")
include(":core:model")
