package com.softartdev.kvace.feature.chat.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ChatScrollTest {

    @Test
    fun mapsProgressToItemAndOffset() {
        assertEquals(
            expected = ChatScrollTarget(index = 2, offsetFraction = 0.5f),
            actual = chatScrollTarget(progress = 0.5f, itemCount = 5),
        )
    }

    @Test
    fun clampsProgressToFirstAndLastItems() {
        assertEquals(
            expected = ChatScrollTarget(index = 0, offsetFraction = 0f),
            actual = chatScrollTarget(progress = -1f, itemCount = 5),
        )
        assertEquals(
            expected = ChatScrollTarget(index = 4, offsetFraction = 1f),
            actual = chatScrollTarget(progress = 2f, itemCount = 5),
        )
    }

    @Test
    fun returnsNullForEmptyList() {
        assertNull(chatScrollTarget(progress = 0.5f, itemCount = 0))
    }
}
