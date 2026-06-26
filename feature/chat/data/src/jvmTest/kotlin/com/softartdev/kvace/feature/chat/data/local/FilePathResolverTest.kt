package com.softartdev.kvace.feature.chat.data.local

import net.harawata.appdirs.impl.MacOSXAppDirs
import net.harawata.appdirs.impl.UnixAppDirs
import net.harawata.appdirs.impl.WindowsAppDirs
import net.harawata.appdirs.impl.WindowsFolderResolver
import kotlin.test.Test
import kotlin.test.assertEquals

class FilePathResolverTest : WindowsFolderResolver {
    private val home = System.getProperty("user.home")

    @Test
    fun resolvesMacosDatabasePathUnderApplicationSupport() {
        val expected = "$home/Library/Application Support/kvace/$DATABASE_NAME"

        val actual = FilePathResolver(
            appDirs = MacOSXAppDirs(),
            createDirectories = false,
        ).invoke()

        assertEquals(expected, actual)
    }

    @Test
    fun resolvesWindowsDatabasePathUnderLocalAppData() {
        val expected = "$home/AppData/Local/kvace/$DATABASE_NAME"

        val actual = FilePathResolver(
            appDirs = WindowsAppDirs(this),
            createDirectories = false,
        ).invoke()

        assertEquals(expected, actual)
    }

    @Test
    fun resolvesLinuxDatabasePathUnderLocalShare() {
        val expected = "$home/.local/share/kvace/$DATABASE_NAME"

        val actual = FilePathResolver(
            appDirs = UnixAppDirs(),
            createDirectories = false,
        ).invoke()

        assertEquals(expected, actual)
    }

    override fun resolveFolder(folderId: WindowsAppDirs.FolderId?): String = when (folderId) {
        WindowsAppDirs.FolderId.LOCAL_APPDATA -> "$home/AppData/Local"
        else -> error("Unsupported folder ID: $folderId")
    }
}
