package com.softartdev.kvace.feature.agent.data

import ai.koog.agents.core.tools.ToolDescriptor
import ai.koog.prompt.Prompt
import ai.koog.prompt.dsl.ModerationResult
import ai.koog.prompt.executor.clients.LLMClient
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.message.Message
import ai.koog.prompt.message.ResponseMetaInfo
import ai.koog.prompt.streaming.StreamFrame
import ai.koog.utils.time.KoogClock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

internal val ON_DEVICE_LLM_PROVIDER = LLMProvider("on-device", "On-device")

internal fun onDeviceLLModel(modelName: String): LLModel = LLModel(
    provider = ON_DEVICE_LLM_PROVIDER,
    id = modelName,
    capabilities = emptyList(),
    contextLength = ON_DEVICE_CONTEXT_LENGTH,
)

internal class OnDeviceLLMClient(
    private val onDeviceModelProvider: OnDeviceModelProvider,
) : LLMClient() {

    override suspend fun execute(
        prompt: Prompt,
        model: LLModel,
        tools: List<ToolDescriptor>,
    ): Message.Assistant {
        require(model.provider == ON_DEVICE_LLM_PROVIDER) {
            "On-device client cannot execute provider ${model.provider.display}."
        }
        if (tools.isNotEmpty()) {
            throw UnsupportedOperationException("On-device AI does not support tool execution.")
        }
        val generatedText = onDeviceModelProvider.generateContent(prompt.toPlainTextPrompt())
        return Message.Assistant(
            content = generatedText,
            metaInfo = ResponseMetaInfo.create(
                clock = KoogClock.System,
                modelId = model.id,
            ),
            finishReason = "stop",
        )
    }

    override fun executeStreaming(
        prompt: Prompt,
        model: LLModel,
        tools: List<ToolDescriptor>,
    ): Flow<StreamFrame> = flow {
        val response = execute(prompt, model, tools)
        val text = response.textContent()
        emit(StreamFrame.TextComplete(text))
        emit(StreamFrame.End(response.finishReason, response.metaInfo))
    }

    override suspend fun moderate(prompt: Prompt, model: LLModel): ModerationResult =
        throw UnsupportedOperationException("On-device AI does not support moderation.")

    override suspend fun models(): List<LLModel> = when (onDeviceModelProvider.isAvailable) {
        true -> listOf(onDeviceLLModel(onDeviceModelProvider.modelName))
        else -> emptyList()
    }

    override fun llmProvider(): LLMProvider = ON_DEVICE_LLM_PROVIDER

    override val clientName: String = "OnDeviceLLMClient"

    override fun close() = Unit

    private fun Prompt.toPlainTextPrompt(): String = messages.mapNotNull { message ->
        val text = message.textContent().trim()
        if (text.isBlank()) null else "${message.role.name}: $text"
    }.joinToString(separator = "\n\n")
        .ifBlank { throw OnDeviceModelException.GenerationFailed("On-device AI prompt is empty.") }
}

private const val ON_DEVICE_CONTEXT_LENGTH = 4096L
