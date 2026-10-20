package com.loregate.app.core.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent

class GeofenceBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        if (geofencingEvent == null || geofencingEvent.hasError()) {
            Log.e("GeofenceReceiver", "Error: ${geofencingEvent?.errorCode}")
            return
        }

        if (geofencingEvent.geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            val triggeringGeofences = geofencingEvent.triggeringGeofences ?: return

            triggeringGeofences.forEach { geofence ->
                val rawData = geofence.requestId
                try {
                    val parts = rawData.split("|")
                    if (parts.size >= 3) {
                        val isHidden = parts[1].toBoolean()
                        val siteName = parts[2]

                        Log.d("GeofenceReceiver", "Masuk Area: $siteName (Hidden: $isHidden)")

                        if (isHidden) {
                            NotificationHelper.showGeofenceNotification(
                                context = context,
                                title = "Situs Rahasia Ditemukan! 🕵️‍♂️",
                                message = "Kamu menemukan jejak tersembunyi '$siteName'. Buka peta sekarang!"
                            )
                        } else {
                            NotificationHelper.showGeofenceNotification(
                                context = context,
                                title = "Chapter Terbuka! 🔓",
                                message = "Kamu telah sampai di $siteName. Ketuk untuk membuka cerita."
                            )
                        }
                    } else {
                        NotificationHelper.showGeofenceNotification(
                            context = context,
                            title = "Lokasi Terdeteksi",
                            message = "Kamu memasuki area bersejarah."
                        )
                    }
                } catch (e: Exception) {
                    Log.e("GeofenceReceiver", "Gagal parsing data: ${e.message}")
                }
            }
        }
    }
}
