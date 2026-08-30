package com.softartdev.kvace.feature.agent.data

import com.softartdev.kvace.feature.agent.domain.OpenAiEndpointValidator

class DefaultOpenAiEndpointValidator : OpenAiEndpointValidator {
    override fun normalize(input: String): String? {
        val trimmed = input.trim().trimEnd('/')
        if (!trimmed.startsWith("https://") && !trimmed.startsWith("http://")) return null
        if (trimmed.contains('?') || trimmed.contains('#') || trimmed.substringAfter("://").isBlank()) return null
        return trimmed.removeSuffix("/v1").takeIf { it.substringAfter("://").isNotBlank() }
    }
}
