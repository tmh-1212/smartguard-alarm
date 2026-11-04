package com.example.myalarmapp.security

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import com.example.myalarmapp.AlarmApplication
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Helper class to fetch location using GPS, Cellular Towers, and Wi-Fi simultaneously (Hybrid Provider),
 * with Room DB fallback so it never fails.
 */
class LocationHelper(private val context: Context) {

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): Location? = suspendCancellableCoroutine { continuation ->
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        if (locationManager == null) {
            continuation.resume(getFallbackLocationFromDb())
            return@suspendCancellableCoroutine
        }

        val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (!hasFine && !hasCoarse) {
            continuation.resume(getFallbackLocationFromDb())
            return@suspendCancellableCoroutine
        }

        try {
            val lastGps = if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            } else null

            val lastNetwork = if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            } else null

            val bestLast = when {
                lastGps != null && lastNetwork != null -> if (lastGps.time > lastNetwork.time) lastGps else lastNetwork
                lastGps != null -> lastGps
                else -> lastNetwork
            }

            if (bestLast != null) {
                continuation.resume(bestLast)
                return@suspendCancellableCoroutine
            }

            val locationListener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    try {
                        locationManager.removeUpdates(this)
                        if (continuation.isActive) {
                            continuation.resume(location)
                        }
                    } catch (_: Exception) {}
                }
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}
            }

            // Request updates from both GPS and Network (Cellular/Wi-Fi) simultaneously for maximum reliability
            var requestedAny = false
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                try {
                    locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0L, 0f, locationListener, Looper.getMainLooper())
                    requestedAny = true
                } catch (_: Exception) {}
            }
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                try {
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0L, 0f, locationListener, Looper.getMainLooper())
                    requestedAny = true
                } catch (_: Exception) {}
            }

            if (!requestedAny) {
                continuation.resume(getFallbackLocationFromDb())
                return@suspendCancellableCoroutine
            }

            // Timeout fallback after 3.5 seconds
            Handler(Looper.getMainLooper()).postDelayed({
                try {
                    locationManager.removeUpdates(locationListener)
                    if (continuation.isActive) {
                        continuation.resume(getFallbackLocationFromDb())
                    }
                } catch (_: Exception) {}
            }, 3500L)

        } catch (e: SecurityException) {
            continuation.resume(getFallbackLocationFromDb())
        } catch (e: Exception) {
            continuation.resume(getFallbackLocationFromDb())
        }
    }

    private fun getFallbackLocationFromDb(): Location? {
        return try {
            val app = context.applicationContext as? AlarmApplication
            val logs = app?.database?.securityDao()?.getAllLogsSync()
            val lastLogWithCoords = logs?.firstOrNull { it.latitude != null && it.longitude != null }
            if (lastLogWithCoords?.latitude != null && lastLogWithCoords.longitude != null) {
                Location("RoomDbFallback").apply {
                    latitude = lastLogWithCoords.latitude!!
                    longitude = lastLogWithCoords.longitude!!
                }
            } else {
                Location("DefaultFallback").apply {
                    latitude = 9.03
                    longitude = 38.74 // Addis Ababa default center
                }
            }
        } catch (_: Exception) {
            null
        }
    }
}
