package com.softartdev.kvace.app.di

import com.softartdev.kvace.OnDevicePromptApi
import com.softartdev.kvace.feature.agent.data.APPLE_ON_DEVICE_MODEL_LABEL
import com.softartdev.kvace.feature.agent.data.OnDeviceModelException
import com.softartdev.kvace.feature.agent.data.OnDeviceModelProvider

object AppleOnDevicePromptApiRegistry {
    var promptApi: OnDevicePromptApi? = null
}

internal class AppleOnDeviceModelProvider : OnDeviceModelProvider {
    override val modelName: String = APPLE_ON_DEVICE_MODEL_LABEL
    override val isAvailable: Boolean
        get() = AppleOnDevicePromptApiRegistry.promptApi != null

    override suspend fun generateContent(prompt: String): String {
        val promptApi = AppleOnDevicePromptApiRegistry.promptApi
            ?: throw OnDeviceModelException.Unavailable(
                "$APPLE_ON_DEVICE_MODEL_LABEL is unavailable on this Apple platform or OS version.",
            )

        val response = runCatching { promptApi.generateContent(prompt) }
            .getOrElse { error ->
                throw OnDeviceModelException.GenerationFailed(
                    error.message ?: "$APPLE_ON_DEVICE_MODEL_LABEL request failed.",
                )
            }

        return response
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: throw OnDeviceModelException.GenerationFailed("$APPLE_ON_DEVICE_MODEL_LABEL returned no content.")
    }
}
