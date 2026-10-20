package com.loregate.app.data.repository

import com.loregate.app.domain.model.Spear
import com.loregate.app.domain.model.SpearSnapshot
import com.loregate.app.domain.repository.SpearRepository
import dev.gitlive.firebase.firestore.FieldValue
import dev.gitlive.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
class FirestoreSpearRepository(
    private val firestore: FirebaseFirestore
) : SpearRepository {

    private val spearsCollection = firestore.collection("spears")
    private val usersCollection = firestore.collection("users")
    private val spearClaimsCollection = firestore.collection("spearCollections")

    override fun observeSpears(): Flow<SpearSnapshot> =
        spearsCollection.snapshots.map { snapshot ->
            val fetchedSpears = snapshot.documents.mapNotNull { doc ->
                runCatching { doc.data<Spear>() }.getOrNull()?.copy(
                    id = doc.id
                )
            }
            val activeSpears = fetchedSpears.filter { it.isActive && !it.isCollected }
            SpearSnapshot(
                totalCount = fetchedSpears.size,
                availableSpears = activeSpears
            )
        }

    override suspend fun getCollectedSpearCount(uid: String): Result<Int> = runCatching {
        val snapshot = spearClaimsCollection
            .where { "userId" equalTo uid }
            .get()
        snapshot.documents.size
    }

    override suspend fun collectSpear(uid: String, spearId: String): Result<Unit> = runCatching {
        val spearRef = spearsCollection.document(spearId)
        val userRef = usersCollection.document(uid)
        val claimId = "${uid}_$spearId"
        val claimRef = spearClaimsCollection.document(claimId)

        val spearSnapshot = spearRef.get()
        val currentSpear = if (spearSnapshot.exists) spearSnapshot.data<Spear>() else null
        if (currentSpear == null || currentSpear.isCollected) {
            throw IllegalStateException("Spear already collected or not found.")
        }

        spearRef.update(
            "isCollected" to true,
            "isActive" to false,
            "collectedByUserId" to uid,
            "collectedAt" to FieldValue.serverTimestamp
        )

        userRef.update(
            "collectedSpears" to FieldValue.arrayUnion(spearId),
            "experiencePoints" to FieldValue.increment(currentSpear.xpReward)
        )

        claimRef.set(
            mapOf(
                "id" to claimId,
                "spearId" to spearId,
                "userId" to uid,
                "collectedAt" to FieldValue.serverTimestamp,
                "difficulty" to currentSpear.difficulty,
                "xpReward" to currentSpear.xpReward,
                "latitude" to currentSpear.latitude,
                "longitude" to currentSpear.longitude
            )
        )
    }
}
