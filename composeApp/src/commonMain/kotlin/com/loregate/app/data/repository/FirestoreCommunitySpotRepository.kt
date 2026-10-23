package com.loregate.app.data.repository

import com.loregate.app.domain.model.CommunitySpot
import com.loregate.app.domain.model.CommunitySpotCategory
import com.loregate.app.domain.repository.CommunitySpotRepository
import com.loregate.app.domain.repository.UserRepository
import com.loregate.app.domain.service.MediaStorageService
import dev.gitlive.firebase.firestore.DocumentSnapshot
import dev.gitlive.firebase.firestore.FieldValue
import dev.gitlive.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlin.random.Random

data class PendingCommunityDraft(
    val spot: CommunitySpot,
    val imageBytes: ByteArray?
)

class FirestoreCommunitySpotRepository(
    private val firestore: FirebaseFirestore,
    private val mediaStorageService: MediaStorageService,
    private val userRepository: UserRepository
) : CommunitySpotRepository {

    private val spotsCollection = firestore.collection("communitySpots")
    private val pendingDrafts = mutableListOf<PendingCommunityDraft>()

    override fun observeCommunitySpots(): Flow<List<CommunitySpot>> =
        spotsCollection.snapshots
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    parseSpotDocument(doc)
                }
            }
            .catch { error ->
                println("observeCommunitySpots error: ${error.message}")
                emit(emptyList())
            }

    override suspend fun getCommunitySpots(): Result<List<CommunitySpot>> = runCatching {
        val snapshot = spotsCollection.get()
        snapshot.documents.mapNotNull { doc ->
            parseSpotDocument(doc)
        }
    }

    override suspend fun getCommunitySpotsBySiteId(siteId: String): Result<List<CommunitySpot>> =
        runCatching {
            val snapshot = spotsCollection
                .where { "targetSiteId" equalTo siteId }
                .get()
            snapshot.documents.mapNotNull { doc ->
                parseSpotDocument(doc)
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
                val uploadResult = mediaStorageService.uploadImage(
                    bytes = imageBytes,
                    folder = "community_spots",
                    fileName = "$spotId.jpg"
                ).getOrThrow()
                uploadedImageUrl = uploadResult.url
                uploadedThumbnailUrl = uploadResult.thumbnailUrl
            }

            val finalSpot = spot.copy(
                id = spotId,
                imageUrl = uploadedImageUrl,
                thumbnailUrl = uploadedThumbnailUrl,
                createdAt = if (spot.createdAt == 0L) currentTimeMillis() else spot.createdAt,
                updatedAt = currentTimeMillis()
            )

            // Save using clean map serialization to avoid dynamic reflection/serializer issues
            spotsCollection.document(spotId).set(finalSpot.toMap())
            println("CommunitySpot successfully written to Firestore with id: $spotId")

            // Reward author with 30 XP for contribution
            if (finalSpot.authorId.isNotBlank()) {
                runCatching {
                    userRepository.addExperiencePoints(finalSpot.authorId, 30)
                }
            }

            finalSpot
        } catch (e: Exception) {
            println("createCommunitySpot error: ${e.message}")
            pendingDrafts.add(PendingCommunityDraft(spot.copy(id = spotId), imageBytes))
            throw e
        }
    }

    override suspend fun updateCommunitySpot(spot: CommunitySpot): Result<Unit> = runCatching {
        val updatedSpot = spot.copy(updatedAt = currentTimeMillis())
        spotsCollection.document(spot.id).set(updatedSpot.toMap())
    }

    override suspend fun deleteCommunitySpot(spotId: String): Result<Unit> = runCatching {
        val doc = spotsCollection.document(spotId).get()
        if (doc.exists) {
            val spot = parseSpotDocument(doc)
            if (spot != null && spot.imageUrl.isNotBlank()) {
                runCatching {
                    mediaStorageService.deleteImage(spot.imageUrl)
                }
            }
        }
        spotsCollection.document(spotId).delete()
    }

    override suspend fun toggleHelpful(spotId: String, userId: String): Result<Boolean> = runCatching {
        val doc = spotsCollection.document(spotId).get()
        if (!doc.exists) return@runCatching false

        val spot = parseSpotDocument(doc) ?: return@runCatching false
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
            if (spot.authorId.isNotBlank() && spot.authorId != userId) {
                runCatching {
                    userRepository.addExperiencePoints(spot.authorId, 5)
                }
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

        val spot = parseSpotDocument(doc) ?: return@runCatching
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

    private fun parseSpotDocument(doc: DocumentSnapshot): CommunitySpot? {
        return runCatching {
            doc.data<CommunitySpot>()
        }.getOrElse {
            runCatching {
                val id: String = doc.get("id")
                val title: String = doc.get("title")
                val description: String = doc.get("description")
                val categoryStr: String = doc.get("category")
                val latitude: Double = doc.get("latitude")
                val longitude: Double = doc.get("longitude")
                val targetSiteId: String? = runCatching { doc.get<String?>("targetSiteId") }.getOrNull()
                val imageUrl: String = runCatching { doc.get<String>("imageUrl") }.getOrElse { "" }
                val thumbnailUrl: String = runCatching { doc.get<String>("thumbnailUrl") }.getOrElse { imageUrl }
                val authorId: String = runCatching { doc.get<String>("authorId") }.getOrElse { "" }
                val authorName: String = runCatching { doc.get<String>("authorName") }.getOrElse { "Pengelana" }
                val authorAvatarUrl: String? = runCatching { doc.get<String?>("authorAvatarUrl") }.getOrNull()
                val helpfulUserIds: List<String> = runCatching { doc.get<List<String>>("helpfulUserIds") }.getOrElse { emptyList() }
                val reportCount: Int = runCatching { (doc.get<Number>("reportCount")).toInt() }.getOrElse { 0 }
                val reportedByUserIds: List<String> = runCatching { doc.get<List<String>>("reportedByUserIds") }.getOrElse { emptyList() }
                val isVerifiedOnSite: Boolean = runCatching { doc.get<Boolean>("isVerifiedOnSite") }.getOrElse { false }
                val isAutoHidden: Boolean = runCatching { doc.get<Boolean>("isAutoHidden") }.getOrElse { false }
                val createdAt: Long = runCatching { (doc.get<Number>("createdAt")).toLong() }.getOrElse { 0L }
                val updatedAt: Long = runCatching { (doc.get<Number>("updatedAt")).toLong() }.getOrElse { 0L }

                CommunitySpot(
                    id = id,
                    title = title,
                    description = description,
                    category = CommunitySpotCategory.fromString(categoryStr),
                    latitude = latitude,
                    longitude = longitude,
                    targetSiteId = targetSiteId,
                    imageUrl = imageUrl,
                    thumbnailUrl = thumbnailUrl,
                    authorId = authorId,
                    authorName = authorName,
                    authorAvatarUrl = authorAvatarUrl,
                    helpfulUserIds = helpfulUserIds,
                    reportCount = reportCount,
                    reportedByUserIds = reportedByUserIds,
                    isVerifiedOnSite = isVerifiedOnSite,
                    isAutoHidden = isAutoHidden,
                    createdAt = createdAt,
                    updatedAt = updatedAt
                )
            }.getOrNull()
        }
    }

    private fun CommunitySpot.toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "title" to title,
        "description" to description,
        "category" to category.name,
        "latitude" to latitude,
        "longitude" to longitude,
        "targetSiteId" to targetSiteId,
        "imageUrl" to imageUrl,
        "thumbnailUrl" to thumbnailUrl,
        "authorId" to authorId,
        "authorName" to authorName,
        "authorAvatarUrl" to authorAvatarUrl,
        "helpfulUserIds" to helpfulUserIds,
        "reportCount" to reportCount,
        "reportedByUserIds" to reportedByUserIds,
        "isVerifiedOnSite" to isVerifiedOnSite,
        "isAutoHidden" to isAutoHidden,
        "createdAt" to createdAt,
        "updatedAt" to updatedAt
    )

    private fun generateSpotId(): String {
        val timestamp = currentTimeMillis()
        val randomSuffix = Random.nextInt(1000, 9999)
        return "spot_${timestamp}_$randomSuffix"
    }

    private fun currentTimeMillis(): Long {
        return Clock.System.now().toEpochMilliseconds()
    }
}
