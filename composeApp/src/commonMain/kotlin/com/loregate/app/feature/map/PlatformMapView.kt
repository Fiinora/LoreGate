package com.loregate.app.feature.map

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlinx.coroutines.flow.Flow

@Composable
expect fun PlatformMapView(
    modifier: Modifier = Modifier,
    uiState: MapUiState,
    uiEffectFlow: Flow<MapUiEffect>,
    onEvent: (MapUiEvent) -> Unit
)

@Composable
expect fun RequestLocationPermissionEffect(
    onGranted: () -> Unit,
    onBackgroundGranted: () -> Unit
)
