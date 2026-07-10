package com.softartdev.kvace.feature.agent.data

import com.softartdev.kvace.feature.agent.domain.ShellCommandExecutor
import com.softartdev.kvace.feature.agent.domain.ShellCommandRequest
import com.softartdev.kvace.feature.agent.domain.ShellCommandResult

open class UnavailableShellCommandExecutor(
    private val reason: String = "Shell command execution is unavailable on this platform.",
) : ShellCommandExecutor {

    override suspend fun execute(request: ShellCommandRequest): ShellCommandResult =
        ShellCommandResult.Unsupported(reason)
}
