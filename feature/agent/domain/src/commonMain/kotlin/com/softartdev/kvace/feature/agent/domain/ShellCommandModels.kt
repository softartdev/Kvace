package com.softartdev.kvace.feature.agent.domain

data class ShellCommandRequest(
    val command: String,
    val args: List<String> = emptyList(),
    val workingDirectory: String? = null,
)

sealed interface ShellCommandResult {
    data class Success(
        val exitCode: Int,
        val output: String,
        val truncated: Boolean = false,
    ) : ShellCommandResult

    data class Rejected(val reason: String) : ShellCommandResult
    data class Unsupported(val reason: String) : ShellCommandResult
    data class Failed(val message: String, val output: String? = null) : ShellCommandResult
}

interface ShellCommandExecutor {
    suspend fun execute(request: ShellCommandRequest): ShellCommandResult
}
