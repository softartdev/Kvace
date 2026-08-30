package com.softartdev.kvace.feature.agent.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProcessFoundationModelsBridgeTest {

    @Test
    fun sendsVersionedStatusRequestWithFiveSecondTimeout() = runBlocking {
        var receivedInput = ""
        var receivedTimeout = 0L
        val bridge = ProcessFoundationModelsBridge(commandRunner = FoundationModelsCommandRunner { input, timeoutMillis ->
            receivedInput = input
            receivedTimeout = timeoutMillis
            successfulResult("""{"version":1,"kind":"status","status":"available"}""")
        })

        val response = bridge.status()

        assertContains(receivedInput, "\"version\":1")
        assertContains(receivedInput, "\"operation\":\"status\"")
        assertEquals(5_000L, receivedTimeout)
        assertEquals("available", response.status)
    }

    @Test
    fun sendsPromptOnlyThroughVersionedStdinRequest() = runBlocking {
        var receivedInput = ""
        var receivedTimeout = 0L
        val bridge = ProcessFoundationModelsBridge(commandRunner = FoundationModelsCommandRunner { input, timeoutMillis ->
            receivedInput = input
            receivedTimeout = timeoutMillis
            successfulResult("""{"version":1,"kind":"generation","content":"Hello"}""")
        })

        val response = bridge.generate("private prompt")

        assertContains(receivedInput, "\"operation\":\"generate\"")
        assertContains(receivedInput, "\"prompt\":\"private prompt\"")
        assertEquals(120_000L, receivedTimeout)
        assertEquals("Hello", response.content)
    }

    @Test
    fun rejectsMalformedEmptyAndUnsupportedResponses() = runBlocking {
        val invalidOutputs = listOf(
            "not-json",
            "",
            """{"version":2,"kind":"status","status":"available"}""",
        )

        invalidOutputs.forEach { output ->
            val bridge = ProcessFoundationModelsBridge(
                commandRunner = FoundationModelsCommandRunner { _, _ -> successfulResult(output) },
            )
            assertFailsWith<FoundationModelsBridgeException> { bridge.status() }
        }
    }

    @Test
    fun rejectsNonZeroTimeoutAndOversizedOutput() = runBlocking {
        val failures = listOf(
            FoundationModelsProcessResult(exitCode = 9, stdout = "", stderr = "native failure"),
            FoundationModelsProcessResult(exitCode = -1, stdout = "", stderr = "", timedOut = true),
            FoundationModelsProcessResult(
                exitCode = 0,
                stdout = "{}",
                stderr = "",
                stdoutTruncated = true,
            ),
        )

        failures.forEach { result ->
            val bridge = ProcessFoundationModelsBridge(
                commandRunner = FoundationModelsCommandRunner { _, _ -> result },
            )
            assertFailsWith<FoundationModelsBridgeException> { bridge.status() }
        }
    }

    @Test
    fun reportsMissingApplicationResource() {
        val resources = Files.createTempDirectory("kvace-empty-resources-")

        assertFailsWith<FoundationModelsBridgeException> {
            FoundationModelsHelperExecutable(resources).path()
        }
    }

    @Test
    fun cancellationForciblyDestroysChildProcess() = runBlocking {
        val process = BlockingProcess()
        val runner = FoundationModelsProcessRunner(
            executableProvider = { Path.of("unused") },
            processFactory = { process },
        )
        val job = launch(Dispatchers.Default) {
            runner.run("{}", timeoutMillis = 120_000L)
        }

        assertTrue(process.waitStarted.await(2, TimeUnit.SECONDS))
        job.cancelAndJoin()

        assertTrue(process.wasForciblyDestroyed.get())
        assertFalse(process.isAlive)
    }

    private fun successfulResult(stdout: String) = FoundationModelsProcessResult(
        exitCode = 0,
        stdout = stdout,
        stderr = "",
    )
}

private class BlockingProcess : Process() {
    val waitStarted = CountDownLatch(1)
    val wasForciblyDestroyed = AtomicBoolean(false)
    private val alive = AtomicBoolean(true)
    private val stdin = ByteArrayOutputStream()

    override fun getOutputStream(): OutputStream = stdin
    override fun getInputStream(): InputStream = ByteArrayInputStream(ByteArray(0))
    override fun getErrorStream(): InputStream = ByteArrayInputStream(ByteArray(0))

    override fun waitFor(): Int {
        waitUntilDestroyed()
        return 0
    }

    override fun waitFor(timeout: Long, unit: TimeUnit): Boolean {
        waitStarted.countDown()
        waitUntilDestroyed()
        return true
    }

    override fun exitValue(): Int {
        if (alive.get()) throw IllegalThreadStateException("Process is still running")
        return 0
    }

    override fun destroy() {
        alive.set(false)
    }

    override fun destroyForcibly(): Process {
        wasForciblyDestroyed.set(true)
        alive.set(false)
        return this
    }

    override fun isAlive(): Boolean = alive.get()

    private fun waitUntilDestroyed() {
        while (alive.get()) {
            Thread.sleep(10_000L)
        }
    }
}
