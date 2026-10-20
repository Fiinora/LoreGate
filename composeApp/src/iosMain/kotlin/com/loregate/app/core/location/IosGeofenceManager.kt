package com.loregate.app.core.location

import com.loregate.app.domain.model.HistoricalSite
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreLocation.CLCircularRegion
import platform.CoreLocation.CLLocationCoordinate2DMake
import platform.CoreLocation.CLLocationManager

@OptIn(ExperimentalForeignApi::class)
class IosGeofenceManager : GeofenceManager {

    private val locationManager = CLLocationManager()

    override fun registerSitesGeofences(sites: List<HistoricalSite>) {
        if (!CLLocationManager.isMonitoringAvailableForClass(CLCircularRegion)) return

        sites.forEach { site ->
            val center = CLLocationCoordinate2DMake(site.latitude, site.longitude)
            val region = CLCircularRegion(
                center = center,
                radius = site.radius.toDouble(),
                identifier = site.id
            ).apply {
                notifyOnEntry = true
                notifyOnExit = false
            }
            locationManager.startMonitoringForRegion(region)
        }
    }
}
