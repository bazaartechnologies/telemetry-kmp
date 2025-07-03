pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
    plugins {
        id("com.android.library") version "8.4.0"
        id("org.jetbrains.kotlin.multiplatform") version "2.2.0"
        id("org.jetbrains.kotlin.plugin.serialization") version "2.2.0"
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "telemetry-kmp"
include(":telemetry-kmp")