package com.loregate.app

import android.app.Application
import com.loregate.app.di.commonModule
import com.loregate.app.di.platformModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.maplibre.android.MapLibre
import org.maplibre.android.WellKnownTileServer

class LoreGateApp : Application() {

    companion object {
        var instance: LoreGateApp? = null
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        startKoin {
            androidContext(this@LoreGateApp)
            modules(commonModule, platformModule())
        }

        MapLibre.getInstance(
            applicationContext,
            BuildConfig.MAPTILER_API_KEY,
            WellKnownTileServer.MapTiler
        )
    }
}
