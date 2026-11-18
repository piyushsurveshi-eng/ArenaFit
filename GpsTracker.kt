package com.arenafit.app.gps

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Minimal foreground-only GPS tracker. For tracking with the screen off, move this into a
 * foreground Service (type "location") with a notification.
 */
class GpsTracker(context: Context) : LocationListener {
    private val lm = context.applicationContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    val distanceM = MutableStateFlow(0f)
    val points = MutableStateFlow<List<Pair<Double, Double>>>(emptyList())
    private var last: Location? = null

    @SuppressLint("MissingPermission")
    fun start() {
        distanceM.value = 0f
        points.value = emptyList()
        last = null
        try {
            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000L, 3f, this)
        } catch (_: Exception) {
            // permission missing or provider unavailable
        }
    }

    fun stop() {
        try {
            lm.removeUpdates(this)
        } catch (_: Exception) {
        }
    }

    override fun onLocationChanged(location: Location) {
        last?.let {
            val d = it.distanceTo(location)
            if (d in 2f..100f) distanceM.value = distanceM.value + d // ignore jitter and jumps
        }
        last = location
        points.value = points.value + (location.latitude to location.longitude)
    }

    // Required on API < 30 where these are abstract.
    @Deprecated("Deprecated in Java")
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}
}
