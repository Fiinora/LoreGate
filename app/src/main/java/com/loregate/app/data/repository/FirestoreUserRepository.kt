package com.loregate.app.data.repository

import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.loregate.app.domain.model.AuthUser
import com.loregate.app.domain.model.UserAppData
import com.loregate.app.domain.repository.UserRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

@Singleton
class FirestoreUserRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) : UserRepository {

    private val usersCollection = firestore.collection("users")

    override fun observeUserAppData(uid: String): Flow<UserAppData?> = callbackFlow {
        val registration = usersCollection.document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("FirestoreUserRepo", "Error observing user data", error)
                    return@addSnapshotListener
                }
                val data = if (snapshot != null && snapshot.exists()) {
                    runCatching { snapshot.toObject(UserAppData::class.java) }.getOrNull()
                } else {
                    null
                }
                trySend(data)
            }
        awaitClose {
            registration.remove()
        }
    }

    override suspend fun getUserAppData(uid: String): Result<UserAppData?> = runCatching {
        val snapshot = usersCollection.document(uid).get().await()
        if (snapshot.exists()) {
            snapshot.toObject(UserAppData::class.java)
        } else {
            null
        }
    }

    override suspend fun createOrUpdateUserProfile(user: AuthUser): Result<Unit> = runCatching {
        val userRef = usersCollection.document(user.uid)
        val snapshot = userRef.get().await()

        if (snapshot.exists()) {
            val updateData = hashMapOf<String, Any>(
                "displayName" to (user.displayName ?: ""),
                "email" to (user.email ?: ""),
                "photoUrl" to (user.photoUrl ?: ""),
                "lastLogin" to FieldValue.serverTimestamp()
            )
            userRef.update(updateData).await()
        } else {
            val newUserData = hashMapOf<String, Any>(
                "uid" to user.uid,
                "displayName" to (user.displayName ?: ""),
                "email" to (user.email ?: ""),
                "photoUrl" to (user.photoUrl ?: ""),
                "unlockedChapters" to listOf<String>(),
                "totalChapters" to 50L,
                "unlockedChaptersCount" to 0L,
                "visitedSites" to listOf<String>(),
                "placesVisited" to 0L,
                "experiencePoints" to 0L,
                "level" to 1L,
                "achievements" to listOf<String>(),
                "accountCreated" to FieldValue.serverTimestamp(),
                "lastLogin" to FieldValue.serverTimestamp(),
                "notificationsEnabled" to true,
                "language" to "en"
            )
            userRef.set(newUserData).await()
        }
    }

    override suspend fun unlockChapterAndVisitSite(uid: String, siteId: String): Result<Unit> =
        runCatching {
            val userRef = usersCollection.document(uid)
            val currentDoc = userRef.get().await()
            val currentData = currentDoc.toObject(UserAppData::class.java)

            val isAlreadyUnlocked = currentData?.unlockedChapters?.contains(siteId) ?: false
            val isAlreadyVisited = currentData?.visitedSites?.contains(siteId) ?: false

            val updates = mutableMapOf<String, Any>()
            var xpGain = 0L

            if (!isAlreadyUnlocked) {
                updates["unlockedChapters"] = FieldValue.arrayUnion(siteId)
                updates["unlockedChaptersCount"] = FieldValue.increment(1)
                xpGain += 50L
            }

            if (!isAlreadyVisited) {
                updates["visitedSites"] = FieldValue.arrayUnion(siteId)
                updates["placesVisited"] = FieldValue.increment(1)
                xpGain += 25L
            }

            if (xpGain > 0L) {
                updates["experiencePoints"] = FieldValue.increment(xpGain)
            }

            if (updates.isNotEmpty()) {
                userRef.update(updates).await()
            }
        }
}
