package com.softartdev.kvace.feature.agent.data

import com.softartdev.kvace.feature.agent.domain.AgentConfigurationRepository
import com.softartdev.kvace.feature.agent.domain.AgentExecutionEvent
import com.softartdev.kvace.feature.agent.domain.AgentRequest
import com.softartdev.kvace.feature.agent.domain.AgentRuntime
import com.softartdev.kvace.feature.agent.domain.HarnessConfigurationRepository
import com.softartdev.kvace.feature.agent.domain.ProviderCredentialRepository
import com.softartdev.kvace.feature.agent.domain.ShellCommandExecutor
import kotlinx.coroutines.flow.Flow

expect class KoogAgentRuntime(
    configurationRepository: AgentConfigurationRepository,
    harnessConfigurationRepository: HarnessConfigurationRepository,
    onDeviceModelProvider: OnDeviceModelProvider,
    shellCommandExecutor: ShellCommandExecutor,
    credentialRepository: ProviderCredentialRepository,
) : AgentRuntime {

    override fun execute(request: AgentRequest): Flow<AgentExecutionEvent>
}
