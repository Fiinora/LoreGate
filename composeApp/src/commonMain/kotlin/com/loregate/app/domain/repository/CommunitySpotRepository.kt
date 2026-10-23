package com.loregate.app.domain.repository

import com.loregate.app.domain.model.CommunitySpot
import kotlinx.coroutines.flow.Flow

interface CommunitySpotRepository {
    fun observeCommunitySpots(): Flow<List<CommunitySpot>>
    suspend fun getCommunitySpots(): Result<List<CommunitySpot>>
    suspend fun getCommunitySpotsBySiteId(siteId: String): Result<List<CommunitySpot>>
    suspend fun createCommunitySpot(spot: CommunitySpot, imageBytes: ByteArray?): Result<CommunitySpot>
    suspend fun updateCommunitySpot(spot: CommunitySpot): Result<Unit>
    suspend fun deleteCommunitySpot(spotId: String): Result<Unit>
    suspend fun toggleHelpful(spotId: String, userId: String): Result<Boolean>
    suspend fun reportSpot(spotId: String, userId: String, reason: String): Result<Unit>
    suspend fun syncPendingDrafts(): Result<Int>
}
