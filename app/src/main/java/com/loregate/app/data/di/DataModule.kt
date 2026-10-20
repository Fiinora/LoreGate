package com.loregate.app.data.di

import android.content.Context
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.loregate.app.core.location.FusedLocationTracker
import com.loregate.app.core.location.LocationTracker
import com.loregate.app.data.repository.FirebaseAuthRepository
import com.loregate.app.data.repository.FirestoreSiteRepository
import com.loregate.app.data.repository.FirestoreSpearRepository
import com.loregate.app.data.repository.FirestoreUserRepository
import com.loregate.app.data.repository.OsrmRouteRepository
import com.loregate.app.domain.repository.AuthRepository
import com.loregate.app.domain.repository.RouteRepository
import com.loregate.app.domain.repository.SiteRepository
import com.loregate.app.domain.repository.SpearRepository
import com.loregate.app.domain.repository.UserRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FirebaseAndLocationModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    @Provides
    @Singleton
    fun provideFusedLocationClient(
        @ApplicationContext context: Context
    ): FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)

    @Provides
    @Singleton
    fun provideGeofencingClient(
        @ApplicationContext context: Context
    ): GeofencingClient = LocationServices.getGeofencingClient(context)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: FirebaseAuthRepository): AuthRepository

    @Binds
    @Singleton
    abstract fun bindUserRepository(impl: FirestoreUserRepository): UserRepository

    @Binds
    @Singleton
    abstract fun bindSiteRepository(impl: FirestoreSiteRepository): SiteRepository

    @Binds
    @Singleton
    abstract fun bindSpearRepository(impl: FirestoreSpearRepository): SpearRepository

    @Binds
    @Singleton
    abstract fun bindRouteRepository(impl: OsrmRouteRepository): RouteRepository

    @Binds
    @Singleton
    abstract fun bindLocationTracker(impl: FusedLocationTracker): LocationTracker
}
