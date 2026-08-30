import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeHotReload)
    alias(libs.plugins.composeCompiler)
    id("kvace.desktop.foundation-models")
}

val appResourcesRoot = layout.buildDirectory.dir("generated/appResources")

compose.desktop {
    application {
        mainClass = "com.softartdev.kvace.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "com.softartdev.kvace"
            packageVersion = "1.0.0"
            modules("java.sql")
            appResourcesRootDir.set(appResourcesRoot)

            macOS {
                iconFile.set(project.file("src/main/resources/icons/kvace.icns"))
            }
            windows {
                iconFile.set(project.file("src/main/resources/icons/kvace.ico"))
            }
            linux {
                iconFile.set(project.file("../../core/ui/src/commonMain/composeResources/drawable/kvace_window_icon.png"))
            }
        }
    }
}

dependencies {
    implementation(projects.core.ui)
    implementation(projects.app.shared)
    implementation(compose.desktop.currentOs)
    implementation(libs.compose.components.resources)
    implementation(libs.kotlinx.coroutinesSwing)
    implementation(libs.compose.uiToolingPreview)
    implementation(libs.kronos)
}
