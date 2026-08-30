import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import org.gradle.work.DisableCachingByDefault
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.PosixFilePermission
import javax.inject.Inject

@DisableCachingByDefault(because = "The output depends on the installed Xcode toolchain and macOS SDK")
abstract class CompileFoundationModelsBridgeTask : DefaultTask() {
    @get:Inject
    abstract val execOperations: ExecOperations

    @get:InputFile
    abstract val sourceFile: RegularFileProperty

    @get:Input
    abstract val swiftTarget: Property<String>

    @get:OutputFile
    abstract val executableFile: RegularFileProperty

    @TaskAction
    fun compile() {
        val output = executableFile.get().asFile
        output.parentFile.mkdirs()
        execOperations.exec {
            commandLine(
                "xcrun",
                "swiftc",
                "-parse-as-library",
                "-O",
                "-target",
                swiftTarget.get(),
                sourceFile.get().asFile.absolutePath,
                "-o",
                output.absolutePath,
            )
        }
    }
}

@DisableCachingByDefault(because = "Code signing depends on the local macOS keychain")
abstract class PrepareFoundationModelsBridgeTask : DefaultTask() {
    @get:Inject
    abstract val execOperations: ExecOperations

    @get:InputFile
    abstract val compiledExecutable: RegularFileProperty

    @get:OutputFile
    abstract val stagedExecutable: RegularFileProperty

    @get:Input
    abstract val signingEnabled: Property<Boolean>

    @get:Input
    @get:Optional
    abstract val signingIdentity: Property<String>

    @get:Input
    @get:Optional
    abstract val signingKeychain: Property<String>

    @TaskAction
    fun prepare() {
        val output = stagedExecutable.get().asFile
        output.parentFile.mkdirs()
        Files.copy(
            compiledExecutable.get().asFile.toPath(),
            output.toPath(),
            StandardCopyOption.REPLACE_EXISTING,
        )
        Files.setPosixFilePermissions(
            output.toPath(),
            setOf(
                PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE,
                PosixFilePermission.OWNER_EXECUTE,
            ),
        )

        val identity = when {
            signingEnabled.get() -> signingIdentity.orNull
                ?: throw GradleException(
                    "compose.desktop.mac.signing.identity is required when macOS signing is enabled.",
                )
            else -> "-"
        }
        val arguments = mutableListOf(
            "codesign",
            "--force",
            "--sign",
            identity,
            "--options",
            "runtime",
        )
        signingKeychain.orNull?.let { keychain ->
            arguments += listOf("--keychain", keychain)
        }
        if (signingEnabled.get()) {
            arguments += "--timestamp"
        }
        arguments += output.absolutePath
        execOperations.exec { commandLine(arguments) }
    }
}

@DisableCachingByDefault(because = "The result reports live Apple Foundation Models availability")
abstract class VerifyFoundationModelsBridgeTask : DefaultTask() {
    @get:Inject
    abstract val execOperations: ExecOperations

    @get:InputFile
    abstract val executableFile: RegularFileProperty

    @TaskAction
    fun verify() {
        val stdout = ByteArrayOutputStream()
        execOperations.exec {
            commandLine(executableFile.get().asFile.absolutePath)
            standardInput = ByteArrayInputStream("""{"version":1,"operation":"status"}""".toByteArray())
            standardOutput = stdout
        }
        val response = stdout.toString(Charsets.UTF_8)
        check(Regex("\"version\"\\s*:\\s*1").containsMatchIn(response)) {
            "Foundation Models bridge returned an unsupported protocol response: $response"
        }
        check(Regex("\"kind\"\\s*:\\s*\"status\"").containsMatchIn(response)) {
            "Foundation Models bridge did not return a status response: $response"
        }
    }
}
