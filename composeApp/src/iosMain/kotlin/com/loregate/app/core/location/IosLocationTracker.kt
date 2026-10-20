package com.loregate.app.core.location

import com.loregate.app.domain.model.GeoPoint
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLLocationAccuracyBest
import platform.Foundation.timeIntervalSince1970
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class)
class IosLocationTracker(
    private val movementDetector: MovementDetector
) : LocationTracker {

    private val locationManager = CLLocationManager()

    override fun observeLocationUpdates(): Flow<UserLocationSample> = callbackFlow {
        locationManager.desiredAccuracy = kCLLocationAccuracyBest
        locationManager.requestWhenInUseAuthorization()

        val delegate = object : NSObject(), CLLocationManagerDelegateProtocol {
            override fun locationManager(manager: CLLocationManager, didUpdateLocations: List<*>) {
                val lastLocation = didUpdateLocations.lastOrNull() as? CLLocation ?: return
                val point = lastLocation.coordinate.useContents {
                    GeoPoint(latitude = latitude, longitude = longitude)
                }
                val now = (lastLocation.timestamp.timeIntervalSince1970 * 1000.0).toLong()
                val isMoving = movementDetector.isMoving(
                    geoPoint = point,
                    accuracy = lastLocation.horizontalAccuracy.toFloat(),
                    timeMillis = now
                )
                trySend(
                    UserLocationSample(
                        geoPoint = point,
                        isMoving = isMoving
                    )
                )
            }
        }

        locationManager.delegate = delegate
        locationManager.startUpdatingLocation()

        awaitClose {
            locationManager.stopUpdatingLocation()
            locationManager.delegate = null
        }
    }
}
