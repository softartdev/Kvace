package com.softartdev.kvace.feature.chat.data.local

import net.harawata.appdirs.AppDirs
import net.harawata.appdirs.AppDirsFactory
import java.io.File

internal class FilePathResolver(
    private val appDirs: AppDirs = AppDirsFactory.getInstance(),
    private val appName: String = "kvace",
    private val databaseName: String = DATABASE_NAME,
    private val createDirectories: Boolean = true,
) : () -> String {

    override fun invoke(): String {
        val directory = File(appDirs.getUserDataDir(appName, null, null))
        if (createDirectories) {
            directory.mkdirs()
        }
        return File(directory, databaseName).absolutePath
    }
}
