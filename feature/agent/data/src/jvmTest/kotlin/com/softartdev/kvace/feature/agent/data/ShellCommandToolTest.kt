package com.softartdev.kvace.feature.agent.data

import ai.koog.prompt.message.MessagePart
import com.softartdev.kvace.feature.agent.domain.ShellCommandExecutor
import com.softartdev.kvace.feature.agent.domain.ShellCommandRequest
import com.softartdev.kvace.feature.agent.domain.ShellCommandResult
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ShellCommandToolTest {

    @Test
    fun decodesKoogToolCallArguments() {
        val call = MessagePart.Tool.Call(
            id = "call-1",
            tool = SHELL_COMMAND_TOOL_NAME,
            args = buildJsonObject {
                put("command", "git")
                putJsonArray("args") {
                    add("status")
                    add("--short")
                }
                put("workingDirectory", ".")
            },
        )

        val args = call.shellCommandArgsOrNull()

        assertEquals(ShellCommandToolArgs(command = "git", args = listOf("status", "--short"), workingDirectory = "."), args)
    }

    @Test
    fun executesDomainShellRequestAndFormatsSuccess() = runTest {
        val executor = CapturingShellCommandExecutor(
            ShellCommandResult.Success(exitCode = 0, output = "clean"),
        )
        val tool = ShellCommandTool(executor)

        val output = tool.execute(
            ShellCommandToolArgs(
                command = "git",
                args = listOf("status", "--short"),
                workingDirectory = ".",
            ),
        )

        assertEquals(ShellCommandRequest("git", listOf("status", "--short"), "."), executor.request)
        assertTrue(output.contains("exit code 0"))
        assertTrue(output.contains("clean"))
    }

    @Test
    fun formatsUnsupportedResultForModel() = runTest {
        val tool = ShellCommandTool(
            CapturingShellCommandExecutor(
                ShellCommandResult.Unsupported("Android shell execution requires a configured Termux bridge."),
            )
        )

        val output = tool.execute(ShellCommandToolArgs(command = "pwd"))

        assertTrue(output.contains("unsupported"))
        assertTrue(output.contains("Termux"))
    }
}

private class CapturingShellCommandExecutor(
    private val result: ShellCommandResult,
) : ShellCommandExecutor {
    lateinit var request: ShellCommandRequest

    override suspend fun execute(request: ShellCommandRequest): ShellCommandResult {
        this.request = request
        return result
    }
}
