plugins {
    `kotlin-dsl`
}

group = "com.softartdev.kvace.buildlogic"

dependencies {
    implementation(libs.android.gradlePlugin)
    implementation(libs.compose.gradlePlugin)
    implementation(libs.kotlin.composeCompilerGradlePlugin)
    implementation(libs.kotlin.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("kvaceKmpLibrary") {
            id = "kvace.kmp.library"
            implementationClass = "KvaceKmpLibraryPlugin"
        }
        register("kvaceComposeKmpLibrary") {
            id = "kvace.kmp.compose-library"
            implementationClass = "KvaceComposeKmpLibraryPlugin"
        }
        register("kvaceDesktopFoundationModels") {
            id = "kvace.desktop.foundation-models"
            implementationClass = "FoundationModelsBridgePlugin"
        }
    }
}
