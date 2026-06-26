plugins {
    id("kvace.kmp.library")
}

kotlin {
    android {
        namespace = "com.softartdev.kvace.feature.settings.domain"
    }
    sourceSets {
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}
