package com.loregate.app.domain.repository

import com.loregate.app.domain.model.AuthUser
import com.loregate.app.domain.model.ChapterContent
import com.loregate.app.domain.model.GeoPoint
import com.loregate.app.domain.model.HistoricalSite
import com.loregate.app.domain.model.SpearSnapshot
import com.loregate.app.domain.model.UserAppData
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val currentUser: Flow<AuthUser?>
    fun getCurrentUser(): AuthUser?
    suspend fun signInWithEmail(email: String, password: String): Result<AuthUser>
    suspend fun signUpWithEmail(email: String, password: String): Result<AuthUser>
    suspend fun signInWithGoogleIdToken(idToken: String): Result<AuthUser>
    suspend fun signOut()
}

interface UserRepository {
    fun observeUserAppData(uid: String): Flow<UserAppData?>
    suspend fun getUserAppData(uid: String): Result<UserAppData?>
    suspend fun createOrUpdateUserProfile(user: AuthUser): Result<Unit>
    suspend fun unlockChapterAndVisitSite(uid: String, siteId: String): Result<Unit>
}

interface SiteRepository {
    fun observeHistoricalSites(): Flow<List<HistoricalSite>>
    suspend fun getHistoricalSites(): Result<List<HistoricalSite>>
    suspend fun getSitesByStoryGroup(storyGroupId: String): Result<List<HistoricalSite>>
    suspend fun getChapterContent(siteId: String): Result<ChapterContent>
    suspend fun seedInitialData(userLocation: GeoPoint? = null): Result<Unit>
}

interface SpearRepository {
    fun observeSpears(): Flow<SpearSnapshot>
    suspend fun getCollectedSpearCount(uid: String): Result<Int>
    suspend fun collectSpear(uid: String, spearId: String): Result<Unit>
}

interface RouteRepository {
    suspend fun getWalkingRoute(start: GeoPoint, end: GeoPoint): Result<List<GeoPoint>>
}
