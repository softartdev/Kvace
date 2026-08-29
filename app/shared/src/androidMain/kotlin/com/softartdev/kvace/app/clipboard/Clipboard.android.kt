package com.softartdev.kvace.app.clipboard

import android.content.ClipData
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.toClipEntry

actual suspend fun Clipboard.setPlainText(text: String) {
    setClipEntry(ClipData.newPlainText(null, text).toClipEntry())
}
