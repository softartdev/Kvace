package com.softartdev.kvace.app.clipboard

import androidx.compose.ui.platform.Clipboard

expect suspend fun Clipboard.setPlainText(text: String)
