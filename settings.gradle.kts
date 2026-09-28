pluginManagement {
    includeBuild("build-logic")
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

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "Skycast"

include(":app")

// Core — pure Kotlin (no Android)
include(":core:model")
include(":core:common")
include(":core:domain")
include(":core:testing")

// Core — Android infrastructure
include(":core:network")
include(":core:database")
include(":core:datastore")
include(":core:location")
include(":core:data")

// Core — presentation
include(":core:mvi")
include(":core:designsystem")
include(":core:ui")
include(":core:ai")

// Background sync
include(":sync:work")

// Features
include(":feature:home")
include(":feature:places")
include(":feature:settings")
include(":feature:routine")
include(":feature:widget")
