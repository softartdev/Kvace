plugins {
    id("kvace.kmp.library")
    alias(libs.plugins.sqldelight)
}

kotlin {
    android {
        namespace = "com.softartdev.kvace.feature.chat.data"
    }
    sourceSets {
        commonMain.dependencies {
            implementation(projects.feature.chat.domain)
            implementation(libs.kermit)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.sqldelight.async.extensions)
            implementation(libs.sqldelight.coroutines.extensions)
            implementation(libs.sqldelight.runtime)
        }
        androidMain.dependencies {
            implementation(libs.sqldelight.android.driver)
        }
        iosMain.dependencies {
            implementation(libs.sqldelight.native.driver)
        }
        jvmMain.dependencies {
            implementation(libs.appdirs)
            implementation(libs.sqldelight.sqlite.driver)
        }
        wasmJsMain.dependencies {
            implementation(libs.sqldelight.web.worker.driver)
            implementation(npm("@cashapp/sqldelight-sqljs-worker", libs.versions.sqldelight.get()))
            implementation(npm("sql.js", "1.8.0"))
            implementation(devNpm("copy-webpack-plugin", "9.1.0"))
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
        jvmTest.dependencies {
            implementation(libs.sqldelight.sqlite.driver)
        }
    }
}

sqldelight {
    databases {
        register("ChatDatabase") {
            packageName.set("com.softartdev.kvace.feature.chat.data.local")
            generateAsync.set(true)
        }
    }
}
