package com.softartdev.kvace.app.clipboard

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.ClipEntry
import java.awt.datatransfer.StringSelection

@OptIn(ExperimentalComposeUiApi::class)
actual suspend fun Clipboard.setPlainText(text: String) {
    setClipEntry(ClipEntry(StringSelection(text)))
}
