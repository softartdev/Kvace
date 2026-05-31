package com.softartdev.kvace.feature.agent.data

import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText

internal suspend fun fetchAgentEndpointStatus(
    url: String,
    timeoutMillis: Long,
): Int {
    val client = createAgentHttpClient(tag = "Ktor/AgentEndpointStatus") {
        installEndpointTimeout(timeoutMillis)
    }
    return try {
        client.get(url).status.value
    } finally {
        client.close()
    }
}

internal suspend fun fetchAgentEndpointText(
    url: String,
    timeoutMillis: Long,
): String {
    val client = createAgentHttpClient(tag = "Ktor/AgentEndpointText") {
        installEndpointTimeout(timeoutMillis)
    }
    return try {
        val response = client.get(url)
        val body = response.bodyAsText()
        val status = response.status.value
        if (status !in 200..299) {
            error("HTTP $status")
        }
        body
    } finally {
        client.close()
    }
}

private fun HttpClientConfig<*>.installEndpointTimeout(timeoutMillis: Long) {
    install(HttpTimeout) {
        connectTimeoutMillis = timeoutMillis
        requestTimeoutMillis = timeoutMillis
        socketTimeoutMillis = timeoutMillis
    }
}
