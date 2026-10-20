package com.loregate.app.data.repository

import com.loregate.app.data.seeder.FirestoreDataSeeder
import com.loregate.app.domain.model.ChapterContent
import com.loregate.app.domain.model.GeoPoint
import com.loregate.app.domain.model.HistoricalSite
import com.loregate.app.domain.repository.SiteRepository
import dev.gitlive.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FirestoreSiteRepository(
    private val firestore: FirebaseFirestore,
    private val dataSeeder: FirestoreDataSeeder
) : SiteRepository {

    private val sitesCollection = firestore.collection("historicalSites")
    private val chaptersCollection = firestore.collection("chapters")

    override fun observeHistoricalSites(): Flow<List<HistoricalSite>> =
        sitesCollection.snapshots.map { snapshot ->
            snapshot.documents.mapNotNull { doc ->
                runCatching { doc.data<HistoricalSite>() }.getOrNull()
            }
        }

    override suspend fun getHistoricalSites(): Result<List<HistoricalSite>> = runCatching {
        val snapshot = sitesCollection.get()
        snapshot.documents.mapNotNull { doc ->
            runCatching { doc.data<HistoricalSite>() }.getOrNull()
        }
    }

    override suspend fun getSitesByStoryGroup(storyGroupId: String): Result<List<HistoricalSite>> =
        runCatching {
            val snapshot = sitesCollection
                .where { "storyGroup" equalTo storyGroupId }
                .get()
            snapshot.documents.mapNotNull { doc ->
                runCatching { doc.data<HistoricalSite>() }.getOrNull()
            }
        }

    override suspend fun getChapterContent(siteId: String): Result<ChapterContent> = runCatching {
        val chapterDoc = chaptersCollection.document(siteId).get()
        if (chapterDoc.exists) {
            chapterDoc.data<ChapterContent>()
        } else {
            val siteDoc = sitesCollection.document(siteId).get()
            val site = siteDoc.data<HistoricalSite>()
            ChapterContent(
                site = site,
                heroImageUrl = site.imageUrl,
                introduction = site.description,
                conclusion = "Penjelajahan Anda di situs ini telah selesai."
            )
        }
    }

    override suspend fun seedInitialData(userLocation: GeoPoint?): Result<Unit> =
        dataSeeder.seedInitialData(userLocation)
}
