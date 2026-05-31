package com.softartdev.kvace.feature.chat.data

@OptIn(kotlin.time.ExperimentalTime::class)
internal fun currentTimeMillis(): Long = kotlin.time.Clock.System.now().toEpochMilliseconds()
