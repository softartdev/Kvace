package com.softartdev.kvace.feature.agent.data

import ai.koog.agents.core.tools.SimpleTool
import ai.koog.prompt.message.MessagePart
import ai.koog.serialization.KSerializerTypeToken
import ai.koog.serialization.annotations.InternalKoogSerializationApi
import com.softartdev.kvace.feature.agent.domain.ShellCommandExecutor
import com.softartdev.kvace.feature.agent.domain.ShellCommandRequest
import com.softartdev.kvace.feature.agent.domain.ShellCommandResult
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

internal const val SHELL_COMMAND_TOOL_NAME = "shell_command"

@Serializable
internal data class ShellCommandToolArgs(
    val command: String,
    val args: List<String> = emptyList(),
    val workingDirectory: String? = null,
)

@OptIn(InternalKoogSerializationApi::class)
internal class ShellCommandTool(
    private val executor: ShellCommandExecutor,
) : SimpleTool<ShellCommandToolArgs>(
    argsType = KSerializerTypeToken(ShellCommandToolArgs.serializer()),
    name = SHELL_COMMAND_TOOL_NAME,
    description = "Run one read-only allowlisted shell command in the local workspace. " +
        "Use this only for inspection commands such as pwd, ls, cat, head, tail, sed -n, rg, and read-only git commands.",
) {

    suspend fun executeForResult(args: ShellCommandToolArgs): ShellCommandResult =
        executor.execute(
            ShellCommandRequest(
                command = args.command,
                args = args.args,
                workingDirectory = args.workingDirectory,
            )
        )

    override suspend fun execute(args: ShellCommandToolArgs): String =
        executeForResult(args).toToolOutput()
}

internal fun MessagePart.Tool.Call.shellCommandArgsOrNull(): ShellCommandToolArgs? =
    runCatching { shellCommandToolJson.decodeFromString<ShellCommandToolArgs>(args) }.getOrNull()

internal fun ShellCommandToolArgs.displayText(): String =
    buildString {
        append(SHELL_COMMAND_TOOL_NAME)
        append('\n')
        append(command)
        if (args.isNotEmpty()) {
            append(' ')
            append(args.joinToString(separator = " "))
        }
        workingDirectory?.takeIf { it.isNotBlank() }?.let { directory ->
            append("\nworkingDirectory: ")
            append(directory)
        }
    }

internal fun ShellCommandResult.toToolOutput(): String = when (this) {
    is ShellCommandResult.Success -> buildString {
        append("shell_command completed with exit code ")
        append(exitCode)
        append(".")
        if (truncated) append("\nOutput was truncated.")
        append("\n\n")
        append(output.ifBlank { "(no output)" })
    }
    is ShellCommandResult.Rejected -> "shell_command rejected: $reason"
    is ShellCommandResult.Unsupported -> "shell_command unsupported: $reason"
    is ShellCommandResult.Failed -> buildString {
        append("shell_command failed: ")
        append(message)
        output?.takeIf { it.isNotBlank() }?.let { value ->
            append("\n\n")
            append(value)
        }
    }
}

private val shellCommandToolJson = Json {
    ignoreUnknownKeys = true
}
