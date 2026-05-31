import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

compose.desktop {
    application {
        mainClass = "com.softartdev.kvace.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "com.softartdev.kvace"
            packageVersion = "1.0.0"

            macOS {
                iconFile.set(project.file("src/main/resources/icons/kvace.icns"))
            }
            windows {
                iconFile.set(project.file("src/main/resources/icons/kvace.ico"))
            }
            linux {
                iconFile.set(project.file("src/main/composeResources/drawable/kvace_window_icon.png"))
            }
        }
    }
}

compose.resources {
    packageOfResClass = "kvace.app.desktop.generated.resources"
}

dependencies {
    implementation(projects.app.shared)
    implementation(compose.desktop.currentOs)
    implementation(libs.compose.components.resources)
    implementation(libs.kotlinx.coroutinesSwing)
    implementation(libs.compose.uiToolingPreview)
    implementation(libs.kronos)
}
