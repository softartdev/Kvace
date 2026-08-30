plugins {
    id("kvace.kmp.library")
}

kotlin {
    android {
        namespace = "com.softartdev.kvace.feature.chat.domain"
    }
    sourceSets {
        commonMain.dependencies {
            api(projects.feature.agent.domain)
            implementation(libs.kotlinx.coroutines.core)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
