package com.loregate.app.data.repository

import com.loregate.app.data.seeder.FirestoreDataSeeder
import com.loregate.app.domain.model.ChapterContent
import com.loregate.app.domain.model.GeoPoint
import com.loregate.app.domain.model.HistoricalSite
import com.loregate.app.domain.repository.SiteRepository
import dev.gitlive.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

class FirestoreSiteRepository(
    private val firestore: FirebaseFirestore,
    private val dataSeeder: FirestoreDataSeeder
) : SiteRepository {

    private val sitesCollection = firestore.collection("historicalSites")
    private val chaptersCollection = firestore.collection("chapters")

    // In-memory cache for offline resilience
    private var cachedSites: List<HistoricalSite>? = null
    private val cachedChapters = mutableMapOf<String, ChapterContent>()

    override fun observeHistoricalSites(): Flow<List<HistoricalSite>> =
        sitesCollection.snapshots
            .map { snapshot ->
                val sites = snapshot.documents.mapNotNull { doc ->
                    runCatching { doc.data<HistoricalSite>() }.getOrNull()
                }
                if (sites.isNotEmpty()) {
                    cachedSites = sites
                    sites
                } else {
                    cachedSites ?: dataSeeder.getBundledFallbackSites()
                }
            }
            .catch {
                // If offline / network error, emit cached or bundled fallback sites
                emit(cachedSites ?: dataSeeder.getBundledFallbackSites())
            }

    override suspend fun getHistoricalSites(): Result<List<HistoricalSite>> = runCatching {
        try {
            val snapshot = sitesCollection.get()
            val sites = snapshot.documents.mapNotNull { doc ->
                runCatching { doc.data<HistoricalSite>() }.getOrNull()
            }
            if (sites.isNotEmpty()) {
                cachedSites = sites
                sites
            } else {
                cachedSites ?: dataSeeder.getBundledFallbackSites()
            }
        } catch (e: Exception) {
            // Offline fallback: never fail when exploring rural candi sites
            cachedSites ?: dataSeeder.getBundledFallbackSites()
        }
    }

    override suspend fun getSitesByStoryGroup(storyGroupId: String): Result<List<HistoricalSite>> =
        runCatching {
            try {
                val snapshot = sitesCollection
                    .where { "storyGroup" equalTo storyGroupId }
                    .get()
                val sites = snapshot.documents.mapNotNull { doc ->
                    runCatching { doc.data<HistoricalSite>() }.getOrNull()
                }
                if (sites.isNotEmpty()) {
                    sites
                } else {
                    (cachedSites ?: dataSeeder.getBundledFallbackSites())
                        .filter { it.storyGroup == storyGroupId }
                }
            } catch (e: Exception) {
                (cachedSites ?: dataSeeder.getBundledFallbackSites())
                    .filter { it.storyGroup == storyGroupId }
            }
        }

    override suspend fun getChapterContent(siteId: String): Result<ChapterContent> = runCatching {
        cachedChapters[siteId]?.let { return@runCatching it }

        try {
            val chapterDoc = chaptersCollection.document(siteId).get()
            if (chapterDoc.exists) {
                val content = chapterDoc.data<ChapterContent>()
                cachedChapters[siteId] = content
                content
            } else {
                val siteDoc = sitesCollection.document(siteId).get()
                val site = siteDoc.data<HistoricalSite>()
                val content = ChapterContent(
                    site = site,
                    heroImageUrl = site.imageUrl,
                    introduction = site.description,
                    conclusion = "Penjelajahan Anda di situs ini telah selesai."
                )
                cachedChapters[siteId] = content
                content
            }
        } catch (e: Exception) {
            // Offline fallback from data seeder
            val bundled = dataSeeder.getBundledFallbackChapter(siteId)
            if (bundled != null) {
                cachedChapters[siteId] = bundled
                bundled
            } else {
                throw e
            }
        }
    }

    override suspend fun seedInitialData(userLocation: GeoPoint?): Result<Unit> =
        dataSeeder.seedInitialData(userLocation)
}
