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
    versionCatalogs {
        create("libs") {
            from(files("gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "DroidPilot"

include(":app")
include(":core:automation")
include(":backend:accessibility")
include(":backend:shizuku")
include(":backend:vision")
include(":script:python")
include(":runner")
include(":reporting")
include(":feature:editor")
include(":feature:projects")
include(":feature:overlay")
include(":feature:inspector")
include(":feature:recorder")
include(":sample-target")
