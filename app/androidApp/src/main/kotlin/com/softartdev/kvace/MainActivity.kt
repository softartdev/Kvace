package com.softartdev.kvace

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import co.touchlab.kermit.Logger

class MainActivity : ComponentActivity() {
    private val logger = Logger.withTag("MainActivity")

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        requestLocalNetworkPermissionIfNeeded()
        setContent { App() }
    }

    private fun requestLocalNetworkPermissionIfNeeded() {
        // Android 17 gates emulator/localhost Ollama connections behind local-network access.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.CINNAMON_BUN) return
        val result: Int = checkSelfPermission(ACCESS_LOCAL_NETWORK_PERMISSION)
        if (result == PackageManager.PERMISSION_GRANTED) return
        registerForActivityResult(contract = RequestPermission()) { isGranted: Boolean ->
            logger.d("$ACCESS_LOCAL_NETWORK_PERMISSION granted:$isGranted")
        }.launch(ACCESS_LOCAL_NETWORK_PERMISSION)
    }

    private companion object {
        const val ACCESS_LOCAL_NETWORK_PERMISSION = "android.permission.ACCESS_LOCAL_NETWORK"
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
