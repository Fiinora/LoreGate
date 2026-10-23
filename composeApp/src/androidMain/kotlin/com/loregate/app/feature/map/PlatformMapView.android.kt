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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.loregate.app.BuildConfig
import com.loregate.app.feature.map.maplibre.MapStyleRenderer
import kotlinx.coroutines.flow.Flow
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView

@Composable
actual fun PlatformMapView(
    modifier: Modifier,
    uiState: MapUiState,
    uiEffectFlow: Flow<MapUiEffect>,
    onEvent: (MapUiEvent) -> Unit
) {
    val context = LocalContext.current
    var mapLibreMap by remember { mutableStateOf<MapLibreMap?>(null) }
    var hasAppliedInitialTargetFocus by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "RadarTransition")
    val radarRadius by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 60f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RadarRadiusAnim"
    )
    val radarAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RadarAlphaAnim"
    )

    val spearPulseRadius by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 20f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "SpearPulseRadiusAnim"
    )
    val spearPulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "SpearPulseAlphaAnim"
    )
    val spearBob by infiniteTransition.animateFloat(
        initialValue = -1.5f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "SpearBobAnim"
    )

    LaunchedEffect(mapLibreMap) {
        val map = mapLibreMap ?: return@LaunchedEffect
        uiEffectFlow.collect { effect ->
            when (effect) {
                is MapUiEffect.AnimateCamera -> {
                    map.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(
                            LatLng(effect.target.latitude, effect.target.longitude),
                            effect.zoom
                        ),
                        effect.durationMs
                    )
                }
                is MapUiEffect.MoveCameraTracking -> {
                    map.easeCamera(
                        CameraUpdateFactory.newLatLng(
                            LatLng(effect.target.latitude, effect.target.longitude)
                        ),
                        1000
                    )
                }
                is MapUiEffect.ZoomBy -> {
                    map.animateCamera(CameraUpdateFactory.zoomBy(effect.delta), 300)
                }
                else -> Unit
            }
        }
    }

    LaunchedEffect(mapLibreMap, uiState.selectedSite?.id) {
        val map = mapLibreMap ?: return@LaunchedEffect
        val selectedSite = uiState.selectedSite
        if (selectedSite != null && !hasAppliedInitialTargetFocus) {
            hasAppliedInitialTargetFocus = true
            map.animateCamera(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(selectedSite.latitude, selectedSite.longitude),
                    17.0
                ),
                1200
            )
        }
    }

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

    LaunchedEffect(mapLibreMap, uiState.visibleCommunitySpots, uiState.selectedCommunitySpot?.id) {
        val map = mapLibreMap ?: return@LaunchedEffect
        MapStyleRenderer.updateCommunitySpotsLayer(
            map = map,
            visibleSpots = uiState.visibleCommunitySpots,
            selectedSpotId = uiState.selectedCommunitySpot?.id
        )
    }

    AndroidView(
        modifier = modifier,
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

                                val communitySpotFeatures = map.queryRenderedFeatures(
                                    rect,
                                    MapStyleRenderer.COMMUNITY_SPOTS_UNCLUSTERED_LAYER_ID
                                )
                                val spotId = communitySpotFeatures
                                    .firstOrNull()
                                    ?.getStringProperty("id")
                                if (spotId != null) {
                                    onEvent(MapUiEvent.OnCommunitySpotMarkerClicked(spotId))
                                    return@addOnMapClickListener true
                                }

                                val clusterFeatures = map.queryRenderedFeatures(
                                    rect,
                                    MapStyleRenderer.COMMUNITY_SPOTS_CLUSTER_CIRCLE_LAYER_ID,
                                    MapStyleRenderer.COMMUNITY_SPOTS_CLUSTER_COUNT_LAYER_ID
                                )
                                if (clusterFeatures.isNotEmpty()) {
                                    map.animateCamera(
                                        CameraUpdateFactory.newLatLngZoom(
                                            latLng,
                                            map.cameraPosition.zoom + 2.0
                                        ),
                                        500
                                    )
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

                            map.addOnMapLongClickListener { latLng ->
                                onEvent(
                                    MapUiEvent.OnStartAddSpot(
                                        initialCoordinate = com.loregate.app.domain.model.GeoPoint(
                                            latitude = latLng.latitude,
                                            longitude = latLng.longitude
                                        ),
                                        targetSiteId = null
                                    )
                                )
                                true
                            }
                        }
                    }
                }
            }
        }
    )
}

@Composable
actual fun RequestLocationPermissionEffect(
    onGranted: () -> Unit,
    onBackgroundGranted: () -> Unit
) {
    val context = LocalContext.current

    val backgroundLocationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            onBackgroundGranted()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        if (fineGranted) {
            onGranted()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val hasBg = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                ) == PackageManager.PERMISSION_GRANTED

                if (!hasBg) {
                    backgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                } else {
                    onBackgroundGranted()
                }
            } else {
                onBackgroundGranted()
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
}
