package com.softartdev.kvace.feature.agent.data

import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging

fun createAgentHttpClient(
    tag: String = "Ktor",
    configure: (HttpClientConfig<*>.() -> Unit)? = null,
): HttpClient = HttpClient {
    install(Logging) {
        level = LogLevel.ALL
        logger = KermitKtorLogger(Severity.Debug, Logger.withTag(tag))
    }
    followRedirects = true
    configure?.invoke(this)
}
