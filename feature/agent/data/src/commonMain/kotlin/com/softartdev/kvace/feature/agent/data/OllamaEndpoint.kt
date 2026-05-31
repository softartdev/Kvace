package com.softartdev.kvace.feature.agent.data

internal const val LOOPBACK_HOST = "127.0.0.1"
internal const val ANDROID_EMULATOR_HOST = "10.0.2.2"
internal const val OLLAMA_DEFAULT_PORT = 11434

interface OllamaEndpointProvider {
    fun defaultEndpoint(): String
}

fun defaultOllamaEndpointProvider(): OllamaEndpointProvider = StaticOllamaEndpointProvider(LOOPBACK_HOST)

internal fun ollamaEndpoint(host: String, port: Int = OLLAMA_DEFAULT_PORT): String = "http://$host:$port"

internal class StaticOllamaEndpointProvider(
    private val host: String,
) : OllamaEndpointProvider {
    override fun defaultEndpoint(): String = ollamaEndpoint(host)
}
