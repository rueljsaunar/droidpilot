pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        maven { url = uri("https://chaquo.com/maven/") }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        maven { url = uri("https://chaquo.com/maven/") }
        mavenCentral()
    }
}

rootProject.name = "DroidPilot"

include(":detekt-rules")
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
include(":feature:object-repository")
include(":feature:recorder")
include(":sample-target")
