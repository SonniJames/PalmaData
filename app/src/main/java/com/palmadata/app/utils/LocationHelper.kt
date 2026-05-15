package com.palmadata.app.utils

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*

class LocationHelper(
    private val context: Context,
    private val onLocationUpdate: (lat: Double, lon: Double) -> Unit
) {

    private val fusedClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private val locationRequest = LocationRequest.Builder(
        Priority.PRIORITY_HIGH_ACCURACY, 15_000L
    ).apply {
        setMinUpdateIntervalMillis(10_000L)
        setWaitForAccurateLocation(false)
    }.build()

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val location: Location = result.lastLocation ?: return
            SessionManager.saveLastLocation(context, location.latitude, location.longitude)
            onLocationUpdate(location.latitude, location.longitude)
        }
    }

    private var isTracking = false

    fun hasPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    context, Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    fun startLocationUpdates() {
        if (isTracking || !hasPermissions()) return
        fusedClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        isTracking = true
        fusedClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                SessionManager.saveLastLocation(context, it.latitude, it.longitude)
                onLocationUpdate(it.latitude, it.longitude)
            }
        }
    }

    fun stopLocationUpdates() {
        if (!isTracking) return
        fusedClient.removeLocationUpdates(locationCallback)
        isTracking = false
    }

    fun isTracking(): Boolean = isTracking
}