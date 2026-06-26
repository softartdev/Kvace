package com.softartdev.kvace.feature.chat.data.local

import android.content.Context
import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import app.cash.sqldelight.db.SqlDriver

class AndroidChatDatabaseDriverFactory(
    private val context: Context,
) : ChatDatabaseDriverFactory {

    override suspend fun createDriver(): SqlDriver = AndroidSqliteDriver(
        schema = ChatDatabase.Schema.synchronous(),
        context = context.applicationContext,
        name = DATABASE_NAME,
    )
}
