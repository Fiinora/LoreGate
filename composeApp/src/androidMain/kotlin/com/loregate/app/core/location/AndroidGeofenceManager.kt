package com.loregate.app.core.location

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.loregate.app.core.notification.GeofenceBroadcastReceiver
import com.loregate.app.domain.model.HistoricalSite

class AndroidGeofenceManager(
    private val context: Context,
    private val geofencingClient: GeofencingClient
) : GeofenceManager {

    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
        PendingIntent.getBroadcast(context, 0, intent, flags)
    }

    @SuppressLint("MissingPermission")
    override fun registerSitesGeofences(sites: List<HistoricalSite>) {
        if (sites.isEmpty()) return

        sites.forEach { site ->
            val packedData = "${site.id}|${site.isHidden}|${site.nama}"

            val geofence = Geofence.Builder()
                .setRequestId(packedData)
                .setCircularRegion(site.latitude, site.longitude, site.radius)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                .setNotificationResponsiveness(1000)
                .build()

            val request = GeofencingRequest.Builder()
                .addGeofence(geofence)
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .build()

            geofencingClient.addGeofences(request, geofencePendingIntent)
                .addOnSuccessListener {
                    Log.d("GeofenceManager", "Geofence registered for ${site.nama}")
                }
                .addOnFailureListener { e ->
                    Log.e("GeofenceManager", "Failed to register geofence: ${e.message}")
                }
        }
    }
}
