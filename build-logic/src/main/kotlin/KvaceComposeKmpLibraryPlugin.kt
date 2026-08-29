import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import com.android.build.api.dsl.KotlinMultiplatformAndroidHostTestCompilation
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class KvaceComposeKmpLibraryPlugin : Plugin<Project> {

    override fun apply(target: Project) = with(target) {
        pluginManager.apply("kvace.kmp.library")
        pluginManager.apply("org.jetbrains.compose")
        pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

        extensions.configure<KotlinMultiplatformExtension> {
            @OptIn(ExperimentalWasmDsl::class)
            wasmJs {
                binaries.executable()
            }
            targets.withType<KotlinMultiplatformAndroidLibraryTarget>().configureEach {
                androidResources {
                    enable = true
                }
                compilations.withType<KotlinMultiplatformAndroidHostTestCompilation>().configureEach {
                    isIncludeAndroidResources = true
                }
            }
        }
    }
}
