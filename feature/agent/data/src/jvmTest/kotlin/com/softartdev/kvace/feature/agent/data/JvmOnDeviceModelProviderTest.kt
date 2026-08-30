package com.softartdev.kvace.feature.agent.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class JvmOnDeviceModelProviderTest {

    @Test
    fun makesAppleFoundationModelsAvailableOnMacOs26AppleSiliconAndCachesStatus() {
        val bridge = FakeFoundationModelsBridge(statusResponse = availableStatus())
        val provider = provider(bridge = bridge)

        assertEquals(APPLE_ON_DEVICE_MODEL_LABEL, provider.modelName)
        assertTrue(provider.isAvailable)
        assertTrue(provider.isAvailable)
        assertEquals(JvmOnDeviceAvailability.Available, provider.cachedAvailability)
        assertEquals(1, bridge.statusCalls)
    }

    @Test
    fun rejectsUnsupportedJvmPlatformsBeforeStartingBridge() {
        val platforms = listOf(
            JvmPlatformInfo("Linux", "6.8", "aarch64") to JvmOnDeviceAvailability.UnsupportedJvmPlatform,
            JvmPlatformInfo("Windows 11", "10.0", "amd64") to JvmOnDeviceAvailability.UnsupportedJvmPlatform,
            JvmPlatformInfo("Mac OS X", "26.0", "x86_64") to JvmOnDeviceAvailability.UnsupportedHardware,
            JvmPlatformInfo("Mac OS X", "25.9", "aarch64") to JvmOnDeviceAvailability.UnsupportedOs,
            JvmPlatformInfo("Mac OS X", "invalid", "aarch64") to JvmOnDeviceAvailability.UnsupportedOs,
        )

        platforms.forEach { (platform, expected) ->
            val bridge = FakeFoundationModelsBridge(statusResponse = availableStatus())
            val provider = JvmOnDeviceModelProvider(platform, bridge)
            assertFalse(provider.isAvailable)
            assertEquals(DEFAULT_ON_DEVICE_MODEL_LABEL, provider.modelName)
            assertEquals(expected, provider.cachedAvailability)
            assertEquals(0, bridge.statusCalls)
        }
    }

    @Test
    fun mapsEveryNativeStatusReason() {
        val expectations = mapOf(
            "appleIntelligenceNotEnabled" to JvmOnDeviceAvailability.AppleIntelligenceNotEnabled,
            "modelNotReady" to JvmOnDeviceAvailability.ModelNotReady,
            "deviceNotEligible" to JvmOnDeviceAvailability.DeviceNotEligible,
            "unsupportedHardware" to JvmOnDeviceAvailability.UnsupportedHardware,
            "unsupportedOs" to JvmOnDeviceAvailability.UnsupportedOs,
            "unknown" to JvmOnDeviceAvailability.Unknown,
        )

        expectations.forEach { (reason, expected) ->
            val provider = provider(
                bridge = FakeFoundationModelsBridge(
                    statusResponse = FoundationModelsBridgeResponse(
                        version = 1,
                        kind = "status",
                        status = "unavailable",
                        reason = reason,
                    ),
                ),
            )
            assertFalse(provider.isAvailable)
            assertEquals(expected, provider.cachedAvailability)
        }
    }

    @Test
    fun mapsBridgeStatusFailuresToUnavailable() {
        val provider = provider(
            bridge = FakeFoundationModelsBridge(statusFailure = IllegalStateException("broken process")),
        )

        assertFalse(provider.isAvailable)
        assertEquals(JvmOnDeviceAvailability.BridgeUnavailable, provider.cachedAvailability)
    }

    @Test
    fun returnsNonBlankGeneratedContent() = runTest {
        val provider = provider(
            bridge = FakeFoundationModelsBridge(
                statusResponse = availableStatus(),
                generationResponse = FoundationModelsBridgeResponse(
                    version = 1,
                    kind = "generation",
                    content = "  Local answer  ",
                ),
            ),
        )

        assertEquals("Local answer", provider.generateContent("Hello"))
    }

    @Test
    fun mapsGenerationErrorsAndEmptyResponses() = runTest {
        val downloading = provider(
            bridge = FakeFoundationModelsBridge(
                statusResponse = availableStatus(),
                generationResponse = errorResponse("unavailable", "modelNotReady"),
            ),
        )
        val unavailable = provider(
            bridge = FakeFoundationModelsBridge(
                statusResponse = availableStatus(),
                generationResponse = errorResponse("unavailable", "appleIntelligenceNotEnabled"),
            ),
        )
        val empty = provider(
            bridge = FakeFoundationModelsBridge(
                statusResponse = availableStatus(),
                generationResponse = FoundationModelsBridgeResponse(
                    version = 1,
                    kind = "generation",
                    content = "  ",
                ),
            ),
        )

        assertFailsWith<OnDeviceModelException.Downloading> { downloading.generateContent("Hello") }
        assertFailsWith<OnDeviceModelException.Unavailable> { unavailable.generateContent("Hello") }
        assertFailsWith<OnDeviceModelException.GenerationFailed> { empty.generateContent("Hello") }
    }

    @Test
    fun cancellationIsNotConvertedToGenerationFailure() = runTest {
        val provider = provider(
            bridge = FakeFoundationModelsBridge(
                statusResponse = availableStatus(),
                suspendGeneration = true,
            ),
        )

        assertFailsWith<CancellationException> {
            withTimeout(1) { provider.generateContent("Hello") }
        }
    }

    private fun provider(
        bridge: FoundationModelsBridge,
        platform: JvmPlatformInfo = JvmPlatformInfo("Mac OS X", "26.0", "aarch64"),
    ) = JvmOnDeviceModelProvider(platform, bridge)

    private fun availableStatus() = FoundationModelsBridgeResponse(
        version = 1,
        kind = "status",
        status = "available",
    )

    private fun errorResponse(code: String, reason: String) = FoundationModelsBridgeResponse(
        version = 1,
        kind = "error",
        errorCode = code,
        reason = reason,
    )
}

private class FakeFoundationModelsBridge(
    private val statusResponse: FoundationModelsBridgeResponse = FoundationModelsBridgeResponse(
        version = 1,
        kind = "status",
        status = "available",
    ),
    private val generationResponse: FoundationModelsBridgeResponse = FoundationModelsBridgeResponse(
        version = 1,
        kind = "generation",
        content = "Answer",
    ),
    private val statusFailure: Throwable? = null,
    private val suspendGeneration: Boolean = false,
) : FoundationModelsBridge {
    var statusCalls: Int = 0
        private set

    override suspend fun status(): FoundationModelsBridgeResponse {
        statusCalls += 1
        statusFailure?.let { throw it }
        return statusResponse
    }

    override suspend fun generate(prompt: String): FoundationModelsBridgeResponse {
        if (suspendGeneration) awaitCancellation()
        return generationResponse
    }
}
