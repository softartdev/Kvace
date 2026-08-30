import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.Sync

class FoundationModelsBridgePlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        val isMacOsArm64 = providers.systemProperty("os.name")
            .zip(providers.systemProperty("os.arch")) { osName, architecture ->
                val isMacOs = osName.equals("Mac OS X", ignoreCase = true) ||
                    osName.equals("macOS", ignoreCase = true)
                val isArm64 = architecture.equals("aarch64", ignoreCase = true) ||
                    architecture.equals("arm64", ignoreCase = true)
                isMacOs && isArm64
            }
        val source = layout.projectDirectory.file("src/native/macos/FoundationModelsBridge.swift")
        val compiledExecutableFile = layout.buildDirectory.file(
            "native/foundation-models/KvaceFoundationModelsBridge",
        )
        val appResourcesRoot = layout.buildDirectory.dir("generated/appResources")
        val stagedExecutableFile = appResourcesRoot.map { directory ->
            directory.file("macos-arm64/foundation-models/KvaceFoundationModelsBridge")
        }
        val signingEnabledProvider = providers.gradleProperty("compose.desktop.mac.sign")
            .map(String::toBoolean)
            .orElse(false)

        val compileBridge = tasks.register(
            "compileMacOsFoundationModelsBridge",
            CompileFoundationModelsBridgeTask::class.java,
        ) {
            group = "build"
            description = "Compiles the Apple Foundation Models helper for macOS ARM64."
            enabled = isMacOsArm64.get()
            sourceFile.set(source)
            swiftTarget.set("arm64-apple-macosx26.0")
            executableFile.set(compiledExecutableFile)
        }

        val prepareBridge = tasks.register(
            "prepareMacOsFoundationModelsBridge",
            PrepareFoundationModelsBridgeTask::class.java,
        ) {
            group = "build"
            description = "Signs and stages the Apple Foundation Models helper as a Desktop app resource."
            enabled = isMacOsArm64.get()
            dependsOn(compileBridge)
            compiledExecutable.set(compiledExecutableFile)
            stagedExecutable.set(stagedExecutableFile)
            signingEnabled.set(signingEnabledProvider)
            signingIdentity.set(providers.gradleProperty("compose.desktop.mac.signing.identity"))
            signingKeychain.set(providers.gradleProperty("compose.desktop.mac.signing.keychain"))
        }

        tasks.register(
            "verifyMacOsFoundationModelsBridge",
            VerifyFoundationModelsBridgeTask::class.java,
        ) {
            group = "verification"
            description = "Runs the macOS Foundation Models helper and validates its status protocol."
            enabled = isMacOsArm64.get()
            dependsOn(prepareBridge)
            executableFile.set(stagedExecutableFile)
        }

        tasks.withType(Sync::class.java)
            .matching { it.name == "prepareAppResources" }
            .configureEach { dependsOn(prepareBridge) }

        tasks.withType(JavaExec::class.java)
            .matching { it.name == "hotRun" }
            .configureEach {
                dependsOn("prepareAppResources")
                systemProperty(
                    "compose.application.resources.dir",
                    layout.buildDirectory.dir("compose/tmp/prepareAppResources").get().asFile.absolutePath,
                )
            }
    }
}
