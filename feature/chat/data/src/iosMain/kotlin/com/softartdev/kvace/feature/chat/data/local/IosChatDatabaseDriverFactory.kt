package com.softartdev.kvace.feature.chat.data.local

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import co.touchlab.sqliter.DatabaseConfiguration
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask

@OptIn(ExperimentalForeignApi::class)
class IosChatDatabaseDriverFactory : ChatDatabaseDriverFactory {

    override suspend fun createDriver(): SqlDriver = NativeSqliteDriver(
        schema = ChatDatabase.Schema.synchronous(),
        name = DATABASE_NAME,
        onConfiguration = this::hookBasePath
    )

    private fun hookBasePath(configuration: DatabaseConfiguration): DatabaseConfiguration {
        val documentDirectory: NSURL? = NSFileManager.defaultManager.URLForDirectory(
            directory = NSDocumentDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = false,
            error = null,
        )
        val extended = configuration.extendedConfig.copy(basePath = documentDirectory?.path)
        return configuration.copy(extendedConfig = extended)
    }
}
