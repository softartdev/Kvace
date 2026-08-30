package com.softartdev.kvace.feature.agent.data

import co.touchlab.kermit.Logger
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.runBlocking

class JvmOnDeviceModelProvider internal constructor(
    private val platform: JvmPlatformInfo,
    private val bridge: FoundationModelsBridge,
) : OnDeviceModelProvider {
    constructor() : this(
        platform = JvmPlatformInfo.current(),
        bridge = ProcessFoundationModelsBridge(),
    )

    private val logger = Logger.withTag("JvmOnDeviceModelProvider")
    private val availability: JvmOnDeviceAvailability = loadAvailability()

    internal val cachedAvailability: JvmOnDeviceAvailability
        get() = availability

    override val modelName: String = if (platform.unavailableReason() == null) {
        APPLE_ON_DEVICE_MODEL_LABEL
    } else {
        DEFAULT_ON_DEVICE_MODEL_LABEL
    }

    override val isAvailable: Boolean
        get() = availability == JvmOnDeviceAvailability.Available

    override suspend fun generateContent(prompt: String): String {
        platform.unavailableReason()?.let { reason ->
            throw OnDeviceModelException.Unavailable(reason.message)
        }

        val response = try {
            bridge.generate(prompt)
        } catch (error: Throwable) {
            currentCoroutineContext().ensureActive()
            logger.e(error) { "Apple Foundation Models bridge request failed" }
            throw OnDeviceModelException.GenerationFailed("Apple Foundation Models request failed.")
        }
        return when (response.kind) {
            GENERATION_KIND -> response.content
                ?.trim()
                ?.takeIf { it.isNotBlank() }
                ?: throw OnDeviceModelException.GenerationFailed("Apple Foundation Models returned no content.")
            ERROR_KIND -> throw response.toOnDeviceModelException()
            else -> throw OnDeviceModelException.GenerationFailed(
                "Apple Foundation Models returned an unsupported response.",
            )
        }
    }

    private fun loadAvailability(): JvmOnDeviceAvailability {
        platform.unavailableReason()?.let { return it }
        val response = try {
            runBlocking { bridge.status() }
        } catch (error: Throwable) {
            logger.e(error) { "Failed to check Apple Foundation Models availability" }
            return JvmOnDeviceAvailability.BridgeUnavailable
        }
        return when {
            response.kind == STATUS_KIND && response.status == AVAILABLE_STATUS -> JvmOnDeviceAvailability.Available
            response.kind == STATUS_KIND -> response.reason.toAvailability()
            response.kind == ERROR_KIND -> response.reason.toAvailability()
            else -> JvmOnDeviceAvailability.Unknown
        }
    }

    private fun FoundationModelsBridgeResponse.toOnDeviceModelException(): OnDeviceModelException {
        val safeMessage = message?.takeIf { it.isNotBlank() }
        return when {
            reason == MODEL_NOT_READY_REASON -> OnDeviceModelException.Downloading(
                safeMessage ?: JvmOnDeviceAvailability.ModelNotReady.message,
            )
            errorCode == UNAVAILABLE_ERROR -> OnDeviceModelException.Unavailable(
                safeMessage ?: reason.toAvailability().message,
            )
            else -> OnDeviceModelException.GenerationFailed(
                safeMessage ?: "Apple Foundation Models generation failed.",
            )
        }
    }

    private fun String?.toAvailability(): JvmOnDeviceAvailability = when (this) {
        APPLE_INTELLIGENCE_NOT_ENABLED_REASON -> JvmOnDeviceAvailability.AppleIntelligenceNotEnabled
        MODEL_NOT_READY_REASON -> JvmOnDeviceAvailability.ModelNotReady
        DEVICE_NOT_ELIGIBLE_REASON -> JvmOnDeviceAvailability.DeviceNotEligible
        UNSUPPORTED_HARDWARE_REASON -> JvmOnDeviceAvailability.UnsupportedHardware
        UNSUPPORTED_OS_REASON -> JvmOnDeviceAvailability.UnsupportedOs
        else -> JvmOnDeviceAvailability.Unknown
    }

    private companion object {
        const val STATUS_KIND = "status"
        const val GENERATION_KIND = "generation"
        const val ERROR_KIND = "error"
        const val AVAILABLE_STATUS = "available"
        const val UNAVAILABLE_ERROR = "unavailable"
        const val APPLE_INTELLIGENCE_NOT_ENABLED_REASON = "appleIntelligenceNotEnabled"
        const val MODEL_NOT_READY_REASON = "modelNotReady"
        const val DEVICE_NOT_ELIGIBLE_REASON = "deviceNotEligible"
        const val UNSUPPORTED_HARDWARE_REASON = "unsupportedHardware"
        const val UNSUPPORTED_OS_REASON = "unsupportedOs"
    }
}

internal data class JvmPlatformInfo(
    val osName: String,
    val osVersion: String,
    val architecture: String,
) {
    val isMacOs: Boolean
        get() = osName.equals("Mac OS X", ignoreCase = true) || osName.equals("macOS", ignoreCase = true)

    private val isAppleSilicon: Boolean
        get() = architecture.equals("aarch64", ignoreCase = true) || architecture.equals("arm64", ignoreCase = true)

    private val macOsMajorVersion: Int?
        get() = osVersion.substringBefore('.').toIntOrNull()

    fun unavailableReason(): JvmOnDeviceAvailability? {
        val majorVersion = macOsMajorVersion
        return when {
        !isMacOs -> JvmOnDeviceAvailability.UnsupportedJvmPlatform
        !isAppleSilicon -> JvmOnDeviceAvailability.UnsupportedHardware
        majorVersion == null || majorVersion < MINIMUM_MACOS_VERSION -> JvmOnDeviceAvailability.UnsupportedOs
        else -> null
        }
    }

    companion object {
        fun current() = JvmPlatformInfo(
            osName = System.getProperty("os.name").orEmpty(),
            osVersion = System.getProperty("os.version").orEmpty(),
            architecture = System.getProperty("os.arch").orEmpty(),
        )

        private const val MINIMUM_MACOS_VERSION = 26
    }
}

internal sealed class JvmOnDeviceAvailability(val message: String) {
    data object Available : JvmOnDeviceAvailability("Apple Foundation Models is available.")
    data object UnsupportedJvmPlatform : JvmOnDeviceAvailability(
        "On-device AI is unavailable on this JVM platform.",
    )
    data object UnsupportedHardware : JvmOnDeviceAvailability(
        "Apple Foundation Models requires a Mac with Apple silicon.",
    )
    data object UnsupportedOs : JvmOnDeviceAvailability(
        "Apple Foundation Models requires macOS 26 or newer.",
    )
    data object AppleIntelligenceNotEnabled : JvmOnDeviceAvailability(
        "Turn on Apple Intelligence in System Settings to use Apple Foundation Models.",
    )
    data object ModelNotReady : JvmOnDeviceAvailability(
        "Apple Foundation Models is not ready yet. Try again after the model download completes.",
    )
    data object DeviceNotEligible : JvmOnDeviceAvailability(
        "Apple Foundation Models is unavailable on this Mac.",
    )
    data object BridgeUnavailable : JvmOnDeviceAvailability(
        "Apple Foundation Models bridge is unavailable.",
    )
    data object Unknown : JvmOnDeviceAvailability(
        "Apple Foundation Models is currently unavailable.",
    )
}
