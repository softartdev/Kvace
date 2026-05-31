package com.softartdev.kvace.feature.agent.data

import com.softartdev.kvace.feature.agent.domain.AgentConfigurationRepository
import com.softartdev.kvace.feature.agent.domain.AgentExecutionEvent
import com.softartdev.kvace.feature.agent.domain.AgentRequest
import com.softartdev.kvace.feature.agent.domain.AgentRuntime
import kotlinx.coroutines.flow.Flow

expect class KoogAgentRuntime(
    configurationRepository: AgentConfigurationRepository,
    onDeviceModelProvider: OnDeviceModelProvider,
) : AgentRuntime {
    override fun execute(request: AgentRequest): Flow<AgentExecutionEvent>
}
