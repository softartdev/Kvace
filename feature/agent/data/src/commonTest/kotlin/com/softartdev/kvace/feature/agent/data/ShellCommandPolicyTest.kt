package com.softartdev.kvace.feature.agent.data

import com.softartdev.kvace.feature.agent.domain.ShellCommandRequest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ShellCommandPolicyTest {

    @Test
    fun acceptsReadOnlyDiagnosticCommands() {
        assertNull(ShellCommandPolicy.validate(ShellCommandRequest(command = "pwd")))
        assertNull(ShellCommandPolicy.validate(ShellCommandRequest(command = "ls", args = listOf("-la"))))
        assertNull(ShellCommandPolicy.validate(ShellCommandRequest(command = "rg", args = listOf("-n", "AgentRuntime"))))
        assertNull(ShellCommandPolicy.validate(ShellCommandRequest(command = "git", args = listOf("status", "--short"))))
        assertNull(ShellCommandPolicy.validate(ShellCommandRequest(command = "git", args = listOf("diff", "--stat"))))
        assertNull(ShellCommandPolicy.validate(ShellCommandRequest(command = "git", args = listOf("log", "--oneline"))))
        assertNull(ShellCommandPolicy.validate(ShellCommandRequest(command = "git", args = listOf("show", "HEAD"))))
    }

    @Test
    fun acceptsNumericSedPrintRangesOnly() {
        assertNull(ShellCommandPolicy.validate(ShellCommandRequest(command = "sed", args = listOf("-n", "1,40p", "README.md"))))

        val rejected = ShellCommandPolicy.validate(ShellCommandRequest(command = "sed", args = listOf("-n", "1e touch bad")))
        val extraScript = ShellCommandPolicy.validate(
            ShellCommandRequest(command = "sed", args = listOf("-n", "1p", "-e", "w out.txt", "README.md"))
        )

        assertTrue(rejected?.reason.orEmpty().contains("numeric print ranges"))
        assertTrue(extraScript?.reason.orEmpty().contains("file paths"))
    }

    @Test
    fun rejectsShellSyntaxAndPathEscapes() {
        val pipe = ShellCommandPolicy.validate(ShellCommandRequest(command = "rg", args = listOf("agent|runtime")))
        val substitution = ShellCommandPolicy.validate(ShellCommandRequest(command = "ls", args = listOf("\$(pwd)")))
        val parent = ShellCommandPolicy.validate(ShellCommandRequest(command = "cat", args = listOf("../secret")))
        val absolute = ShellCommandPolicy.validate(ShellCommandRequest(command = "cat", args = listOf("/etc/passwd")))

        assertTrue(pipe?.reason.orEmpty().contains("unsafe shell token"))
        assertTrue(substitution?.reason.orEmpty().contains("unsafe shell token"))
        assertTrue(parent?.reason.orEmpty().contains("path escape"))
        assertTrue(absolute?.reason.orEmpty().contains("Absolute paths"))
    }

    @Test
    fun rejectsCommandsOutsideAllowlist() {
        val rejected = ShellCommandPolicy.validate(ShellCommandRequest(command = "rm", args = listOf("README.md")))

        assertEquals("Command \"rm\" is not in the read-only allowlist.", rejected?.reason)
    }

    @Test
    fun rejectsUnsafeGitAndRipgrepOptions() {
        val checkout = ShellCommandPolicy.validate(ShellCommandRequest(command = "git", args = listOf("checkout", "main")))
        val workTree = ShellCommandPolicy.validate(ShellCommandRequest(command = "git", args = listOf("status", "--work-tree=/tmp")))
        val output = ShellCommandPolicy.validate(ShellCommandRequest(command = "git", args = listOf("diff", "--output=diff.txt")))
        val pre = ShellCommandPolicy.validate(ShellCommandRequest(command = "rg", args = listOf("--pre", "cat", "Agent")))

        assertTrue(checkout?.reason.orEmpty().contains("read-only allowlist"))
        assertTrue(workTree?.reason.orEmpty().contains("unsafe git option"))
        assertTrue(output?.reason.orEmpty().contains("unsafe git option"))
        assertTrue(pre?.reason.orEmpty().contains("unsafe rg option"))
    }
}
