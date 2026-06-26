plugins {
    id("kvace.kmp.compose-library")
}

kotlin {
    android {
        namespace = "com.softartdev.kvace.feature.settings.ui"
    }
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.ui)
            implementation(projects.feature.settings.domain)
            implementation(projects.feature.settings.presentation)
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            api(libs.aboutlibraries.compose.core)
            api(libs.aboutlibraries.compose.m3)
            implementation(libs.compose.adaptive)
            implementation(libs.compose.adaptive.layout)
            implementation(libs.compose.adaptive.navigation)
            implementation(libs.material.theme.prefs)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        androidMain.dependencies {
            api(libs.aboutlibraries.compose.core)
            api(libs.aboutlibraries.compose.m3)
        }
    }
}

dependencies {
    androidRuntimeClasspath(libs.compose.uiTooling)
}
