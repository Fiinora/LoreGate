package com.loregate.app.feature.map

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.RectF
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.loregate.app.BuildConfig
import com.loregate.app.core.designsystem.OrangePrimary
import com.loregate.app.feature.map.components.MapFloatingControlButtons
import com.loregate.app.feature.map.components.MapSelectedSiteBannerCard
import com.loregate.app.feature.map.components.MapTopHudOverlay
import com.loregate.app.feature.map.components.SiteDetailBottomSheet
import com.loregate.app.feature.map.components.SpearCollectionDialog
import com.loregate.app.feature.map.components.SpearFocusedCard
import com.loregate.app.feature.map.maplibre.MapStyleRenderer
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView

@Composable
fun MapRoute(
    targetSiteId: String? = null,
    onNavigateBack: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToChapterDetail: (String) -> Unit,
    viewModel: MapViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(targetSiteId) {
        if (!targetSiteId.isNullOrBlank()) {
            viewModel.onEvent(MapUiEvent.OnFocusTargetSite(targetSiteId))
        }
    }

    val backgroundLocationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.onEvent(MapUiEvent.OnBackgroundLocationPermissionGranted)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        if (fineGranted) {
            viewModel.onEvent(MapUiEvent.OnLocationPermissionGranted)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val hasBg = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                ) == PackageManager.PERMISSION_GRANTED

                if (!hasBg) {
                    backgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                } else {
                    viewModel.onEvent(MapUiEvent.OnBackgroundLocationPermissionGranted)
                }
            } else {
                viewModel.onEvent(MapUiEvent.OnBackgroundLocationPermissionGranted)
            }
        }
    }

    LaunchedEffect(Unit) {
        val permissionsToRequest = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionLauncher.launch(permissionsToRequest.toTypedArray())
    }

    MapScreen(
        uiState = uiState,
        uiEffectFlow = viewModel.uiEffect,
        onEvent = viewModel::onEvent,
        onNavigateBack = onNavigateBack,
        onNavigateToProfile = onNavigateToProfile,
        onNavigateToChapterDetail = onNavigateToChapterDetail
    )
}

