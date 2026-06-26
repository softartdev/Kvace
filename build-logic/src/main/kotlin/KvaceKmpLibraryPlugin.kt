import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

class KvaceKmpLibraryPlugin : Plugin<Project> {

    override fun apply(target: Project) = with(target) {
        pluginManager.apply("org.jetbrains.kotlin.multiplatform")
        pluginManager.apply("com.android.kotlin.multiplatform.library")

        val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")
        val compileSdk = libs.findVersion("android-compileSdk").get().requiredVersion.toInt()
        val minSdk = libs.findVersion("android-minSdk").get().requiredVersion.toInt()

        extensions.configure<KotlinMultiplatformExtension> {
            compilerOptions {
                languageVersion.set(KotlinVersion.KOTLIN_2_4)
                freeCompilerArgs.add("-Xexplicit-backing-fields")
            }
            jvm()
            @OptIn(ExperimentalWasmDsl::class)
            wasmJs { browser() }
            iosArm64()
            iosSimulatorArm64()
            targets.withType<KotlinMultiplatformAndroidLibraryTarget>().configureEach {
                this.compileSdk = compileSdk
                this.minSdk = minSdk
                compilerOptions {
                    jvmTarget.set(JvmTarget.JVM_11)
                }
                withHostTest {}
            }
        }
        tasks.withType<KotlinJvmCompile>().configureEach {
            compilerOptions.jvmTarget.set(JvmTarget.JVM_11)
        }
    }
}
