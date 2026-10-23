package com.loregate.app.feature.map

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.loregate.app.core.designsystem.OrangePrimary
import com.loregate.app.domain.model.CommunitySpotCategory
import com.loregate.app.feature.map.components.AddCommunitySpotBottomSheet
import com.loregate.app.feature.map.components.CommunitySpotBottomSheet
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
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        uiEffectFlow.collect { effect ->
            when (effect) {
                is MapUiEffect.NavigateToChapterDetail -> onNavigateToChapterDetail(effect.siteId)
                MapUiEffect.NavigateToProfile -> onNavigateToProfile()
                MapUiEffect.NavigateBack -> onNavigateBack()
                is MapUiEffect.ShowToast -> snackbarHostState.showSnackbar(effect.message)
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

                // 1. Top HUD & Overlays in a single cohesive Column
                Column(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .zIndex(15f)
                ) {
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

                    if (uiState.activeTrail != null) {
                        MapActiveTrailHudCard(
                            activeTrail = uiState.activeTrail,
                            onExitTrail = { onEvent(MapUiEvent.OnExitTrail) }
                        )
                    }

                    val statusToast = uiState.statusToast
                    if (statusToast != null) {
                        val isError = statusToast.contains("Gagal", ignoreCase = true) || statusToast.contains("error", ignoreCase = true)
                        Card(
                            modifier = Modifier
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                                .fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isError) Color(0xFFEF4444) else OrangePrimary
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isError) Icons.Default.Warning else Icons.Default.Place,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = statusToast,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }

                    if (uiState.isOfflineFallback) {
                        MapOfflineStatusChip(
                            modifier = Modifier
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                                .align(Alignment.CenterHorizontally)
                        )
                    }

                    // Category Filter Chips Bar
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            val isAllSelected = uiState.selectedCategoryFilter == null
                            Surface(
                                onClick = { onEvent(MapUiEvent.OnCategoryFilterClicked(null)) },
                                shape = RoundedCornerShape(20.dp),
                                color = if (isAllSelected) OrangePrimary else Color(0xFF1E293B).copy(alpha = 0.88f),
                                shadowElevation = if (isAllSelected) 4.dp else 2.dp,
                                border = BorderStroke(
                                    1.dp,
                                    if (isAllSelected) OrangePrimary else Color.White.copy(alpha = 0.15f)
                                ),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Box(
                                    modifier = Modifier.padding(horizontal = 14.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Semua",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }

                        items(CommunitySpotCategory.entries) { cat ->
                            val isSelected = uiState.selectedCategoryFilter == cat
                            val catColor = runCatching { Color(android.graphics.Color.parseColor(cat.hexColor)) }
                                .getOrElse { OrangePrimary }

                            Surface(
                                onClick = { onEvent(MapUiEvent.OnCategoryFilterClicked(if (isSelected) null else cat)) },
                                shape = RoundedCornerShape(20.dp),
                                color = if (isSelected) catColor else Color(0xFF1E293B).copy(alpha = 0.88f),
                                shadowElevation = if (isSelected) 4.dp else 2.dp,
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) catColor else Color.White.copy(alpha = 0.15f)
                                ),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = cat.emoji, fontSize = 13.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = cat.displayLabel,
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }

                if (uiState.allSites.isEmpty()) {
                    Card(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(start = 20.dp, end = 20.dp, bottom = 28.dp)
                            .fillMaxWidth()
                            .zIndex(10f),
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

                val isAnySheetOpen = uiState.selection != MapSelection.None ||
                    uiState.isAddingCommunitySpot ||
                    uiState.showTrailSelector ||
                    uiState.collectedSpearPopup != null ||
                    uiState.unlockedStampCelebration != null

                // Floating Action Controls & FAB (Ergonomic bottom-right vertical alignment)
                AnimatedVisibility(
                    visible = !isAnySheetOpen,
                    enter = fadeIn(tween(180)) + scaleIn(tween(180), initialScale = 0.85f),
                    exit = fadeOut(tween(120)) + scaleOut(tween(120), targetScale = 0.85f),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .navigationBarsPadding()
                        .padding(end = 16.dp, bottom = 20.dp)
                        .zIndex(10f)
                ) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MapFloatingControlButtons(
                            is25DMode = uiState.is25DMode,
                            onToggle25DMode = { onEvent(MapUiEvent.OnToggle25DModeClicked) },
                            onZoomIn = { onEvent(MapUiEvent.OnZoomInClicked) },
                            onZoomOut = { onEvent(MapUiEvent.OnZoomOutClicked) },
                            onMyLocationClick = { onEvent(MapUiEvent.OnMyLocationClicked) },
                            onToggleTrailSelector = { onEvent(MapUiEvent.OnToggleTrailSelector(true)) }
                        )

                        if (uiState.allSites.isNotEmpty()) {
                            ExtendedFloatingActionButton(
                                onClick = { onEvent(MapUiEvent.OnStartAddSpot()) },
                                containerColor = OrangePrimary,
                                contentColor = Color.White,
                                shape = RoundedCornerShape(20.dp),
                                elevation = FloatingActionButtonDefaults.elevation(
                                    defaultElevation = 6.dp,
                                    pressedElevation = 2.dp
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Bagikan Temuan",
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Bagikan Temuan",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }

                // Modal Selection Sheets with Clickable Scrim to dismiss
                when (val selection = uiState.selection) {
                    is MapSelection.SiteSelected -> {
                        val status = uiState.selectedSiteUnlockStatus
                        if (status != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.45f))
                                    .clickable { onEvent(MapUiEvent.OnDismissSiteSelection) }
                                    .zIndex(20f)
                            )
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
                                },
                                onAddTipClick = {
                                    onEvent(MapUiEvent.OnAddTipForSiteClicked(selection.site.id))
                                },
                                onDismiss = {
                                    onEvent(MapUiEvent.OnDismissSiteSelection)
                                }
                            )
                        }
                    }
                    is MapSelection.SpearSelected -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.45f))
                                .clickable { onEvent(MapUiEvent.OnDismissSpearSelection) }
                                .zIndex(20f)
                        )
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
                    is MapSelection.CommunitySpotSelected -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.45f))
                                .clickable { onEvent(MapUiEvent.OnDismissCommunitySpotSelection) }
                                .zIndex(20f)
                        )
                        CommunitySpotBottomSheet(
                            spot = selection.spot,
                            currentUserId = uiState.currentUserId,
                            userLocation = uiState.userLocation,
                            isRouteLoading = uiState.isRouteLoading,
                            onToggleHelpfulClick = {
                                onEvent(MapUiEvent.OnToggleHelpfulClicked(selection.spot.id))
                            },
                            onGetDirectionsClick = {
                                onEvent(MapUiEvent.OnGetDirectionsClicked)
                            },
                            onReportClick = { reason ->
                                onEvent(MapUiEvent.OnReportSpotClicked(selection.spot.id, reason))
                            },
                            onDeleteClick = {
                                onEvent(MapUiEvent.OnDeleteSpotClicked(selection.spot.id))
                            },
                            onDismiss = {
                                onEvent(MapUiEvent.OnDismissCommunitySpotSelection)
                            }
                        )
                    }
                    MapSelection.None -> Unit
                }

                if (uiState.isAddingCommunitySpot) {
                    val targetSite = uiState.draftTargetSiteId?.let { id -> uiState.allSites.find { it.id == id } }
                    AddCommunitySpotBottomSheet(
                        draftCoordinate = uiState.draftSpotCoordinate,
                        userLocation = uiState.userLocation,
                        targetSiteName = targetSite?.nama,
                        isSaving = uiState.isSavingCommunitySpot,
                        onSubmit = { title, description, category, imageBytes ->
                            onEvent(MapUiEvent.OnSubmitCommunitySpot(title, description, category, imageBytes))
                        },
                        onDismiss = {
                            onEvent(MapUiEvent.OnDismissAddSpot)
                        }
                    )
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

                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 80.dp)
                        .zIndex(20f)
                )
            }
        }
    }
}
