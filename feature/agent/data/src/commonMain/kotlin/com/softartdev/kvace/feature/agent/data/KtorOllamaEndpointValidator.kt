package com.softartdev.kvace.feature.agent.data

import com.softartdev.kvace.feature.agent.domain.OllamaEndpointValidationResult
import com.softartdev.kvace.feature.agent.domain.OllamaEndpointValidator
import com.softartdev.kvace.feature.agent.domain.ValidatedOllamaEndpoint
import io.ktor.http.URLProtocol
import io.ktor.http.Url

class KtorOllamaEndpointValidator : OllamaEndpointValidator {

    override fun validate(hostInput: String, portInput: String): OllamaEndpointValidationResult {
        val host = normalizedHost(hostInput) ?: return OllamaEndpointValidationResult.InvalidHost
        val port = portInput.trim().toIntOrNull()
        if (port == null || port !in PORT_RANGE) return OllamaEndpointValidationResult.InvalidPort
        val urlHost = if (host.isIpv6Address()) "[$host]" else host
        return OllamaEndpointValidationResult.Valid(
            ValidatedOllamaEndpoint(
                value = "http://$urlHost:$port",
                host = host,
                port = port,
            )
        )
    }

    override fun parse(endpoint: String): ValidatedOllamaEndpoint? {
        val url = runCatching { Url(endpoint.trim()) }.getOrNull() ?: return null
        if (url.protocol != URLProtocol.HTTP || url.specifiedPort !in PORT_RANGE) return null
        if (url.user != null || url.password != null || !url.parameters.isEmpty() || url.fragment.isNotEmpty()) {
            return null
        }
        if (url.encodedPath.isNotEmpty() && url.encodedPath != "/") return null

        val result = validate(url.host, url.specifiedPort.toString())
        return (result as? OllamaEndpointValidationResult.Valid)?.endpoint
    }

    private fun normalizedHost(input: String): String? {
        val trimmed = input.trim()
        if (trimmed.isEmpty() || trimmed.any(Char::isWhitespace)) return null
        if (trimmed.contains("://") || trimmed.any { it in FORBIDDEN_HOST_CHARACTERS }) return null

        val hasOpeningBracket = trimmed.startsWith('[')
        val hasClosingBracket = trimmed.endsWith(']')
        if (hasOpeningBracket != hasClosingBracket) return null
        val host = if (hasOpeningBracket) trimmed.substring(1, trimmed.lastIndex) else trimmed
        if (host.isEmpty()) return null

        val valid = when {
            host.contains(':') -> host.isIpv6Address()
            host.all { it.isDigit() || it == '.' } -> host.isIpv4Address()
            else -> host.isHostname()
        }
        return host.lowercase().takeIf { valid }
    }

    private fun String.isHostname(): Boolean {
        val hostname = removeSuffix(".")
        if (hostname.isEmpty() || hostname.length > MAX_HOSTNAME_LENGTH) return false
        return hostname.split('.').all { label ->
            label.length in 1..MAX_HOST_LABEL_LENGTH &&
                label.first().isAsciiLetterOrDigit() &&
                label.last().isAsciiLetterOrDigit() &&
                label.all { it.isAsciiLetterOrDigit() || it == '-' }
        }
    }

    private fun String.isIpv4Address(): Boolean {
        val octets = split('.')
        return octets.size == IPV4_OCTET_COUNT && octets.all { octet ->
            octet.isNotEmpty() && octet.all(Char::isDigit) && octet.toIntOrNull() in 0..255
        }
    }

    private fun String.isIpv6Address(): Boolean {
        if (isEmpty() || countDoubleColons() > 1) return false
        val hasCompression = contains("::")
        val sides = split("::", limit = 2)
        val left: List<String>? = sides.first().ipv6Parts()
        val right: List<String>? = if (sides.size == 2) sides[1].ipv6Parts() else emptyList()
        if (left == null || right == null) return false

        val partCount = left.sumOf { it.ipv6PartSize() } + right.sumOf { it.ipv6PartSize() }
        return if (hasCompression) partCount < IPV6_PART_COUNT else partCount == IPV6_PART_COUNT
    }

    private fun String.ipv6Parts(): List<String>? {
        if (isEmpty()) return emptyList()
        val parts = split(':')
        if (parts.any(String::isEmpty)) return null
        if (parts.dropLast(1).any { it.contains('.') }) return null
        return parts.takeIf { values -> values.all { it.isIpv6Part() } }
    }

    private fun String.isIpv6Part(): Boolean = when {
        contains('.') -> isIpv4Address()
        else -> length in 1..MAX_IPV6_HEXTET_LENGTH && all { it.isHexDigit() }
    }

    private fun String.ipv6PartSize(): Int = if (contains('.')) 2 else 1

    private fun String.countDoubleColons(): Int {
        var count = 0
        var index = indexOf("::")
        while (index >= 0) {
            count++
            index = indexOf("::", startIndex = index + 2)
        }
        return count
    }

    private fun Char.isAsciiLetterOrDigit(): Boolean =
        this in 'a'..'z' || this in 'A'..'Z' || this in '0'..'9'

    private fun Char.isHexDigit(): Boolean =
        isAsciiLetterOrDigit() && (isDigit() || lowercaseChar() in 'a'..'f')

    private companion object {
        val PORT_RANGE: IntRange = 1..65535
        val FORBIDDEN_HOST_CHARACTERS: Set<Char> = setOf('/', '?', '#', '@', '\\')
        const val MAX_HOSTNAME_LENGTH = 253
        const val MAX_HOST_LABEL_LENGTH = 63
        const val IPV4_OCTET_COUNT = 4
        const val IPV6_PART_COUNT = 8
        const val MAX_IPV6_HEXTET_LENGTH = 4
    }
}
