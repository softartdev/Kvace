package com.softartdev.kvace.feature.agent.data

import android.content.Context
import com.softartdev.kvace.feature.agent.data.emulator.EmuDetector

fun androidOllamaEndpointProvider(context: Context): OllamaEndpointProvider {
    val emulator: Boolean = EmuDetector.with(context).detect()
    val host: String = if (emulator) ANDROID_EMULATOR_HOST else LOOPBACK_HOST
    return StaticOllamaEndpointProvider(host)
}
