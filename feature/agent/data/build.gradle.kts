import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.KotlinHierarchyTemplate

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.kotlinSerialization)
}

configurations.configureEach {
    exclude(group = "ai.koog", module = "serialization-jackson")
}

kotlin {
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    applyHierarchyTemplate(KotlinHierarchyTemplate.default) {
        common {
            group("koog") {
                withCompilations { it.target.name == "android" }
                withIos()
                withJvm()
            }
        }
    }
    jvm()
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs { browser() }
    iosArm64()
    iosSimulatorArm64()
    android {
        namespace = "com.softartdev.kvace.feature.agent.data"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        withJava()
        compilerOptions { jvmTarget = JvmTarget.JVM_11 }
        withHostTest {}
    }
    sourceSets {
        val koogMain by getting {
            dependencies {
                implementation(libs.koog.prompt.executor.model)
                implementation(libs.koog.prompt.executor.ollama.client)
                implementation(libs.koog.agents.tools)
                implementation(libs.koog.http.client.ktor)
                implementation(libs.ktor.client.cio)
            }
        }
        commonMain.dependencies {
            implementation(projects.core.data)
            implementation(projects.feature.agent.domain)
            implementation(libs.kermit)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(project.dependencies.platform(libs.ktor.bom))
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.logging)
        }
        androidMain.dependencies {
            implementation(libs.androidx.core.ktx)
            implementation(libs.mlkit.genai.prompt)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
    compilerOptions {
        freeCompilerArgs.addAll("-Xexpect-actual-classes", "-Xexplicit-backing-fields")
    }
}
