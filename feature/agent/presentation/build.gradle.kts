plugins {
    id("kvace.kmp.library")
}

kotlin {
    android {
        namespace = "com.softartdev.kvace.feature.agent.presentation"
    }
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.domain)
            implementation(libs.kermit)
            api(projects.feature.agent.domain)
            api(libs.androidx.lifecycle.viewmodel)
            api(libs.kotlinx.coroutines.core)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
