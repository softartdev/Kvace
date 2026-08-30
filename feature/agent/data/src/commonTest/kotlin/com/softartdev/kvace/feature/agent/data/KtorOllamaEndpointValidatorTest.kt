package com.softartdev.kvace.feature.agent.data

import com.softartdev.kvace.feature.agent.domain.OllamaEndpointValidationResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class KtorOllamaEndpointValidatorTest {
    private val validator = KtorOllamaEndpointValidator()

    @Test
    fun normalizesSupportedHosts() {
        assertValid("localhost", "11434", "http://localhost:11434")
        assertValid("OLLAMA.local", "11434", "http://ollama.local:11434")
        assertValid("127.0.0.1", "11434", "http://127.0.0.1:11434")
        assertValid("::1", "11434", "http://[::1]:11434")
        assertValid("[2001:db8::1]", "11434", "http://[2001:db8::1]:11434")
        assertValid("::ffff:192.168.0.1", "11434", "http://[::ffff:192.168.0.1]:11434")
    }

    @Test
    fun rejectsInvalidHosts() {
        listOf(
            "",
            "   ",
            "http://localhost",
            "localhost/path",
            "localhost?query=true",
            "user@localhost",
            "local host",
            "999.0.0.1",
            "bad_host",
            "2001::db8::1",
            "[::1",
        ).forEach { host ->
            assertEquals(
                OllamaEndpointValidationResult.InvalidHost,
                validator.validate(host, "11434"),
                "Expected invalid host: $host",
            )
        }
    }

    @Test
    fun rejectsInvalidPorts() {
        listOf("", "abc", "0", "65536", "-1").forEach { port ->
            assertEquals(
                OllamaEndpointValidationResult.InvalidPort,
                validator.validate("localhost", port),
                "Expected invalid port: $port",
            )
        }
    }

    @Test
    fun parsesOnlyNormalizedHttpEndpointsWithoutExtraUrlParts() {
        assertEquals(
            "http://[::1]:11434",
            validator.parse("http://[::1]:11434")?.value,
        )
        assertNull(validator.parse("https://localhost:11434"))
        assertNull(validator.parse("http://localhost"))
        assertNull(validator.parse("http://user@localhost:11434"))
        assertNull(validator.parse("http://localhost:11434/api"))
        assertNull(validator.parse("http://localhost:11434?query=true"))
        assertNull(validator.parse("http://localhost:11434#fragment"))
    }

    private fun assertValid(host: String, port: String, expectedEndpoint: String) {
        val result = assertIs<OllamaEndpointValidationResult.Valid>(validator.validate(host, port))
        assertEquals(expectedEndpoint, result.endpoint.value)
    }
}
