package com.loregate.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import org.maplibre.android.MapLibre
import org.maplibre.android.WellKnownTileServer

@HiltAndroidApp
class LoreGateApp : Application() {
    override fun onCreate() {
        super.onCreate()
        MapLibre.getInstance(
            applicationContext,
            BuildConfig.MAPTILER_API_KEY,
            WellKnownTileServer.MapTiler
        )
    }
}
