package com.loregate.app.domain.usecase

import com.loregate.app.domain.model.GeoPoint
import com.loregate.app.domain.model.HistoricalSite
import javax.inject.Inject

sealed interface SiteUnlockStatus {
    data object AlreadyUnlocked : SiteUnlockStatus
    data class LockedBySequence(val previousChapterOrder: Int) : SiteUnlockStatus
    data class ReadyToUnlock(val distanceMeters: Float) : SiteUnlockStatus
    data class OutOfRange(val distanceMeters: Float?, val requiredRadius: Float) : SiteUnlockStatus
}

class EvaluateSiteUnlockStateUseCase @Inject constructor() {

    /**
     * Filters which sites should be visible on the map:
     * - Sites that are not hidden
     * - Sites already unlocked by the user
     * - Hidden sites where the user is currently inside the discovery radius
     */
    fun filterVisibleSites(
        allSites: List<HistoricalSite>,
        unlockedSiteIds: Set<String>,
        userLocation: GeoPoint?
    ): List<HistoricalSite> {
        return allSites.filter { site ->
            val isUnlocked = unlockedSiteIds.contains(site.id)
            val isNotHidden = !site.isHidden
            val isInsideRadius = if (userLocation != null) {
                userLocation.distanceToMeters(site.geoPoint) <= site.radius
            } else {
                false
            }
            isNotHidden || isUnlocked || isInsideRadius
        }
    }

    /**
     * Evaluates whether a selected site can be viewed, unlocked now, is locked by chapter order,
     * or is out of physical range.
     */
    fun evaluateUnlockStatus(
        site: HistoricalSite,
        allSites: List<HistoricalSite>,
        unlockedSiteIds: Set<String>,
        userLocation: GeoPoint?
    ): SiteUnlockStatus {
        if (unlockedSiteIds.contains(site.id)) {
            return SiteUnlockStatus.AlreadyUnlocked
        }

        val isPreviousChapterUnlocked = if (site.chapterOrder <= 1) {
            true
        } else {
            val previousSite = allSites.find {
                it.storyGroup == site.storyGroup && it.chapterOrder == site.chapterOrder - 1
            }
            previousSite != null && unlockedSiteIds.contains(previousSite.id)
        }

        if (!isPreviousChapterUnlocked) {
            return SiteUnlockStatus.LockedBySequence(previousChapterOrder = site.chapterOrder - 1)
        }

        val distance = userLocation?.distanceToMeters(site.geoPoint)
        return if (distance != null && distance <= site.radius) {
            SiteUnlockStatus.ReadyToUnlock(distanceMeters = distance)
        } else {
            SiteUnlockStatus.OutOfRange(
                distanceMeters = distance,
                requiredRadius = site.radius
            )
        }
    }
}
