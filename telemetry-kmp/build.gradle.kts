import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.library")
    kotlin("multiplatform") // Version should be managed by settings.gradle.kts or root project
    id("org.jetbrains.kotlin.plugin.serialization") // Version should be managed
    id("maven-publish")
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
                // If you use Ktor in common code and need a default engine (or expect platform engines)
                // implementation(libs.ktor.client.cio) // Example

                // OpenTelemetry API can be common if using the KMP native version across all platforms
                // For now, KMP API is added to iosMain, Java API to androidMain
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
                implementation(libs.opentelemetry.api) // Java API
                implementation(libs.opentelemetry.sdk)  // Java SDK
                implementation(libs.opentelemetry.exporter.otlp) // Java OTLP gRPC exporter
                implementation(libs.opentelemetry.android.agent)
                implementation(libs.ktor.client.android)
            }
        }

        // Create an intermediate source set for all iOS targets
        val iosMain by creating {
            dependsOn(commonMain)
            dependencies {
                implementation(libs.ktor.client.darwin)
                implementation(libs.opentelemetry.kotlin.api)
                implementation(libs.opentelemetry.kotlin.sdk)
                implementation(libs.opentelemetry.kotlin.exporter.otlp.http)
            }
        }
        val iosX64Main by getting {
            dependsOn(iosMain)
        }
        val iosArm64Main by getting {
            dependsOn(iosMain)
        }
        val iosSimulatorArm64Main by getting {
            dependsOn(iosMain)
        }
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
