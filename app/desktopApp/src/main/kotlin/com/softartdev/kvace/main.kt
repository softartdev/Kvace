package com.softartdev.kvace

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.ui.unit.dp
import com.softartdev.kvace.core.ui.resources.*
import com.softartdev.kronos.Network
import com.softartdev.kronos.sync
import org.jetbrains.compose.resources.painterResource
import kotlin.time.Clock

fun main() = application {
    Clock.Network.sync()
    Window(
        onCloseRequest = ::exitApplication,
        title = "Kvace",
        icon = painterResource(Res.drawable.kvace_window_icon),
        state = rememberWindowState(width = 1280.dp, height = 820.dp),
    ) {
        App()
    }
}
