package com.softartdev.kvace

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.softartdev.kvace.app.di.kvaceModule
import com.softartdev.kvace.app.navigation.KvaceNavigationShell
import com.softartdev.theme.material3.PreferableMaterialTheme
import org.koin.compose.KoinApplication
import org.koin.dsl.KoinConfiguration

@Composable
@Preview
fun App() = KoinApplication(
    configuration = KoinConfiguration {
        modules(kvaceModule)
    },
) {
    PreferableMaterialTheme {
        KvaceNavigationShell()
    }
}
