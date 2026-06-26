package com.softartdev.kvace.feature.chat.data.local

import app.cash.sqldelight.async.coroutines.awaitCreate
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.worker.createDefaultWebWorkerDriver

class WasmChatDatabaseDriverFactory : ChatDatabaseDriverFactory {

    override suspend fun createDriver(): SqlDriver =
        createDefaultWebWorkerDriver().also { driver ->
            ChatDatabase.Schema.awaitCreate(driver)
        }
}