@Composable
fun MapScreen(
    uiState: MapUiState,
    uiEffectFlow: kotlinx.coroutines.flow.Flow<MapUiEffect>,
    onEvent: (MapUiEvent) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToChapterDetail: (String) -> Unit
) {
    var mapLibreMap by remember { mutableStateOf<MapLibreMap?>(null) }

    // Animated user radar pulse (6m -> 20m)
    val radarPulse = rememberInfiniteTransition(label = "radarPulse")
    val radarRadius by radarPulse.animateFloat(
        initialValue = 6f,
        targetValue = 20f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radarRadius"
    )
    val radarAlpha by radarPulse.animateFloat(
        initialValue = 0.32f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radarAlpha"
    )

    // Animated spear pulse & bobbing
    val spearPulse = rememberInfiniteTransition(label = "spearPulse")
    val spearPulseRadius by spearPulse.animateFloat(
        initialValue = 12f,
        targetValue = 22f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400),
            repeatMode = RepeatMode.Restart
        ),
        label = "spearPulseRadius"
    )
    val spearPulseAlpha by spearPulse.animateFloat(
        initialValue = 0.22f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400),
            repeatMode = RepeatMode.Restart
        ),
        label = "spearPulseAlpha"
    )
    val spearBob by spearPulse.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "spearBob"
    )

    // Collect one-off UI Effects (Camera animations & Navigation)
    LaunchedEffect(mapLibreMap) {
        uiEffectFlow.collect { effect ->
            when (effect) {
                is MapUiEffect.AnimateCamera -> {
                    mapLibreMap?.animateCamera(
                        CameraUpdateFactory.newCameraPosition(
                            CameraPosition.Builder()
                                .target(LatLng(effect.target.latitude, effect.target.longitude))
                                .zoom(effect.zoom)
                                .tilt(effect.tilt)
                                .bearing(effect.bearing)
                                .build()
                        ),
                        effect.durationMs
                    )
                }
                is MapUiEffect.MoveCameraTracking -> {
                    mapLibreMap?.let { map ->
                        val currentPos = map.cameraPosition
                        val nextPos = CameraPosition.Builder()
                            .target(LatLng(effect.target.latitude, effect.target.longitude))
                            .zoom(currentPos.zoom)
                            .tilt(effect.tilt)
                            .bearing(currentPos.bearing)
                            .build()
                        map.moveCamera(CameraUpdateFactory.newCameraPosition(nextPos))
                    }
                }
                is MapUiEffect.ZoomBy -> {
                    mapLibreMap?.let { map ->
                        map.animateCamera(
                            CameraUpdateFactory.zoomTo(map.cameraPosition.zoom + effect.delta)
                        )
                    }
                }
                is MapUiEffect.NavigateToChapterDetail -> onNavigateToChapterDetail(effect.siteId)
                MapUiEffect.NavigateToProfile -> onNavigateToProfile()
                MapUiEffect.NavigateBack -> onNavigateBack()
            }
        }
    }

    // Reactive Map Layer Synchronization
    LaunchedEffect(mapLibreMap, uiState.visibleSites, uiState.selectedSite?.id) {
        val map = mapLibreMap ?: return@LaunchedEffect
        MapStyleRenderer.updateSitesLayer(
            map = map,
            visibleSites = uiState.visibleSites,
            selectedSiteId = uiState.selectedSite?.id
        )
    }

    LaunchedEffect(mapLibreMap, uiState.userLocation, uiState.isUserMoving) {
        val map = mapLibreMap ?: return@LaunchedEffect
        val loc = uiState.userLocation ?: return@LaunchedEffect
        MapStyleRenderer.updateUserLocationLayer(
            map = map,
            location = loc,
            isMoving = uiState.isUserMoving
        )
    }

    LaunchedEffect(mapLibreMap, uiState.userLocation, radarRadius, radarAlpha) {
        val map = mapLibreMap ?: return@LaunchedEffect
        val loc = uiState.userLocation ?: return@LaunchedEffect
        MapStyleRenderer.updateUserRadarLayer(
            map = map,
            location = loc,
            radiusMeters = radarRadius,
            alpha = radarAlpha
        )
    }

    LaunchedEffect(
        mapLibreMap,
        uiState.displaySpears,
        spearPulseRadius,
        spearPulseAlpha,
        spearBob
    ) {
        val map = mapLibreMap ?: return@LaunchedEffect
        MapStyleRenderer.updateSpearLayer(
            map = map,
            spears = uiState.displaySpears,
            pulseRadius = spearPulseRadius,
            pulseAlpha = spearPulseAlpha,
            bobOffset = spearBob
        )
    }

    LaunchedEffect(mapLibreMap, uiState.routePoints) {
        val map = mapLibreMap ?: return@LaunchedEffect
        if (uiState.routePoints.isNotEmpty()) {
            MapStyleRenderer.drawRouteLayer(map, uiState.routePoints)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            uiState.isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            color = OrangePrimary,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Fetching sites from Firebase...",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = OrangePrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Please wait",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }
                }
            }

            uiState.errorMessage != null -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Error",
                            tint = Color.Red,
                            modifier = Modifier.size(72.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "❌ Firebase Error",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Red
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = uiState.errorMessage,
                            fontSize = 14.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { onEvent(MapUiEvent.OnRetryFetchSites) },
                            colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Retry Fetching")
                        }
                    }
                }
            }

            else -> {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        MapView(ctx).apply {
                            getMapAsync { map ->
                                if (mapLibreMap == null) {
                                    map.setStyle(
                                        "https://api.maptiler.com/maps/streets/style.json?key=${BuildConfig.MAPTILER_API_KEY}"
                                    ) {
                                        mapLibreMap = map
                                        map.uiSettings.apply {
                                            isRotateGesturesEnabled = true
                                            isTiltGesturesEnabled = true
                                            isQuickZoomGesturesEnabled = true
                                            isScrollGesturesEnabled = true
                                            isZoomGesturesEnabled = true
                                        }

                                        map.addOnMapClickListener { latLng ->
                                            val pointF = map.projection.toScreenLocation(latLng)
                                            val tolerance = 32f
                                            val rect = RectF(
                                                pointF.x - tolerance,
                                                pointF.y - tolerance,
                                                pointF.x + tolerance,
                                                pointF.y + tolerance
                                            )

                                            val spearFeatures = map.queryRenderedFeatures(
                                                rect,
                                                MapStyleRenderer.SPEARS_ICON_LAYER_ID,
                                                MapStyleRenderer.SPEARS_PULSE_LAYER_ID,
                                                MapStyleRenderer.SPEARS_LABEL_LAYER_ID
                                            )
                                            val spearId = spearFeatures
                                                .firstOrNull()
                                                ?.getStringProperty("id")

                                            if (spearId != null) {
                                                onEvent(MapUiEvent.OnSpearMarkerClicked(spearId))
                                                return@addOnMapClickListener true
                                            }

                                            val siteFeatures = map.queryRenderedFeatures(
                                                pointF,
                                                MapStyleRenderer.SITES_LAYER_ID
                                            )
                                            if (siteFeatures.isNotEmpty()) {
                                                val siteId = siteFeatures
                                                    .first()
                                                    .getStringProperty("id")
                                                if (siteId != null) {
                                                    onEvent(MapUiEvent.OnSiteMarkerClicked(siteId))
                                                }
                                            } else {
                                                onEvent(MapUiEvent.OnMapEmptyClicked)
                                            }
                                            true
                                        }
                                    }
                                }
                            }
                        }
                    }
                )

                MapTopHudOverlay(
                    showBackButton = !uiState.showTopMarkerCard,
                    collectedSpearCount = uiState.collectedSpearCount,
                    spearTotalCount = uiState.spearTotalCount,
                    availableSpearCount = uiState.availableSpears.size,
                    nearbySpearCount = uiState.displaySpears.size,
                    unlockedSitesCount = uiState.unlockedSiteIds.size,
                    totalSitesCount = uiState.allSites.size,
                    onBackClick = { onEvent(MapUiEvent.OnBackClicked) },
                    onProfileClick = { onEvent(MapUiEvent.OnProfileClicked) }
                )

                if (uiState.showTopMarkerCard && uiState.selectedSite != null) {
                    MapSelectedSiteBannerCard(
                        site = uiState.selectedSite!!,
                        onDismiss = { onEvent(MapUiEvent.OnDismissSiteSelection) }
                    )
                }

                if (uiState.allSites.isEmpty()) {
                    Card(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 20.dp, vertical = 28.dp)
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF1E293B).copy(alpha = 0.94f)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Place,
                                contentDescription = null,
                                tint = OrangePrimary,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Database Firestore Masih Kosong",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Mode Free Roam tetap aktif. Tekan tombol di bawah untuk mengisi data awal (Situs Sejarah, Cerita, Chapter & Tombak) ke Firestore.",
                                color = Color(0xFFCBD5E1),
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = { onEvent(MapUiEvent.OnSeedInitialDataClicked) },
                                enabled = !uiState.isSeedingData,
                                colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (uiState.isSeedingData) {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("Mengisi Data ke Firestore...")
                                } else {
                                    Icon(Icons.Default.CloudUpload, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Seed Data Awal ke Firestore",
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                MapFloatingControlButtons(
                    is25DMode = uiState.is25DMode,
                    onToggle25DMode = { onEvent(MapUiEvent.OnToggle25DModeClicked) },
                    onZoomIn = { onEvent(MapUiEvent.OnZoomInClicked) },
                    onZoomOut = { onEvent(MapUiEvent.OnZoomOutClicked) },
                    onMyLocationClick = { onEvent(MapUiEvent.OnMyLocationClicked) }
                )

                when (val selection = uiState.selection) {
                    is MapSelection.SiteSelected -> {
                        val status = uiState.selectedSiteUnlockStatus
                        if (status != null) {
                            SiteDetailBottomSheet(
                                site = selection.site,
                                unlockStatus = status,
                                userLocation = uiState.userLocation,
                                isRouteLoading = uiState.isRouteLoading,
                                onPrimaryActionClick = {
                                    onEvent(MapUiEvent.OnPrimarySiteActionClicked)
                                },
                                onGetDirectionsClick = {
                                    onEvent(MapUiEvent.OnGetDirectionsClicked)
                                }
                            )
                        }
                    }
                    is MapSelection.SpearSelected -> {
                        SpearFocusedCard(
                            spear = selection.spear,
                            distanceMeters = uiState.focusedSpearDistanceMeters,
                            isCollecting = uiState.isCollectingSpear,
                            onCollectClick = {
                                onEvent(MapUiEvent.OnCollectSpearClicked(selection.spear.id))
                            },
                            onDismissClick = {
                                onEvent(MapUiEvent.OnDismissSpearSelection)
                            }
                        )
                    }
                    MapSelection.None -> Unit
                }

                SpearCollectionDialog(
                    spear = uiState.collectedSpearPopup,
                    onDismiss = { onEvent(MapUiEvent.OnDismissCollectionPopup) }
                )
            }
        }
    }
}
