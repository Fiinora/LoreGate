package com.loregate.app

import com.loregate.app.domain.model.GeoPoint
import com.loregate.app.domain.model.HistoricalSite
import com.loregate.app.domain.model.SiteType
import com.loregate.app.domain.usecase.EvaluateSiteUnlockStateUseCase
import com.loregate.app.domain.usecase.SiteUnlockStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DomainAndMviUnitTest {

    private val evaluateSiteUseCase = EvaluateSiteUnlockStateUseCase()

    @Test
    fun `filterVisibleSites hides secret site when locked and outside radius`() {
        val normalSite = HistoricalSite(
            id = "SITE_1",
            nama = "Candi Singosari",
            latitude = -7.8878,
            longitude = 112.6639,
            radius = 100f,
            isHidden = false
        )
        val secretSite = HistoricalSite(
            id = "SECRET_1",
            nama = "Situs Rahasia",
            latitude = -7.9000,
            longitude = 112.7000,
            radius = 50f,
            isHidden = true
        )

        val userFarAway = GeoPoint(-7.8878, 112.6639)
        val visibleWhenFar = evaluateSiteUseCase.filterVisibleSites(
            allSites = listOf(normalSite, secretSite),
            unlockedSiteIds = emptySet(),
            userLocation = userFarAway
        )

        assertEquals(listOf(normalSite), visibleWhenFar)

        val userInsideSecretRadius = GeoPoint(-7.9000, 112.7000)
        val visibleWhenNear = evaluateSiteUseCase.filterVisibleSites(
            allSites = listOf(normalSite, secretSite),
            unlockedSiteIds = emptySet(),
            userLocation = userInsideSecretRadius
        )

        assertEquals(2, visibleWhenNear.size)
    }

    @Test
    fun `evaluateUnlockStatus enforces sequential chapter order and range`() {
        val ch1 = HistoricalSite(
            id = "CH_1",
            nama = "Chapter 1",
            latitude = -7.8878,
            longitude = 112.6639,
            radius = 100f,
            type = SiteType.TEMPLE,
            storyGroup = "Wangsa Rajasa",
            chapterOrder = 1
        )
        val ch2 = HistoricalSite(
            id = "CH_2",
            nama = "Chapter 2",
            latitude = -7.8900,
            longitude = 112.6700,
            radius = 100f,
            type = SiteType.TEMPLE,
            storyGroup = "Wangsa Rajasa",
            chapterOrder = 2
        )
        val allSites = listOf(ch1, ch2)

        // Trying to unlock Chapter 2 before Chapter 1 is unlocked
        val lockedStatus = evaluateSiteUseCase.evaluateUnlockStatus(
            site = ch2,
            allSites = allSites,
            unlockedSiteIds = emptySet(),
            userLocation = ch2.geoPoint
        )
        assertEquals(SiteUnlockStatus.LockedBySequence(1), lockedStatus)

        // Chapter 1 unlocked, user at Chapter 2 location -> ReadyToUnlock
        val readyStatus = evaluateSiteUseCase.evaluateUnlockStatus(
            site = ch2,
            allSites = allSites,
            unlockedSiteIds = setOf("CH_1"),
            userLocation = ch2.geoPoint
        )
        assertTrue(readyStatus is SiteUnlockStatus.ReadyToUnlock)

        // Already unlocked -> AlreadyUnlocked regardless of user location
        val viewedStatus = evaluateSiteUseCase.evaluateUnlockStatus(
            site = ch2,
            allSites = allSites,
            unlockedSiteIds = setOf("CH_1", "CH_2"),
            userLocation = null
        )
        assertEquals(SiteUnlockStatus.AlreadyUnlocked, viewedStatus)
    }
}
