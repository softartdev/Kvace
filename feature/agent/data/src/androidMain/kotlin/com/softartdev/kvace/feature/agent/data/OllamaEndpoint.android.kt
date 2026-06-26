package com.softartdev.kvace.feature.agent.data

import android.content.Context
import com.softartdev.kvace.feature.agent.data.emulator.EmuDetector

class AndroidOllamaEndpointProvider(
    private val context: Context,
) : OllamaEndpointProvider {

    override fun defaultEndpoint(): String {
        val emulator: Boolean = EmuDetector.with(context).detect()
        val host: String = if (emulator) ANDROID_EMULATOR_HOST else LOOPBACK_HOST
        return ollamaEndpoint(host)
    }
}
