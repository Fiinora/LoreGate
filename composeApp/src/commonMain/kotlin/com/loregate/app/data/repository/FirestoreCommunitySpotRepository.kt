package com.loregate.app.data.repository

import com.loregate.app.domain.model.CommunitySpot
import com.loregate.app.domain.repository.CommunitySpotRepository
import com.loregate.app.domain.repository.UserRepository
import dev.gitlive.firebase.firestore.FieldValue
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.storage.Data
import dev.gitlive.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlin.random.Random

data class PendingCommunityDraft(
    val spot: CommunitySpot,
    val imageBytes: ByteArray?
)

class FirestoreCommunitySpotRepository(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage,
    private val userRepository: UserRepository
) : CommunitySpotRepository {

    private val spotsCollection = firestore.collection("communitySpots")
    private val pendingDrafts = mutableListOf<PendingCommunityDraft>()

    override fun observeCommunitySpots(): Flow<List<CommunitySpot>> =
        spotsCollection.snapshots
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    runCatching { doc.data<CommunitySpot>() }.getOrNull()
                }
            }
            .catch {
                emit(emptyList())
            }

    override suspend fun getCommunitySpots(): Result<List<CommunitySpot>> = runCatching {
        val snapshot = spotsCollection.get()
        snapshot.documents.mapNotNull { doc ->
            runCatching { doc.data<CommunitySpot>() }.getOrNull()
        }
    }

    override suspend fun getCommunitySpotsBySiteId(siteId: String): Result<List<CommunitySpot>> =
        runCatching {
            val snapshot = spotsCollection
                .where { "targetSiteId" equalTo siteId }
                .get()
            snapshot.documents.mapNotNull { doc ->
                runCatching { doc.data<CommunitySpot>() }.getOrNull()
            }
        }

    override suspend fun createCommunitySpot(
        spot: CommunitySpot,
        imageBytes: ByteArray?
    ): Result<CommunitySpot> = runCatching {
        val spotId = if (spot.id.isNotBlank()) spot.id else generateSpotId()
        var uploadedImageUrl = spot.imageUrl
        var uploadedThumbnailUrl = spot.thumbnailUrl

        try {
            if (imageBytes != null && imageBytes.isNotEmpty()) {
                val imagePath = "community_spots/${spot.authorId}/$spotId.jpg"
                val storageRef = storage.reference(imagePath)
                storageRef.putData(Data(imageBytes))
                uploadedImageUrl = storageRef.getDownloadUrl()
                uploadedThumbnailUrl = uploadedImageUrl
            }

            val finalSpot = spot.copy(
                id = spotId,
                imageUrl = uploadedImageUrl,
                thumbnailUrl = uploadedThumbnailUrl,
                createdAt = if (spot.createdAt == 0L) currentTimeMillis() else spot.createdAt,
                updatedAt = currentTimeMillis()
            )

            spotsCollection.document(spotId).set(finalSpot)

            // Reward author with 30 XP for contribution
            if (finalSpot.authorId.isNotBlank()) {
                userRepository.addExperiencePoints(finalSpot.authorId, 30)
            }

            finalSpot
        } catch (e: Exception) {
            // If offline, queue to pending drafts for background resilience
            pendingDrafts.add(PendingCommunityDraft(spot.copy(id = spotId), imageBytes))
            throw e
        }
    }

    override suspend fun updateCommunitySpot(spot: CommunitySpot): Result<Unit> = runCatching {
        val updatedSpot = spot.copy(updatedAt = currentTimeMillis())
        spotsCollection.document(spot.id).set(updatedSpot)
    }

    override suspend fun deleteCommunitySpot(spotId: String): Result<Unit> = runCatching {
        val doc = spotsCollection.document(spotId).get()
        if (doc.exists) {
            val spot = runCatching { doc.data<CommunitySpot>() }.getOrNull()
            if (spot != null && spot.imageUrl.isNotBlank() && spot.authorId.isNotBlank()) {
                runCatching {
                    val storageRef = storage.reference("community_spots/${spot.authorId}/$spotId.jpg")
                    storageRef.delete()
                }
            }
        }
        spotsCollection.document(spotId).delete()
    }

    override suspend fun toggleHelpful(spotId: String, userId: String): Result<Boolean> = runCatching {
        val doc = spotsCollection.document(spotId).get()
        if (!doc.exists) return@runCatching false

        val spot = doc.data<CommunitySpot>()
        val hasLiked = spot.helpfulUserIds.contains(userId)

        if (hasLiked) {
            spotsCollection.document(spotId).update(
                "helpfulUserIds" to FieldValue.arrayRemove(userId)
            )
            false
        } else {
            spotsCollection.document(spotId).update(
                "helpfulUserIds" to FieldValue.arrayUnion(userId)
            )
            // Reward author with +5 XP for helpful tip
            if (spot.authorId.isNotBlank() && spot.authorId != userId) {
                userRepository.addExperiencePoints(spot.authorId, 5)
            }
            true
        }
    }

    override suspend fun reportSpot(
        spotId: String,
        userId: String,
        reason: String
    ): Result<Unit> = runCatching {
        val doc = spotsCollection.document(spotId).get()
        if (!doc.exists) return@runCatching

        val spot = doc.data<CommunitySpot>()
        if (spot.reportedByUserIds.contains(userId)) return@runCatching

        val newReportCount = spot.reportCount + 1
        val shouldAutoHide = newReportCount >= 3

        spotsCollection.document(spotId).update(
            "reportCount" to FieldValue.increment(1),
            "reportedByUserIds" to FieldValue.arrayUnion(userId),
            "isAutoHidden" to shouldAutoHide
        )
    }

    override suspend fun syncPendingDrafts(): Result<Int> = runCatching {
        if (pendingDrafts.isEmpty()) return@runCatching 0

        var syncedCount = 0
        val iterator = pendingDrafts.iterator()
        while (iterator.hasNext()) {
            val draft = iterator.next()
            val result = createCommunitySpot(draft.spot, draft.imageBytes)
            if (result.isSuccess) {
                iterator.remove()
                syncedCount++
            }
        }
        syncedCount
    }

    private fun generateSpotId(): String {
        val timestamp = currentTimeMillis()
        val randomSuffix = Random.nextInt(1000, 9999)
        return "spot_${timestamp}_$randomSuffix"
    }

    private fun currentTimeMillis(): Long {
        return kotlin.time.TimeSource.Monotonic.markNow().elapsedNow().inWholeMilliseconds + 1729000000000L
    }
}
