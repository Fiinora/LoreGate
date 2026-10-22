package com.loregate.app.feature.map

import com.loregate.app.domain.model.GeoPoint
import com.loregate.app.domain.model.HeritageStamp
import com.loregate.app.domain.model.HeritageTrail
import com.loregate.app.domain.model.HistoricalSite
import com.loregate.app.domain.model.Spear
import com.loregate.app.domain.usecase.SiteUnlockStatus

sealed interface MapSelection {
    data object None : MapSelection
    data class SiteSelected(
        val site: HistoricalSite,
        val showTopBanner: Boolean = true
    ) : MapSelection
    data class SpearSelected(val spear: Spear) : MapSelection
}

data class MapUiState(
    val isLoading: Boolean = true,
    val isSeedingData: Boolean = false,
    val errorMessage: String? = null,
    val isOfflineFallback: Boolean = false,
    val allSites: List<HistoricalSite> = emptyList(),
    val visibleSites: List<HistoricalSite> = emptyList(),
    val unlockedSiteIds: Set<String> = emptySet(),
    val userLocation: GeoPoint? = null,
    val isUserMoving: Boolean = false,
    val availableSpears: List<Spear> = emptyList(),
    val displaySpears: List<Spear> = emptyList(),
    val spearTotalCount: Int = 0,
    val collectedSpearCount: Int = 0,
    val isCollectingSpear: Boolean = false,
    val collectedSpearPopup: Spear? = null,
    val selection: MapSelection = MapSelection.None,
    val selectedSiteUnlockStatus: SiteUnlockStatus? = null,
    val focusedSpearDistanceMeters: Float? = null,
    val routePoints: List<GeoPoint> = emptyList(),
    val isRouteLoading: Boolean = false,
    val is25DMode: Boolean = true,
    val cameraTilt: Double = 50.0,
    val activeTrail: HeritageTrail? = null,
    val availableTrails: List<HeritageTrail> = emptyList(),
    val showTrailSelector: Boolean = false,
    val unlockedStampCelebration: HeritageStamp? = null
) {
    val selectedSite: HistoricalSite?
        get() = (selection as? MapSelection.SiteSelected)?.site

    val showTopMarkerCard: Boolean
        get() = (selection as? MapSelection.SiteSelected)?.showTopBanner == true

    val focusedSpear: Spear?
        get() = (selection as? MapSelection.SpearSelected)?.spear
}

sealed interface MapUiEvent {
    data object OnLocationPermissionGranted : MapUiEvent
    data object OnBackgroundLocationPermissionGranted : MapUiEvent
    data class OnFocusTargetSite(val targetSiteId: String) : MapUiEvent
    data class OnActivateTrail(val trailId: String) : MapUiEvent
    data object OnExitTrail : MapUiEvent
    data class OnToggleTrailSelector(val show: Boolean) : MapUiEvent
    data object OnDismissStampCelebration : MapUiEvent
    data object OnRetryFetchSites : MapUiEvent
    data object OnSeedInitialDataClicked : MapUiEvent
    data class OnSiteMarkerClicked(val siteId: String) : MapUiEvent
    data class OnSpearMarkerClicked(val spearId: String) : MapUiEvent
    data object OnMapEmptyClicked : MapUiEvent
    data object OnDismissSiteSelection : MapUiEvent
    data object OnDismissSpearSelection : MapUiEvent
    data object OnDismissCollectionPopup : MapUiEvent
    data object OnPrimarySiteActionClicked : MapUiEvent
    data object OnGetDirectionsClicked : MapUiEvent
    data class OnCollectSpearClicked(val spearId: String) : MapUiEvent
    data object OnToggle25DModeClicked : MapUiEvent
    data object OnZoomInClicked : MapUiEvent
    data object OnZoomOutClicked : MapUiEvent
    data object OnMyLocationClicked : MapUiEvent
    data object OnProfileClicked : MapUiEvent
    data object OnBackClicked : MapUiEvent
}

sealed interface MapUiEffect {
    data class AnimateCamera(
        val target: GeoPoint,
        val zoom: Double,
        val tilt: Double,
        val bearing: Double = 0.0,
        val durationMs: Int = 1000
    ) : MapUiEffect

    data class MoveCameraTracking(
        val target: GeoPoint,
        val tilt: Double
    ) : MapUiEffect

    data class ZoomBy(val delta: Double) : MapUiEffect
    data class NavigateToChapterDetail(val siteId: String) : MapUiEffect
    data object NavigateToProfile : MapUiEffect
    data object NavigateBack : MapUiEffect
}
