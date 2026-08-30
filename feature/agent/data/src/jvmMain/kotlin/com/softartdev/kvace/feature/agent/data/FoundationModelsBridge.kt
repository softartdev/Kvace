package com.softartdev.kvace.feature.agent.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.PosixFilePermission
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

internal interface FoundationModelsBridge {
    suspend fun status(): FoundationModelsBridgeResponse
    suspend fun generate(prompt: String): FoundationModelsBridgeResponse
}

@Serializable
internal data class FoundationModelsBridgeResponse(
    val version: Int,
    val kind: String,
    val status: String? = null,
    val reason: String? = null,
    val content: String? = null,
    val errorCode: String? = null,
    val message: String? = null,
)

internal class ProcessFoundationModelsBridge(
    private val commandRunner: FoundationModelsCommandRunner = FoundationModelsProcessRunner(),
    private val json: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    },
) : FoundationModelsBridge {
    override suspend fun status(): FoundationModelsBridgeResponse = execute(
        request = FoundationModelsBridgeRequest(operation = STATUS_OPERATION),
        timeoutMillis = STATUS_TIMEOUT_MILLIS,
    )

    override suspend fun generate(prompt: String): FoundationModelsBridgeResponse = execute(
        request = FoundationModelsBridgeRequest(operation = GENERATE_OPERATION, prompt = prompt),
        timeoutMillis = GENERATION_TIMEOUT_MILLIS,
    )

    private suspend fun execute(
        request: FoundationModelsBridgeRequest,
        timeoutMillis: Long,
    ): FoundationModelsBridgeResponse {
        val result = commandRunner.run(
            input = json.encodeToString(request),
            timeoutMillis = timeoutMillis,
        )
        if (result.timedOut) {
            throw FoundationModelsBridgeException("Foundation Models bridge timed out after ${timeoutMillis}ms.")
        }
        if (result.stdoutTruncated || result.stderrTruncated) {
            throw FoundationModelsBridgeException("Foundation Models bridge output exceeded the allowed size.")
        }
        if (result.exitCode != 0) {
            val diagnostic = result.stderr.ifBlank { result.stdout }.ifBlank { "No diagnostic output." }
            throw FoundationModelsBridgeException(
                "Foundation Models bridge exited with code ${result.exitCode}: $diagnostic",
            )
        }
        val response = try {
            json.decodeFromString<FoundationModelsBridgeResponse>(result.stdout)
        } catch (error: Throwable) {
            throw FoundationModelsBridgeException("Foundation Models bridge returned invalid JSON.", error)
        }
        if (response.version != BRIDGE_PROTOCOL_VERSION) {
            throw FoundationModelsBridgeException(
                "Unsupported Foundation Models bridge protocol version ${response.version}.",
            )
        }
        return response
    }

    private companion object {
        const val STATUS_OPERATION = "status"
        const val GENERATE_OPERATION = "generate"
        const val STATUS_TIMEOUT_MILLIS = 5_000L
        const val GENERATION_TIMEOUT_MILLIS = 120_000L
    }
}

@Serializable
private data class FoundationModelsBridgeRequest(
    val version: Int = BRIDGE_PROTOCOL_VERSION,
    val operation: String,
    val prompt: String? = null,
)

internal fun interface FoundationModelsCommandRunner {
    suspend fun run(input: String, timeoutMillis: Long): FoundationModelsProcessResult
}

internal data class FoundationModelsProcessResult(
    val exitCode: Int,
    val stdout: String,
    val stderr: String,
    val timedOut: Boolean = false,
    val stdoutTruncated: Boolean = false,
    val stderrTruncated: Boolean = false,
)

