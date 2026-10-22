package com.loregate.app.feature.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loregate.app.core.location.GeofenceManager
import com.loregate.app.core.location.LocationTracker
import com.loregate.app.domain.model.GeoPoint
import com.loregate.app.domain.model.HistoricalSite
import com.loregate.app.domain.model.Spear
import com.loregate.app.domain.repository.AuthRepository
import com.loregate.app.domain.repository.HeritageTrailRepository
import com.loregate.app.domain.repository.PassportRepository
import com.loregate.app.domain.repository.RouteRepository
import com.loregate.app.domain.repository.SiteRepository
import com.loregate.app.domain.repository.SpearRepository
import com.loregate.app.domain.repository.UserRepository
import com.loregate.app.domain.usecase.EvaluateSiteUnlockStateUseCase
import com.loregate.app.domain.usecase.SiteUnlockStatus
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MapViewModel(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val siteRepository: SiteRepository,
    private val spearRepository: SpearRepository,
    private val routeRepository: RouteRepository,
    private val heritageTrailRepository: HeritageTrailRepository,
    private val passportRepository: PassportRepository,
    private val locationTracker: LocationTracker,
    private val geofenceManager: GeofenceManager,
    private val evaluateSiteUnlockStateUseCase: EvaluateSiteUnlockStateUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    private val _uiEffect = Channel<MapUiEffect>(Channel.BUFFERED)
    val uiEffect = _uiEffect.receiveAsFlow()

    private var locationJob: Job? = null
    private var hasCenteredOnce = false
    private var pendingTargetSiteId: String? = null
    private var pendingInitialTrailId: String? = null

    init {
        fetchHistoricalSites()
        observeRealtimeSpears()
        refreshUserSpearCount()
        loadAvailableTrails()
    }

    fun onEvent(event: MapUiEvent) {
        when (event) {
            MapUiEvent.OnLocationPermissionGranted -> startObservingLocation()
            MapUiEvent.OnBackgroundLocationPermissionGranted -> {
                geofenceManager.registerSitesGeofences(_uiState.value.allSites)
            }
            is MapUiEvent.OnFocusTargetSite -> {
                pendingTargetSiteId = event.targetSiteId
                tryFocusPendingTargetSite()
            }
            is MapUiEvent.OnActivateTrail -> activateTrail(event.trailId)
            MapUiEvent.OnExitTrail -> exitTrail()
            is MapUiEvent.OnToggleTrailSelector -> {
                _uiState.update { it.copy(showTrailSelector = event.show) }
            }
            MapUiEvent.OnDismissStampCelebration -> handleDismissStampCelebration()
            MapUiEvent.OnRetryFetchSites -> fetchHistoricalSites()
            MapUiEvent.OnSeedInitialDataClicked -> seedInitialFirestoreData()
            is MapUiEvent.OnSiteMarkerClicked -> selectSiteById(event.siteId)
            is MapUiEvent.OnSpearMarkerClicked -> selectSpearById(event.spearId)
            MapUiEvent.OnMapEmptyClicked -> clearSelection()
            MapUiEvent.OnDismissSiteSelection -> clearSelection()
            MapUiEvent.OnDismissSpearSelection -> clearSelection()
            MapUiEvent.OnDismissCollectionPopup -> {
                _uiState.update { it.copy(collectedSpearPopup = null) }
            }
            MapUiEvent.OnPrimarySiteActionClicked -> handlePrimarySiteAction()
            MapUiEvent.OnGetDirectionsClicked -> calculateWalkingRoute()
            is MapUiEvent.OnCollectSpearClicked -> collectSpear(event.spearId)
            MapUiEvent.OnToggle25DModeClicked -> toggle25DMode()
            MapUiEvent.OnZoomInClicked -> sendEffect(MapUiEffect.ZoomBy(1.0))
            MapUiEvent.OnZoomOutClicked -> sendEffect(MapUiEffect.ZoomBy(-1.0))
            MapUiEvent.OnMyLocationClicked -> centerOnUserLocation()
            MapUiEvent.OnProfileClicked -> sendEffect(MapUiEffect.NavigateToProfile)
            MapUiEvent.OnBackClicked -> sendEffect(MapUiEffect.NavigateBack)
        }
    }

    fun setInitialTrail(trailId: String?) {
        if (!trailId.isNullOrBlank()) {
            pendingInitialTrailId = trailId
            if (_uiState.value.allSites.isNotEmpty()) {
                activateTrail(trailId)
            }
        }
    }

    private fun loadAvailableTrails() {
        viewModelScope.launch {
            heritageTrailRepository.getHeritageTrails().onSuccess { trails ->
                _uiState.update { it.copy(availableTrails = trails) }
                pendingInitialTrailId?.let { trailId ->
                    activateTrail(trailId)
                    pendingInitialTrailId = null
                }
            }
        }
    }

    private fun activateTrail(trailId: String) {
        viewModelScope.launch {
            val trail = _uiState.value.availableTrails.find { it.id == trailId }
                ?: heritageTrailRepository.getTrailById(trailId).getOrNull()
                ?: return@launch

            _uiState.update {
                it.copy(
                    activeTrail = trail,
                    showTrailSelector = false,
                    isRouteLoading = true
                )
            }

            heritageTrailRepository.calculateTrailFullRoute(trail, _uiState.value.allSites)
                .onSuccess { points ->
                    _uiState.update { it.copy(routePoints = points, isRouteLoading = false) }
                }
                .onFailure {
                    _uiState.update { it.copy(isRouteLoading = false) }
                }

            val firstSite = _uiState.value.allSites.find { it.id == trail.siteIds.firstOrNull() }
            if (firstSite != null) {
                sendEffect(
                    MapUiEffect.AnimateCamera(
                        target = firstSite.geoPoint,
                        zoom = 15.0,
                        tilt = 35.0,
                        bearing = 0.0,
                        durationMs = 1200
                    )
                )
            }
        }
    }

    private fun exitTrail() {
        _uiState.update {
            it.copy(
                activeTrail = null,
                routePoints = emptyList(),
                showTrailSelector = false
            )
        }
    }

    private fun startObservingLocation() {
        if (locationJob?.isActive == true) return
        locationJob = viewModelScope.launch {
            locationTracker.observeLocationUpdates().collect { sample ->
                onUserLocationUpdated(sample.geoPoint, sample.isMoving)
            }
        }
    }

    private fun onUserLocationUpdated(location: GeoPoint, isMoving: Boolean) {
        _uiState.update { current ->
            val visibleSites = evaluateSiteUnlockStateUseCase.filterVisibleSites(
                allSites = current.allSites,
                unlockedSiteIds = current.unlockedSiteIds,
                userLocation = location
            )
            val displaySpears = current.availableSpears.filter { spear ->
                location.distanceToMeters(spear.geoPoint) <= 10f
            }
            val updatedUnlockStatus = current.selectedSite?.let { site ->
                evaluateSiteUnlockStateUseCase.evaluateUnlockStatus(
                    site = site,
                    allSites = current.allSites,
                    unlockedSiteIds = current.unlockedSiteIds,
                    userLocation = location
                )
            }
            val updatedSpearDistance = current.focusedSpear?.let { spear ->
                location.distanceToMeters(spear.geoPoint)
            }

            current.copy(
                userLocation = location,
                isUserMoving = isMoving,
                visibleSites = visibleSites,
                displaySpears = displaySpears,
                selectedSiteUnlockStatus = updatedUnlockStatus,
                focusedSpearDistanceMeters = updatedSpearDistance
            )
        }

        val state = _uiState.value
        if (!hasCenteredOnce) {
            hasCenteredOnce = true
            // If user opened map with a target site, don't override camera to user's location
            if (pendingTargetSiteId == null && state.selectedSite == null) {
                sendEffect(
                    MapUiEffect.AnimateCamera(
                        target = location,
                        zoom = 18.0,
                        tilt = if (state.is25DMode) state.cameraTilt else 0.0,
                        bearing = 0.0,
                        durationMs = 1000
                    )
                )
            }
        } else if (state.is25DMode && state.selection is MapSelection.None && state.activeTrail == null) {
            sendEffect(
                MapUiEffect.MoveCameraTracking(
                    target = location,
                    tilt = state.cameraTilt
                )
            )
        }
    }

    private fun fetchHistoricalSites() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val currentUser = authRepository.getCurrentUser()
            val unlockedIds = if (currentUser != null) {
                userRepository.getUserAppData(currentUser.uid)
                    .getOrNull()?.unlockedChapters?.toSet().orEmpty()
            } else {
                emptySet()
            }

            siteRepository.getHistoricalSites().fold(
                onSuccess = { sites ->
                    _uiState.update { current ->
                        val visibleSites = evaluateSiteUnlockStateUseCase.filterVisibleSites(
                            allSites = sites,
                            unlockedSiteIds = unlockedIds,
                            userLocation = current.userLocation
                        )
                        current.copy(
                            isLoading = false,
                            errorMessage = null,
                            isOfflineFallback = false,
                            allSites = sites,
                            visibleSites = visibleSites,
                            unlockedSiteIds = unlockedIds
                        )
                    }
                    tryFocusPendingTargetSite()
                    pendingInitialTrailId?.let {
                        activateTrail(it)
                        pendingInitialTrailId = null
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Unknown error occurred"
                        )
                    }
                }
            )
        }
    }

    private fun seedInitialFirestoreData() {
        if (_uiState.value.isSeedingData) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSeedingData = true, errorMessage = null) }
            siteRepository.seedInitialData(_uiState.value.userLocation).fold(
                onSuccess = {
                    _uiState.update { it.copy(isSeedingData = false) }
                    fetchHistoricalSites()
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isSeedingData = false,
                            errorMessage = "Gagal melakukan seed data: ${error.message}"
                        )
                    }
                }
            )
        }
    }

    private fun observeRealtimeSpears() {
        viewModelScope.launch {
            spearRepository.observeSpears().collect { snapshot ->
                _uiState.update { current ->
                    val displaySpears = current.userLocation?.let { loc ->
                        snapshot.availableSpears.filter { spear ->
                            loc.distanceToMeters(spear.geoPoint) <= 10f
                        }
                    } ?: emptyList()

                    current.copy(
                        spearTotalCount = snapshot.totalCount,
                        availableSpears = snapshot.availableSpears,
                        displaySpears = displaySpears
                    )
                }
                refreshUserSpearCount()
            }
        }
    }

    private fun refreshUserSpearCount() {
        val uid = authRepository.getCurrentUser()?.uid ?: return
        viewModelScope.launch {
            spearRepository.getCollectedSpearCount(uid).onSuccess { count ->
                _uiState.update { it.copy(collectedSpearCount = count) }
            }
        }
    }

    private fun selectSiteById(siteId: String) {
        val current = _uiState.value
        val site = current.allSites.find { it.id == siteId } ?: return
        applySiteSelection(site = site, disable25DTracking = false)
    }

    private fun tryFocusPendingTargetSite() {
        val targetId = pendingTargetSiteId ?: return
        val current = _uiState.value
        if (current.isLoading || current.allSites.isEmpty()) return

        val targetSite = current.allSites.find { it.id == targetId } ?: return
        pendingTargetSiteId = null
        applySiteSelection(site = targetSite, disable25DTracking = true)
    }

    private fun applySiteSelection(site: HistoricalSite, disable25DTracking: Boolean) {
        _uiState.update { current ->
            val unlockStatus = evaluateSiteUnlockStateUseCase.evaluateUnlockStatus(
                site = site,
                allSites = current.allSites,
                unlockedSiteIds = current.unlockedSiteIds,
                userLocation = current.userLocation
            )
            current.copy(
                selection = MapSelection.SiteSelected(site = site, showTopBanner = true),
                selectedSiteUnlockStatus = unlockStatus,
                focusedSpearDistanceMeters = null,
                is25DMode = if (disable25DTracking) false else current.is25DMode,
                cameraTilt = if (disable25DTracking) 0.0 else current.cameraTilt
            )
        }

        val state = _uiState.value
        sendEffect(
            MapUiEffect.AnimateCamera(
                target = site.geoPoint,
                zoom = if (disable25DTracking) 17.0 else 18.0,
                tilt = if (disable25DTracking) 45.0 else state.cameraTilt,
                bearing = 0.0,
                durationMs = if (disable25DTracking) 2000 else 1000
            )
        )
    }

    private fun selectSpearById(spearId: String) {
        val current = _uiState.value
        val spear: Spear = current.displaySpears.find { it.id == spearId }
            ?: current.availableSpears.find { it.id == spearId }
            ?: return

        val distance = current.userLocation?.distanceToMeters(spear.geoPoint)
        _uiState.update {
            it.copy(
                selection = MapSelection.SpearSelected(spear),
                selectedSiteUnlockStatus = null,
                focusedSpearDistanceMeters = distance
            )
        }
    }

    private fun clearSelection() {
        _uiState.update {
            it.copy(
                selection = MapSelection.None,
                selectedSiteUnlockStatus = null,
                focusedSpearDistanceMeters = null
            )
        }
    }

    private fun handlePrimarySiteAction() {
        val current = _uiState.value
        val site = current.selectedSite ?: return
        val status = current.selectedSiteUnlockStatus ?: return

        when (status) {
            SiteUnlockStatus.AlreadyUnlocked -> {
                sendEffect(MapUiEffect.NavigateToChapterDetail(site.id))
            }
            is SiteUnlockStatus.ReadyToUnlock -> {
                unlockChapterAndNavigate(site.id)
            }
            else -> Unit
        }
    }

    private fun unlockChapterAndNavigate(siteId: String) {
        val uid = authRepository.getCurrentUser()?.uid ?: return
        viewModelScope.launch {
            userRepository.unlockChapterAndVisitSite(uid, siteId)
            _uiState.update { current ->
                val updatedUnlocked = current.unlockedSiteIds + siteId
                val visibleSites = evaluateSiteUnlockStateUseCase.filterVisibleSites(
                    allSites = current.allSites,
                    unlockedSiteIds = updatedUnlocked,
                    userLocation = current.userLocation
                )
                val updatedStatus = current.selectedSite?.let { site ->
                    evaluateSiteUnlockStateUseCase.evaluateUnlockStatus(
                        site = site,
                        allSites = current.allSites,
                        unlockedSiteIds = updatedUnlocked,
                        userLocation = current.userLocation
                    )
                }
                current.copy(
                    unlockedSiteIds = updatedUnlocked,
                    visibleSites = visibleSites,
                    selectedSiteUnlockStatus = updatedStatus
                )
            }

            // Check if there is a matching heritage stamp to celebrate!
            val stamp = passportRepository.getStampBySiteId(siteId, isUnlocked = true)
            if (stamp != null) {
                _uiState.update { it.copy(unlockedStampCelebration = stamp) }
            } else {
                sendEffect(MapUiEffect.NavigateToChapterDetail(siteId))
            }
        }
    }

    private fun handleDismissStampCelebration() {
        val pendingSiteId = _uiState.value.unlockedStampCelebration?.siteId
        _uiState.update { it.copy(unlockedStampCelebration = null) }
        if (pendingSiteId != null) {
            sendEffect(MapUiEffect.NavigateToChapterDetail(pendingSiteId))
        }
    }

    private fun calculateWalkingRoute() {
        val current = _uiState.value
        val userLoc = current.userLocation ?: return
        val site = current.selectedSite ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isRouteLoading = true) }
            routeRepository.getWalkingRoute(userLoc, site.geoPoint).onSuccess { points ->
                _uiState.update { it.copy(routePoints = points) }
            }
            _uiState.update { it.copy(isRouteLoading = false) }
        }
    }

    private fun collectSpear(spearId: String) {
        val uid = authRepository.getCurrentUser()?.uid ?: return
        val current = _uiState.value
        if (current.isCollectingSpear) return

        val spear = current.availableSpears.find { it.id == spearId }
        _uiState.update { it.copy(isCollectingSpear = true) }

        viewModelScope.launch {
            val result = spearRepository.collectSpear(uid, spearId)
            _uiState.update { state ->
                if (result.isSuccess && spear != null) {
                    state.copy(
                        isCollectingSpear = false,
                        collectedSpearCount = state.collectedSpearCount + 1,
                        collectedSpearPopup = spear,
                        selection = MapSelection.None,
                        focusedSpearDistanceMeters = null
                    )
                } else {
                    state.copy(isCollectingSpear = false)
                }
            }
            refreshUserSpearCount()
        }
    }

    private fun toggle25DMode() {
        _uiState.update { current ->
            val nextMode = !current.is25DMode
            val nextTilt = if (nextMode) 50.0 else 0.0
            current.copy(is25DMode = nextMode, cameraTilt = nextTilt)
        }

        val state = _uiState.value
        state.userLocation?.let { loc ->
            sendEffect(
                MapUiEffect.AnimateCamera(
                    target = loc,
                    zoom = 18.0,
                    tilt = state.cameraTilt,
                    bearing = 0.0,
                    durationMs = 1000
                )
            )
        }
    }

    private fun centerOnUserLocation() {
        val state = _uiState.value
        val loc = state.userLocation ?: return
        sendEffect(
            MapUiEffect.AnimateCamera(
                target = loc,
                zoom = if (state.is25DMode) 18.0 else 17.0,
                tilt = if (state.is25DMode) state.cameraTilt else 0.0,
                bearing = 0.0,
                durationMs = 1000
            )
        )
    }

    private fun sendEffect(effect: MapUiEffect) {
        viewModelScope.launch {
            _uiEffect.send(effect)
        }
    }
}
