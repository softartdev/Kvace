package com.softartdev.kvace

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.softartdev.kronos.Network
import com.softartdev.kronos.sync
import kvace.app.desktop.generated.resources.Res
import kvace.app.desktop.generated.resources.kvace_window_icon
import org.jetbrains.compose.resources.painterResource
import kotlin.time.Clock

fun main() = application {
    Clock.Network.sync()
    Window(
        onCloseRequest = ::exitApplication,
        title = "Kvace",
        icon = painterResource(Res.drawable.kvace_window_icon),
    ) {
        App()
    }
}
