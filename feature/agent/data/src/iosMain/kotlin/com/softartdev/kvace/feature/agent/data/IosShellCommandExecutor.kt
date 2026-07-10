package com.softartdev.kvace.feature.agent.data.com.softartdev.kvace.feature.agent.data

import com.softartdev.kvace.feature.agent.data.UnavailableShellCommandExecutor

class IosShellCommandExecutor : UnavailableShellCommandExecutor(
    reason = "Shell command execution is unavailable on iOS."
)
