plugins {
    id("kvace.kmp.library")
}

kotlin {
    android {
        namespace = "com.softartdev.kvace.feature.settings.data"
    }
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.data)
            implementation(projects.feature.settings.domain)
            implementation(libs.kotlinx.coroutines.core)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
