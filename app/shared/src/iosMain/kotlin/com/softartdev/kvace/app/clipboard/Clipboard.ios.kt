package com.softartdev.kvace.app.clipboard

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.ClipEntry

@OptIn(ExperimentalComposeUiApi::class)
actual suspend fun Clipboard.setPlainText(text: String) {
    setClipEntry(ClipEntry.withPlainText(text))
}
