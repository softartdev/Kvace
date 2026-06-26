package com.softartdev.kvace

import android.os.Build
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.rules.ExternalResource

internal class LocalNetworkPermissionRule : ExternalResource() {

    override fun before() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.CINNAMON_BUN) return

        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.uiAutomation.grantRuntimePermission(
            instrumentation.targetContext.packageName,
            "android.permission.ACCESS_LOCAL_NETWORK",
        )
    }
}
