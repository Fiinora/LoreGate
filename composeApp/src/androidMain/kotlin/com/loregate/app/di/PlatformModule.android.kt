package com.loregate.app.di

import com.google.android.gms.location.LocationServices
import com.loregate.app.core.location.AndroidGeofenceManager
import com.loregate.app.core.location.AndroidLocationTracker
import com.loregate.app.core.location.GeofenceManager
import com.loregate.app.core.location.LocationTracker
import com.loregate.app.core.location.MovementDetector
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(): Module = module {
    single { LocationServices.getFusedLocationProviderClient(androidContext()) }
    single { LocationServices.getGeofencingClient(androidContext()) }
    single { MovementDetector() }
    single<LocationTracker> { AndroidLocationTracker(androidContext(), get(), get()) }
    single<GeofenceManager> { AndroidGeofenceManager(androidContext(), get()) }
}
