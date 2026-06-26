package com.softartdev.kvace.feature.chat.data.local

import app.cash.sqldelight.db.SqlDriver

interface ChatDatabaseDriverFactory {
    suspend fun createDriver(): SqlDriver
}
