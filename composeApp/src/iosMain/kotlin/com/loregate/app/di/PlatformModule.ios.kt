package com.loregate.app.di

import com.loregate.app.core.location.GeofenceManager
import com.loregate.app.core.location.IosGeofenceManager
import com.loregate.app.core.location.IosLocationTracker
import com.loregate.app.core.location.LocationTracker
import com.loregate.app.core.location.MovementDetector
import com.loregate.app.data.service.MediaStorageConfig
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(): Module = module {
    single { MediaStorageConfig() }
    single { MovementDetector() }
    single<LocationTracker> { IosLocationTracker(get()) }
    single<GeofenceManager> { IosGeofenceManager() }
}
