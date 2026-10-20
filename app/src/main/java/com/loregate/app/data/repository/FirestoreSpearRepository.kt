package com.loregate.app.data.repository

import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.loregate.app.domain.model.Spear
import com.loregate.app.domain.model.SpearSnapshot
import com.loregate.app.domain.repository.SpearRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

@Singleton
class FirestoreSpearRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) : SpearRepository {

    private val spearsCollection = firestore.collection("spears")
    private val usersCollection = firestore.collection("users")
    private val spearClaimsCollection = firestore.collection("spearCollections")

    override fun observeSpears(): Flow<SpearSnapshot> = callbackFlow {
        val registration = spearsCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("FirestoreSpearRepo", "Spear listener error", error)
                return@addSnapshotListener
            }

            val documents = snapshot?.documents.orEmpty()
            val fetchedSpears = documents.mapNotNull { doc ->
                val base = runCatching { doc.toObject(Spear::class.java) }.getOrNull()
                base?.copy(
                    id = base.id.ifEmpty { doc.id },
                    isActive = doc.getBoolean("isActive") ?: doc.getBoolean("active") ?: base.isActive,
                    isCollected = doc.getBoolean("isCollected") ?: doc.getBoolean("collected") ?: base.isCollected,
                    collectedByUserId = doc.getString("collectedByUserId") ?: base.collectedByUserId,
                    collectedAt = doc.getLong("collectedAt") ?: base.collectedAt
                )
            }

            val activeSpears = fetchedSpears.filter { it.isActive && !it.isCollected }
            trySend(
                SpearSnapshot(
                    totalCount = fetchedSpears.size,
                    availableSpears = activeSpears
                )
            )
        }
        awaitClose {
            registration.remove()
        }
    }

    override suspend fun getCollectedSpearCount(uid: String): Result<Int> = runCatching {
        val snapshot = spearClaimsCollection
            .whereEqualTo("userId", uid)
            .get()
            .await()
        snapshot.size()
    }

    override suspend fun collectSpear(uid: String, spearId: String): Result<Unit> = runCatching {
        firestore.runTransaction { transaction ->
            val spearRef = spearsCollection.document(spearId)
            val userRef = usersCollection.document(uid)
            val claimId = "${uid}_$spearId"
            val claimRef = spearClaimsCollection.document(claimId)

            val spearSnapshot = transaction.get(spearRef)
            val currentSpear = spearSnapshot.toObject(Spear::class.java)
            val isAlreadyCollected = spearSnapshot.getBoolean("isCollected")
                ?: spearSnapshot.getBoolean("collected")
                ?: currentSpear?.isCollected
                ?: false

            if (currentSpear == null || isAlreadyCollected) {
                throw IllegalStateException("Spear already collected or not found.")
            }

            transaction.update(
                spearRef,
                mapOf(
                    "isCollected" to true,
                    "collected" to true,
                    "active" to false,
                    "isActive" to false,
                    "collectedByUserId" to uid,
                    "collectedAt" to System.currentTimeMillis()
                )
            )

            transaction.update(
                userRef,
                "collectedSpears", FieldValue.arrayUnion(spearId),
                "experiencePoints", FieldValue.increment(currentSpear.xpReward.toLong())
            )

            transaction.set(
                claimRef,
                mapOf(
                    "id" to claimId,
                    "spearId" to spearId,
                    "userId" to uid,
                    "collectedAt" to System.currentTimeMillis(),
                    "difficulty" to currentSpear.difficulty,
                    "xpReward" to currentSpear.xpReward,
                    "latitude" to currentSpear.latitude,
                    "longitude" to currentSpear.longitude
                )
            )
            null
        }.await()
    }
}
