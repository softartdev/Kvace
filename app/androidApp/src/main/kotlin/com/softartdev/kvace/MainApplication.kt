package com.softartdev.kvace

import android.app.Application
import com.softartdev.kronos.Network
import com.softartdev.kronos.sync
import kotlin.time.Clock

class MainApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        Clock.Network.sync(applicationContext)
    }
}
