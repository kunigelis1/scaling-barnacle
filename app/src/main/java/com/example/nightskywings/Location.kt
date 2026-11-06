package com.example.nightskywings

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.tasks.await

/**
 * Background workers can't read the location on Android 10+ without the background-location
 * permission. So the app reads it while it is open, saves it, and the workers use the saved value.
 */
object LocationStore {
    private const val PREFS = "nsw_location"

    fun save(ctx: Context, lat: Double, lon: Double) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString("lat", lat.toString()).putString("lon", lon.toString()).apply()
    }

    fun saved(ctx: Context): Pair<Double, Double>? {
        val p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val lat = p.getString("lat", null)?.toDoubleOrNull() ?: return null
        val lon = p.getString("lon", null)?.toDoubleOrNull() ?: return null
        return Pair(lat, lon)
    }
}

@SuppressLint("MissingPermission")
suspend fun currentLatLon(ctx: Context): Pair<Double, Double> {
    val granted = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED
    if (granted) {
        try {
            val loc = LocationServices.getFusedLocationProviderClient(ctx).lastLocation.await()
            if (loc != null) {
                LocationStore.save(ctx, loc.latitude, loc.longitude)
                return Pair(loc.latitude, loc.longitude)
            }
        } catch (e: Exception) {
            // Fall through to the saved or default location.
        }
    }
    return LocationStore.saved(ctx) ?: Pair(Config.DEFAULT_LAT, Config.DEFAULT_LON)
}
