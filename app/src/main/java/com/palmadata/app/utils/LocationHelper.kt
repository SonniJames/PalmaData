package com.palmadata.app.utils

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.palmadata.app.data.model.TrackMovil
import java.text.SimpleDateFormat
import java.util.*

class LocationHelper(
    private val context: Context,
    private val onLocationUpdate: (lat: Double, lon: Double) -> Unit,
    private val onTrackGuardado: ((TrackMovil) -> Unit)? = null
) {

    private val fusedClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private val locationRequest = LocationRequest.Builder(
        Priority.PRIORITY_HIGH_ACCURACY, 5_000L
    ).apply {
        setMinUpdateIntervalMillis(5_000L)
        setWaitForAccurateLocation(false)
    }.build()

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val location: Location = result.lastLocation ?: return
            SessionManager.saveLastLocation(context, location.latitude, location.longitude)
            onLocationUpdate(location.latitude, location.longitude)

            if (enHorarioLaboral()) {
                val track = construirTrack(location)
                TrackStorage.guardarTrack(context, track)
                onTrackGuardado?.invoke(track)
            }
        }
    }

    private var isTracking = false

    fun enHorarioLaboral(): Boolean {
        val cal  = Calendar.getInstance()
        val hora = cal.get(Calendar.HOUR_OF_DAY)
        return hora in 6..13
    }

    private fun construirTrack(location: Location): TrackMovil {
        val ahora    = Date()
        val fmtFecha = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val fmtHora  = SimpleDateFormat("HH:mm:ss",   Locale.getDefault())

        val trabajadorMaquinaria = SessionManager.getTrabajadorMaquinariaActivo(context)
        val trabajadorFinal = if (trabajadorMaquinaria > 0) {
            trabajadorMaquinaria
        } else {
            SessionManager.getCurrentWorker(context)?.code?.toIntOrNull() ?: 0
        }

        return TrackMovil(
            x            = location.latitude,
            y            = location.longitude,
            velocidad    = location.speed.toDouble(),
            precision    = location.accuracy.toDouble(),
            sentido      = location.bearing.toDouble(),
            proveedor    = "fused",
            fecha        = fmtFecha.format(ahora),
            hora         = fmtHora.format(ahora),
            trabajador   = trabajadorFinal,
            plantacionId = 0L,
            formulario   = 0,
            idunico      = UUID.randomUUID().toString(),
            equipo       = SessionManager.getEquipoId(context)
        )
    }

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