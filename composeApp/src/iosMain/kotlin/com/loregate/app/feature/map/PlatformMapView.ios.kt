package com.loregate.app.feature.map

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.flow.Flow
import platform.CoreLocation.CLLocationCoordinate2DMake
import platform.MapKit.MKCoordinateRegionMakeWithDistance
import platform.MapKit.MKMapView
import platform.MapKit.MKPointAnnotation

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun PlatformMapView(
    modifier: Modifier,
    uiState: MapUiState,
    uiEffectFlow: Flow<MapUiEffect>,
    onEvent: (MapUiEvent) -> Unit
) {
    UIKitView(
        factory = {
            MKMapView().apply {
                showsUserLocation = true
                uiState.userLocation?.let { loc ->
                    val coord = CLLocationCoordinate2DMake(loc.latitude, loc.longitude)
                    setRegion(MKCoordinateRegionMakeWithDistance(coord, 1000.0, 1000.0), animated = false)
                }
            }
        },
        modifier = modifier,
        update = { mapView ->
            mapView.removeAnnotations(mapView.annotations)
            uiState.visibleSites.forEach { site ->
                val annotation = MKPointAnnotation().apply {
                    setCoordinate(CLLocationCoordinate2DMake(site.latitude, site.longitude))
                    setTitle(site.nama)
                    setSubtitle(site.type.displayLabel)
                }
                mapView.addAnnotation(annotation)
            }
        }
    )
}

@Composable
actual fun RequestLocationPermissionEffect(
    onGranted: () -> Unit,
    onBackgroundGranted: () -> Unit
) {
    onGranted()
    onBackgroundGranted()
}
