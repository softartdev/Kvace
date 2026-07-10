package com.softartdev.kvace.feature.agent.data

import com.softartdev.kvace.feature.agent.domain.ShellCommandRequest
import com.softartdev.kvace.feature.agent.domain.ShellCommandResult
import kotlinx.coroutines.test.runTest
import java.nio.file.Files
import kotlin.io.path.name
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JvmShellCommandExecutorTest {

    @Test
    fun pwdRunsInWorkspaceRoot() = runTest {
        val root = Files.createTempDirectory("kvace-shell-root").toRealPath()
        val executor = JvmShellCommandExecutor(rootDirectory = root)

        val result = executor.execute(ShellCommandRequest(command = "pwd"))

        assertTrue(result is ShellCommandResult.Success)
        assertEquals(root.toString(), result.output.trim())
    }

    @Test
    fun rejectedCommandDoesNotRun() = runTest {
        val root = Files.createTempDirectory("kvace-shell-root").toRealPath()
        val executor = JvmShellCommandExecutor(rootDirectory = root)

        val result = executor.execute(ShellCommandRequest(command = "rm", args = listOf("file.txt")))

        assertTrue(result is ShellCommandResult.Rejected)
    }

    @Test
    fun capsLargeOutput() = runTest {
        val root = Files.createTempDirectory("kvace-shell-root").toRealPath()
        Files.writeString(root.resolve("large.txt"), (1..20).joinToString(separator = "\n") { "line-$it" })
        val executor = JvmShellCommandExecutor(
            rootDirectory = root,
            maxOutputBytes = 1_024,
            maxOutputLines = 3,
        )

        val result = executor.execute(ShellCommandRequest(command = "cat", args = listOf("large.txt")))

        assertTrue(result is ShellCommandResult.Success)
        assertTrue(result.truncated)
        assertEquals(listOf("line-1", "line-2", "line-3"), result.output.lines())
    }

    @Test
    fun timesOutLongRunningCommand() = runTest {
        val root = Files.createTempDirectory("kvace-shell-root").toRealPath()
        val file = root.resolve("follow.txt")
        Files.writeString(file, "waiting\n")
        val executor = JvmShellCommandExecutor(
            rootDirectory = root,
            timeoutMillis = 100L,
        )

        val result = executor.execute(ShellCommandRequest(command = "tail", args = listOf("-f", file.name)))

        assertTrue(result is ShellCommandResult.Failed)
        assertTrue(result.message.contains("timed out"))
    }
}
