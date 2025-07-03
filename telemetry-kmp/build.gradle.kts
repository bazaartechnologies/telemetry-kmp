import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.library")
    kotlin("multiplatform") // Version should be managed by settings.gradle.kts or root project
    id("org.jetbrains.kotlin.plugin.serialization") // Version should be managed
    id("maven-publish")
    alias(libs.plugins.wire)
}

kotlin {
    androidTarget {
        publishLibraryVariants("debug", "release")

        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    jvmToolchain(21) // Sets the JDK toolchain for Kotlin JVM compilations

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.ktor.client.core)
                implementation(libs.wire.runtime)
//                implementation(libs.wire.runtime) // Wire runtime for common
                // If you use Ktor in common code and need a default engine (or expect platform engines)
                // implementation(libs.ktor.client.cio) // Example
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val androidMain by getting {
            dependencies {
                implementation(project.dependencies.platform(libs.opentelemetry.bom))
                implementation(libs.opentelemetry.api)
                implementation(libs.opentelemetry.sdk)
                implementation(libs.opentelemetry.exporter.otlp)
                implementation(libs.opentelemetry.android.agent)
                implementation(libs.ktor.client.android)
            }
        }
        val iosX64Main by getting {
            dependencies {
                implementation(libs.ktor.client.darwin)
            }
        }
        val iosArm64Main by getting {
            dependencies {
                implementation(libs.ktor.client.darwin)
            }
        }
        val iosSimulatorArm64Main by getting {
            dependencies {
                implementation(libs.ktor.client.darwin)
            }
        }
        // Consider creating intermediate source sets if there's shared code between iOS targets
        // e.g., val iosMain by creating { dependsOn(commonMain) }
        // then iosX64Main.dependsOn(iosMain), etc.
    }
}

android {
    namespace = "com.bazaar.telemetry"
    compileSdk = 34
    defaultConfig {
        minSdk = 21
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildFeatures {
        buildConfig = true
    }
}

wire {
    kotlin {
        // Configure Wire for Kotlin code generation
        // For KMP, outputting to a common directory that platform source sets can pick up from is typical.
        // Or, configure per-target generation if needed.
        // Wire plugin should ideally handle KMP source set integration.
        // If proto files are in commonMain, they can be used by all targets.
        // Example: include "opentelemetry/proto/**/*.proto"
        // out = "${buildDir}/generated/source/wire" // Default or specify
        // For KMP, you often target specific source sets or common.
        // Let's assume protos in src/commonMain/proto and generate for commonMain if possible,
        // or specifically for iosMain if common generation is problematic for other platforms.
        // For now, this basic setup might need refinement based on actual proto paths and KMP structure.
    }
}