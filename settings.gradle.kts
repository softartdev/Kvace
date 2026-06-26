rootProject.name = "Kvace"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

include(":app:androidApp")
include(":app:desktopApp")
include(":app:shared")
include(":app:webApp")

include(":core:domain")
include(":core:data")
include(":core:presentation")
include(":core:ui")

include(":feature:agent:domain")
include(":feature:agent:data")
include(":feature:agent:presentation")
include(":feature:agent:ui")

include(":feature:chat:domain")
include(":feature:chat:data")
include(":feature:chat:presentation")
include(":feature:chat:ui")

include(":feature:settings:domain")
include(":feature:settings:data")
include(":feature:settings:presentation")
include(":feature:settings:ui")
