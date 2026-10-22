package com.loregate.app.feature.map

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.loregate.app.core.designsystem.OrangePrimary
import com.loregate.app.feature.map.components.HeritageTrailSelectorBottomSheet
import com.loregate.app.feature.map.components.MapActiveTrailHudCard
import com.loregate.app.feature.map.components.MapFloatingControlButtons
import com.loregate.app.feature.map.components.MapOfflineStatusChip
import com.loregate.app.feature.map.components.MapSelectedSiteBannerCard
import com.loregate.app.feature.map.components.MapTopHudOverlay
import com.loregate.app.feature.map.components.SiteDetailBottomSheet
import com.loregate.app.feature.map.components.SpearCollectionDialog
import com.loregate.app.feature.map.components.SpearFocusedCard
import com.loregate.app.feature.map.components.StampCelebrationDialog
import kotlinx.coroutines.flow.Flow
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun MapRoute(
    targetSiteId: String? = null,
    initialTrailId: String? = null,
    onNavigateBack: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToChapterDetail: (String) -> Unit,
    viewModel: MapViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(targetSiteId) {
        if (!targetSiteId.isNullOrBlank()) {
            viewModel.onEvent(MapUiEvent.OnFocusTargetSite(targetSiteId))
        }
    }

    LaunchedEffect(initialTrailId) {
        if (!initialTrailId.isNullOrBlank()) {
            viewModel.setInitialTrail(initialTrailId)
        }
    }

    RequestLocationPermissionEffect(
        onGranted = { viewModel.onEvent(MapUiEvent.OnLocationPermissionGranted) },
        onBackgroundGranted = { viewModel.onEvent(MapUiEvent.OnBackgroundLocationPermissionGranted) }
    )

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
    uiEffectFlow: Flow<MapUiEffect>,
    onEvent: (MapUiEvent) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToChapterDetail: (String) -> Unit
) {
    LaunchedEffect(Unit) {
        uiEffectFlow.collect { effect ->
            when (effect) {
                is MapUiEffect.NavigateToChapterDetail -> onNavigateToChapterDetail(effect.siteId)
                MapUiEffect.NavigateToProfile -> onNavigateToProfile()
                MapUiEffect.NavigateBack -> onNavigateBack()
                else -> Unit
            }
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
                            text = "Memuat situs dari Firebase...",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = OrangePrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Mohon tunggu sebentar",
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
                            contentDescription = null,
                            tint = Color.Red,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Gagal memuat peta",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = uiState.errorMessage,
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = { onEvent(MapUiEvent.OnRetryFetchSites) },
                            colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Coba Lagi")
                        }
                    }
                }
            }

            else -> {
                PlatformMapView(
                    modifier = Modifier.fillMaxSize(),
                    uiState = uiState,
                    uiEffectFlow = uiEffectFlow,
                    onEvent = onEvent
                )

                MapTopHudOverlay(
                    showBackButton = true,
                    collectedSpearCount = uiState.collectedSpearCount,
                    spearTotalCount = uiState.spearTotalCount,
                    availableSpearCount = uiState.availableSpears.size,
                    nearbySpearCount = uiState.displaySpears.size,
                    unlockedSitesCount = uiState.unlockedSiteIds.size,
                    totalSitesCount = uiState.allSites.size,
                    onBackClick = { onEvent(MapUiEvent.OnBackClicked) },
                    onProfileClick = { onEvent(MapUiEvent.OnProfileClicked) }
                )

                if (uiState.isOfflineFallback) {
                    MapOfflineStatusChip()
                }

                if (uiState.activeTrail != null) {
                    MapActiveTrailHudCard(
                        activeTrail = uiState.activeTrail,
                        onExitTrail = { onEvent(MapUiEvent.OnExitTrail) }
                    )
                }

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
                    onMyLocationClick = { onEvent(MapUiEvent.OnMyLocationClicked) },
                    onToggleTrailSelector = { onEvent(MapUiEvent.OnToggleTrailSelector(true)) }
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

                if (uiState.showTrailSelector) {
                    HeritageTrailSelectorBottomSheet(
                        trails = uiState.availableTrails,
                        onSelectTrail = { trailId ->
                            onEvent(MapUiEvent.OnActivateTrail(trailId))
                        },
                        onDismiss = { onEvent(MapUiEvent.OnToggleTrailSelector(false)) }
                    )
                }

                StampCelebrationDialog(
                    stamp = uiState.unlockedStampCelebration,
                    onDismiss = { onEvent(MapUiEvent.OnDismissStampCelebration) }
                )
            }
        }
    }
}
