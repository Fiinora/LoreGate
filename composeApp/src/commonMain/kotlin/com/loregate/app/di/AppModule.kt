package com.loregate.app.di

import com.loregate.app.data.repository.FirebaseAuthRepository
import com.loregate.app.data.repository.FirestoreSiteRepository
import com.loregate.app.data.repository.FirestoreSpearRepository
import com.loregate.app.data.repository.FirestoreUserRepository
import com.loregate.app.data.repository.OsrmRouteRepository
import com.loregate.app.data.seeder.FirestoreDataSeeder
import com.loregate.app.domain.repository.AuthRepository
import com.loregate.app.domain.repository.RouteRepository
import com.loregate.app.domain.repository.SiteRepository
import com.loregate.app.domain.repository.SpearRepository
import com.loregate.app.domain.repository.UserRepository
import com.loregate.app.domain.usecase.EvaluateSiteUnlockStateUseCase
import com.loregate.app.domain.usecase.GetStoriesWithProgressUseCase
import com.loregate.app.domain.usecase.GetStoryTimelineUseCase
import com.loregate.app.feature.auth.LoginViewModel
import com.loregate.app.feature.home.HomeViewModel
import com.loregate.app.feature.map.MapViewModel
import com.loregate.app.feature.profile.ProfileViewModel
import com.loregate.app.feature.story.ChapterDetailViewModel
import com.loregate.app.feature.story.GameChapterViewModel
import com.loregate.app.feature.story.StoryListViewModel
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.firestore
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val commonModule = module {
    // Firebase
    single { Firebase.auth }
    single { Firebase.firestore }

    // Seeders & Use Cases
    single { FirestoreDataSeeder(get()) }
    single { EvaluateSiteUnlockStateUseCase() }
    single { GetStoriesWithProgressUseCase(get(), get(), get()) }
    single { GetStoryTimelineUseCase(get(), get(), get()) }

    // Repositories
    single<AuthRepository> { FirebaseAuthRepository(get(), get()) }
    single<UserRepository> { FirestoreUserRepository(get()) }
    single<SiteRepository> { FirestoreSiteRepository(get(), get()) }
    single<SpearRepository> { FirestoreSpearRepository(get()) }
    single<RouteRepository> { OsrmRouteRepository() }

    // ViewModels
    viewModelOf(::HomeViewModel)
    viewModelOf(::LoginViewModel)
    viewModelOf(::MapViewModel)
    viewModelOf(::ProfileViewModel)
    viewModelOf(::StoryListViewModel)
    viewModelOf(::ChapterDetailViewModel)
    viewModelOf(::GameChapterViewModel)
}

expect fun platformModule(): Module