internal class FoundationModelsProcessRunner(
    private val executableProvider: () -> Path = FoundationModelsHelperExecutable()::path,
    private val processFactory: (Path) -> Process = { executable ->
        ProcessBuilder(executable.toString()).start()
    },
) : FoundationModelsCommandRunner {

    override suspend fun run(input: String, timeoutMillis: Long): FoundationModelsProcessResult =
        withContext(Dispatchers.IO) {
            val process = try {
                processFactory(executableProvider())
            } catch (error: Throwable) {
                throw FoundationModelsBridgeException("Foundation Models bridge failed to start.", error)
            }
            val stdout = LimitedProcessOutput(MAX_STDOUT_BYTES)
            val stderr = LimitedProcessOutput(MAX_STDERR_BYTES)
            val stdoutReader = thread(name = "kvace-foundation-models-stdout", isDaemon = true) {
                stdout.readFrom(process.inputStream)
            }
            val stderrReader = thread(name = "kvace-foundation-models-stderr", isDaemon = true) {
                stderr.readFrom(process.errorStream)
            }

            try {
                process.outputStream.bufferedWriter().use { writer ->
                    writer.write(input)
                    writer.newLine()
                }
                val finished = runInterruptible {
                    process.waitFor(timeoutMillis, TimeUnit.MILLISECONDS)
                }
                if (!finished) {
                    process.destroyForcibly()
                }
                stdoutReader.join(READER_JOIN_TIMEOUT_MILLIS)
                stderrReader.join(READER_JOIN_TIMEOUT_MILLIS)
                FoundationModelsProcessResult(
                    exitCode = if (finished) process.exitValue() else PROCESS_TIMEOUT_EXIT_CODE,
                    stdout = stdout.value(),
                    stderr = stderr.value(),
                    timedOut = !finished,
                    stdoutTruncated = stdout.truncated,
                    stderrTruncated = stderr.truncated,
                )
            } finally {
                if (process.isAlive) {
                    process.destroyForcibly()
                }
                stdoutReader.join(READER_JOIN_TIMEOUT_MILLIS)
                stderrReader.join(READER_JOIN_TIMEOUT_MILLIS)
            }
        }

    private class LimitedProcessOutput(private val maxBytes: Int) {
        private val buffer = ByteArrayOutputStream()

        var truncated: Boolean = false
            private set

        fun readFrom(inputStream: InputStream) {
            runCatching {
                inputStream.use { stream ->
                    val chunk = ByteArray(READ_CHUNK_BYTES)
                    while (true) {
                        val bytesRead = stream.read(chunk)
                        if (bytesRead < 0) return
                        append(chunk, bytesRead)
                    }
                }
            }
        }

        @Synchronized
        private fun append(bytes: ByteArray, bytesRead: Int) {
            if (truncated) return
            val remaining = maxBytes - buffer.size()
            if (remaining <= 0) {
                truncated = true
                return
            }
            val bytesToWrite = minOf(bytesRead, remaining)
            buffer.write(bytes, 0, bytesToWrite)
            if (bytesToWrite < bytesRead) {
                truncated = true
            }
        }

        @Synchronized
        fun value(): String = buffer.toByteArray().toString(Charsets.UTF_8).trim()
    }

    private companion object {
        const val MAX_STDOUT_BYTES = 1024 * 1024
        const val MAX_STDERR_BYTES = 64 * 1024
        const val READ_CHUNK_BYTES = 4096
        const val READER_JOIN_TIMEOUT_MILLIS = 1_000L
        const val PROCESS_TIMEOUT_EXIT_CODE = -1
    }
}

internal class FoundationModelsHelperExecutable(
    private val appResourcesDirectory: Path? = System.getProperty(APP_RESOURCES_PROPERTY)
        ?.takeIf { it.isNotBlank() }
        ?.let(Paths::get),
) {
    private val extractedPath: Path by lazy(::extract)

    fun path(): Path = extractedPath

    private fun extract(): Path {
        val resourcesDirectory = appResourcesDirectory
            ?: throw FoundationModelsBridgeException("Desktop application resources are unavailable.")
        val bundledExecutable = resourcesDirectory.resolve(BUNDLED_EXECUTABLE_PATH)
        if (!Files.isRegularFile(bundledExecutable)) {
            throw FoundationModelsBridgeException("Foundation Models bridge is missing from application resources.")
        }

        val temporaryDirectory = Files.createTempDirectory(TEMP_DIRECTORY_PREFIX)
        Files.setPosixFilePermissions(temporaryDirectory, OWNER_DIRECTORY_PERMISSIONS)
        val executable = temporaryDirectory.resolve(EXECUTABLE_NAME)
        Files.copy(bundledExecutable, executable, StandardCopyOption.REPLACE_EXISTING)
        Files.setPosixFilePermissions(executable, OWNER_EXECUTABLE_PERMISSIONS)
        temporaryDirectory.toFile().deleteOnExit()
        executable.toFile().deleteOnExit()
        return executable
    }

    private companion object {
        const val APP_RESOURCES_PROPERTY = "compose.application.resources.dir"
        const val EXECUTABLE_NAME = "KvaceFoundationModelsBridge"
        const val BUNDLED_EXECUTABLE_PATH = "foundation-models/$EXECUTABLE_NAME"
        const val TEMP_DIRECTORY_PREFIX = "kvace-foundation-models-"

        val OWNER_DIRECTORY_PERMISSIONS = setOf(
            PosixFilePermission.OWNER_READ,
            PosixFilePermission.OWNER_WRITE,
            PosixFilePermission.OWNER_EXECUTE,
        )
        val OWNER_EXECUTABLE_PERMISSIONS = setOf(
            PosixFilePermission.OWNER_READ,
            PosixFilePermission.OWNER_WRITE,
            PosixFilePermission.OWNER_EXECUTE,
        )
    }
}

internal class FoundationModelsBridgeException(message: String, cause: Throwable? = null) :
    RuntimeException(message, cause)

private const val BRIDGE_PROTOCOL_VERSION = 1
