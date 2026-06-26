package com.softartdev.kvace.feature.chat.data.local

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.util.Properties

class JvmChatDatabaseDriverFactory : ChatDatabaseDriverFactory {
    private val filePathResolver: () -> String = FilePathResolver()

    override suspend fun createDriver(): SqlDriver = JdbcSqliteDriver(
        url = "jdbc:sqlite:${filePathResolver()}",
        properties = Properties(),
        schema = ChatDatabase.Schema.synchronous(),
    )
}
