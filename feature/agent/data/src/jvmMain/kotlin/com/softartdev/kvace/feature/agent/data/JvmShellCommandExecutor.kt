package com.softartdev.kvace.feature.agent.data

import com.softartdev.kvace.feature.agent.domain.ShellCommandExecutor
import com.softartdev.kvace.feature.agent.domain.ShellCommandRequest
import com.softartdev.kvace.feature.agent.domain.ShellCommandResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

class JvmShellCommandExecutor(
    private val rootDirectory: Path = Paths.get(System.getProperty("user.dir")).toRealPath(),
    private val timeoutMillis: Long = DEFAULT_TIMEOUT_MILLIS,
    private val maxOutputBytes: Int = DEFAULT_MAX_OUTPUT_BYTES,
    private val maxOutputLines: Int = DEFAULT_MAX_OUTPUT_LINES,
) : ShellCommandExecutor {

    override suspend fun execute(request: ShellCommandRequest): ShellCommandResult = withContext(Dispatchers.IO) {
        ShellCommandPolicy.validate(request)?.let { rejected ->
            return@withContext rejected
        }
        val workingDirectory = resolveWorkingDirectory(request.workingDirectory)
            ?: return@withContext ShellCommandResult.Rejected("Working directory must stay inside the workspace root.")

        runCommand(request, workingDirectory)
    }

    private fun resolveWorkingDirectory(workingDirectory: String?): Path? {
        val candidate = when {
            workingDirectory.isNullOrBlank() -> rootDirectory
            else -> {
                val path = Paths.get(workingDirectory)
                if (path.isAbsolute) path else rootDirectory.resolve(path)
            }
        }.normalize()
        val realPath = runCatching {
            if (Files.exists(candidate)) candidate.toRealPath() else candidate
        }.getOrElse { return null }
        return realPath.takeIf { it.startsWith(rootDirectory) && Files.isDirectory(it) }
    }

    private fun runCommand(request: ShellCommandRequest, workingDirectory: Path): ShellCommandResult {
        val process = try {
            ProcessBuilder(listOf(request.command) + request.args)
                .directory(workingDirectory.toFile())
                .redirectErrorStream(true)
                .apply {
                    environment()["GIT_PAGER"] = "cat"
                    environment()["PAGER"] = "cat"
                    environment()["GIT_TERMINAL_PROMPT"] = "0"
                    environment()["GIT_OPTIONAL_LOCKS"] = "0"
                    environment().remove("GIT_EXTERNAL_DIFF")
                }
                .start()
        } catch (error: Throwable) {
            return ShellCommandResult.Failed(error.message ?: "Command failed to start.")
        }
        val output = LimitedProcessOutput(maxOutputBytes = maxOutputBytes, maxOutputLines = maxOutputLines)
        val reader = thread(name = "kvace-shell-output-reader", isDaemon = true) {
            runCatching { output.readFrom(process.inputStream) }
        }

        val finished = process.waitFor(timeoutMillis, TimeUnit.MILLISECONDS)
        if (!finished) {
            process.destroyForcibly()
            reader.join(1_000L)
            return ShellCommandResult.Failed(
                message = "Command timed out after ${timeoutMillis}ms.",
                output = output.value(),
            )
        }
        reader.join(1_000L)
        return ShellCommandResult.Success(
            exitCode = process.exitValue(),
            output = output.value(),
            truncated = output.truncated,
        )
    }

    private class LimitedProcessOutput(
        private val maxOutputBytes: Int,
        private val maxOutputLines: Int,
    ) {
        private val buffer = ByteArrayOutputStream()
        private var lineCount = 0
        var truncated: Boolean = false
            private set

        fun readFrom(inputStream: InputStream) {
            inputStream.use { stream ->
                val chunk = ByteArray(DEFAULT_READ_CHUNK_BYTES)
                while (true) {
                    val bytesRead = stream.read(chunk)
                    if (bytesRead < 0) return
                    append(chunk, bytesRead)
                }
            }
        }

        @Synchronized
        private fun append(bytes: ByteArray, bytesRead: Int) {
            if (truncated) return
            for (index in 0 until bytesRead) {
                if (buffer.size() >= maxOutputBytes || lineCount >= maxOutputLines) {
                    truncated = true
                    return
                }
                val byte = bytes[index]
                buffer.write(byte.toInt())
                if (byte == '\n'.code.toByte()) {
                    lineCount += 1
                    if (lineCount >= maxOutputLines && index < bytesRead - 1) {
                        truncated = true
                        return
                    }
                }
            }
        }

        @Synchronized
        fun value(): String = buffer.toByteArray().toString(Charsets.UTF_8).trimEnd()
    }

    private companion object {
        const val DEFAULT_TIMEOUT_MILLIS = 10_000L
        const val DEFAULT_MAX_OUTPUT_BYTES = 32 * 1024
        const val DEFAULT_MAX_OUTPUT_LINES = 200
        const val DEFAULT_READ_CHUNK_BYTES = 4096
    }
}
