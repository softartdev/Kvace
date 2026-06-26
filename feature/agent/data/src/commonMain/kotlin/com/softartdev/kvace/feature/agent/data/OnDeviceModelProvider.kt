package com.softartdev.kvace.feature.agent.data

interface OnDeviceModelProvider {
    val modelName: String
    val isAvailable: Boolean

    suspend fun generateContent(prompt: String): String
}

sealed class OnDeviceModelException(message: String) : RuntimeException(message) {
    class Unavailable(message: String) : OnDeviceModelException(message)
    class Downloading(message: String) : OnDeviceModelException(message)
    class DownloadFailed(message: String) : OnDeviceModelException(message)
    class GenerationFailed(message: String) : OnDeviceModelException(message)
}

class UnavailableOnDeviceModelProvider(
    override val modelName: String = DEFAULT_ON_DEVICE_MODEL_LABEL,
    private val message: String = "On-device AI is unavailable on this platform.",
) : OnDeviceModelProvider {
    override val isAvailable: Boolean = false

    override suspend fun generateContent(prompt: String): String =
        throw OnDeviceModelException.Unavailable(message)
}

class DefaultUnavailableOnDeviceModelProvider : OnDeviceModelProvider by UnavailableOnDeviceModelProvider()

const val ANDROID_ON_DEVICE_MODEL_LABEL = "Gemini Nano"
const val APPLE_ON_DEVICE_MODEL_LABEL = "Apple Foundation Models"
const val DEFAULT_ON_DEVICE_MODEL_LABEL = "On-device model"
