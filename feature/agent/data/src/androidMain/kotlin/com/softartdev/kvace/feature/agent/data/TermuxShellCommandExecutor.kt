package com.softartdev.kvace.feature.agent.data

import com.softartdev.kvace.feature.agent.domain.ShellCommandExecutor
import com.softartdev.kvace.feature.agent.domain.ShellCommandRequest
import com.softartdev.kvace.feature.agent.domain.ShellCommandResult

class TermuxShellCommandExecutor : ShellCommandExecutor {

    override suspend fun execute(request: ShellCommandRequest): ShellCommandResult =
        ShellCommandResult.Unsupported("Android shell execution requires a configured Termux bridge.")
}
