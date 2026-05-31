package com.softartdev.kvace

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        requestLocalNetworkPermissionIfNeeded()

        setContent {
            App()
        }
    }

    private fun requestLocalNetworkPermissionIfNeeded() {
        // Android 17 gates emulator/localhost Ollama connections behind local-network access.
        if (Build.VERSION.SDK_INT < LOCAL_NETWORK_PERMISSION_SDK) return
        if (checkSelfPermission(ACCESS_LOCAL_NETWORK_PERMISSION) == PackageManager.PERMISSION_GRANTED) return

        requestPermissions(
            arrayOf(ACCESS_LOCAL_NETWORK_PERMISSION),
            ACCESS_LOCAL_NETWORK_REQUEST_CODE,
        )
    }

    private companion object {
        const val ACCESS_LOCAL_NETWORK_PERMISSION = "android.permission.ACCESS_LOCAL_NETWORK"
        const val ACCESS_LOCAL_NETWORK_REQUEST_CODE = 37_001
        const val LOCAL_NETWORK_PERMISSION_SDK = 37
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
