package com.softartdev.kvace.core.domain

import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlin.test.Test
import kotlin.test.assertSame

class CoroutineDispatchersStubTest {

    @Test
    fun usesSingleTestDispatcherForAllDispatchers() {
        val dispatchers = CoroutineDispatchersStub(TestCoroutineScheduler())

        assertSame(dispatchers.default, dispatchers.main)
        assertSame(dispatchers.default, dispatchers.unconfined)
        assertSame(dispatchers.default, dispatchers.io)
    }
}
