package com.loregate.app.data.repository

import com.loregate.app.domain.model.AuthUser
import com.loregate.app.domain.model.UserAppData
import com.loregate.app.domain.repository.UserRepository
import dev.gitlive.firebase.firestore.FieldValue
import dev.gitlive.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FirestoreUserRepository(
    private val firestore: FirebaseFirestore
) : UserRepository {

    private val usersCollection = firestore.collection("users")

    override fun observeUserAppData(uid: String): Flow<UserAppData?> =
        usersCollection.document(uid).snapshots.map { snapshot ->
            if (snapshot.exists) {
                runCatching { snapshot.data<UserAppData>() }.getOrNull()
            } else {
                null
            }
        }

    override suspend fun getUserAppData(uid: String): Result<UserAppData?> = runCatching {
        val snapshot = usersCollection.document(uid).get()
        if (snapshot.exists) {
            snapshot.data<UserAppData>()
        } else {
            null
        }
    }

    override suspend fun createOrUpdateUserProfile(user: AuthUser): Result<Unit> = runCatching {
        val userRef = usersCollection.document(user.uid)
        val snapshot = userRef.get()

        if (snapshot.exists) {
            userRef.update(
                "displayName" to (user.displayName ?: ""),
                "email" to (user.email ?: ""),
                "photoUrl" to (user.photoUrl ?: "")
            )
        } else {
            val newUserData = UserAppData(
                uid = user.uid,
                displayName = user.displayName ?: "",
                email = user.email ?: "",
                photoUrl = user.photoUrl ?: ""
            )
            userRef.set(newUserData)
        }
    }

    override suspend fun unlockChapterAndVisitSite(uid: String, siteId: String): Result<Unit> =
        runCatching {
            val userRef = usersCollection.document(uid)
            val currentDoc = userRef.get()
            val currentData = if (currentDoc.exists) currentDoc.data<UserAppData>() else null

            val isAlreadyUnlocked = currentData?.unlockedChapters?.contains(siteId) ?: false
            val isAlreadyVisited = currentData?.visitedSites?.contains(siteId) ?: false

            val updates = mutableListOf<Pair<String, Any>>()
            var xpGain = 0

            if (!isAlreadyUnlocked) {
                updates.add("unlockedChapters" to FieldValue.arrayUnion(siteId))
                updates.add("unlockedChaptersCount" to FieldValue.increment(1))
                xpGain += 50
            }

            if (!isAlreadyVisited) {
                updates.add("visitedSites" to FieldValue.arrayUnion(siteId))
                updates.add("placesVisited" to FieldValue.increment(1))
                xpGain += 25
            }

            if (xpGain > 0) {
                updates.add("experiencePoints" to FieldValue.increment(xpGain))
            }

            if (updates.isNotEmpty()) {
                userRef.update(*updates.toTypedArray())
            }
        }

    override suspend fun addExperiencePoints(uid: String, xp: Int): Result<Unit> = runCatching {
        if (xp <= 0) return@runCatching
        val userRef = usersCollection.document(uid)
        val snapshot = userRef.get()
        if (snapshot.exists) {
            userRef.update("experiencePoints" to FieldValue.increment(xp))
        } else {
            userRef.set(UserAppData(uid = uid, experiencePoints = xp.toLong()))
        }
    }
}
