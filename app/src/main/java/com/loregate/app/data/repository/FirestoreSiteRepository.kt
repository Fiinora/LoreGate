package com.loregate.app.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.loregate.app.data.seeder.FirestoreDataSeeder
import com.loregate.app.domain.model.ChapterContent
import com.loregate.app.domain.model.GeoPoint
import com.loregate.app.domain.model.HistoricalSite
import com.loregate.app.domain.repository.SiteRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

@Singleton
class FirestoreSiteRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val dataSeeder: FirestoreDataSeeder
) : SiteRepository {

    private val sitesCollection = firestore.collection("historicalSites")
    private val chaptersCollection = firestore.collection("chapters")

    override fun observeHistoricalSites(): Flow<List<HistoricalSite>> = callbackFlow {
        val registration = sitesCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("FirestoreSiteRepo", "Error listening to historicalSites", error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val sites = snapshot.toObjects(HistoricalSite::class.java)
                trySend(sites)
            }
        }
        awaitClose {
            registration.remove()
        }
    }

    override suspend fun getHistoricalSites(): Result<List<HistoricalSite>> = runCatching {
        val snapshot = sitesCollection.get().await()
        snapshot.toObjects(HistoricalSite::class.java)
    }

    override suspend fun getSitesByStoryGroup(storyGroupId: String): Result<List<HistoricalSite>> =
        runCatching {
            val snapshot = sitesCollection
                .whereEqualTo("storyGroup", storyGroupId)
                .get()
                .await()
            snapshot.toObjects(HistoricalSite::class.java)
        }

    override suspend fun getChapterContent(siteId: String): Result<ChapterContent> = runCatching {
        require(siteId.isNotBlank()) { "Site ID is missing." }
        val document = chaptersCollection.document(siteId).get().await()
        if (!document.exists()) {
            throw NoSuchElementException("Chapter not found.")
        }
        document.toObject(ChapterContent::class.java)
            ?: throw IllegalStateException("Failed to parse chapter data.")
    }

    override suspend fun seedInitialData(userLocation: GeoPoint?): Result<Unit> {
        return dataSeeder.seedInitialData(userLocation)
    }
}
