plugins {
    id("kvace.kmp.library")
}

kotlin {
    android {
        namespace = "com.softartdev.kvace.core.data"
    }
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.domain)
            implementation(libs.multiplatform.settings)
        }
        androidMain.dependencies {
        }
        iosMain.dependencies {
        }
        jvmMain.dependencies {
        }
        wasmJsMain.dependencies {
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}
