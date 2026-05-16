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

    // Track cada 5 segundos
    private val locationRequest = LocationRequest.Builder(
        Priority.PRIORITY_HIGH_ACCURACY, 5_000L
    ).apply {
        setMinUpdateIntervalMillis(5_000L)
        setWaitForAccurateLocation(false)
    }.build()

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val location: Location = result.lastLocation ?: return

            val lat = location.latitude
            val lon = location.longitude

            // Guardar última ubicación conocida
            SessionManager.saveLastLocation(context, lat, lon)

            // Notificar UI
            onLocationUpdate(lat, lon)

            // Registrar track solo si está en horario laboral
            if (enHorarioLaboral()) {
                val track = construirTrack(location)
                TrackStorage.guardarTrack(context, track)
                onTrackGuardado?.invoke(track)
            }
        }
    }

    private var isTracking = false

    // ── Verificación de horario ──────────────────────────────────────────────

    /**
     * Retorna true si la hora actual está entre 6:00 AM y 2:59 PM
     * y no es domingo.
     */
    fun enHorarioLaboral(): Boolean {
        val cal = Calendar.getInstance()
        val hora = cal.get(Calendar.HOUR_OF_DAY)   // 0-23
        val dia  = cal.get(Calendar.DAY_OF_WEEK)   // 1=Dom, 2=Lun...

        val esDomingo   = dia == Calendar.SUNDAY
        val enHorario   = hora in 6..14             // 6:00 AM a 2:59 PM

        return !esDomingo && enHorario
    }

    // ── Construcción del track ────────────────────────────────────────────────

    private fun construirTrack(location: Location): TrackMovil {
        val ahora = Date()
        val fmtFecha = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val fmtHora  = SimpleDateFormat("HH:mm:ss",   Locale.getDefault())

        val worker     = SessionManager.getCurrentWorker(context)
        val plantacion = SessionManager.getCurrentPlantacion(context)

        return TrackMovil(
            x           = location.latitude,
            y           = location.longitude,
            velocidad   = location.speed.toDouble(),
            precision   = location.accuracy.toDouble(),
            sentido     = location.bearing.toDouble(),
            proveedor   = "fused",
            fecha       = fmtFecha.format(ahora),
            hora        = fmtHora.format(ahora),
            trabajador  = worker?.code?.toIntOrNull() ?: 0,
            plantacionId = plantacion?.id?.toLongOrNull() ?: 0L,
            formulario  = 0,   // TODO: tomar módulo activo
            idunico     = UUID.randomUUID().toString(),
            equipo      = SessionManager.getEquipoId(context)
        )
    }

    // ── Permisos ─────────────────────────────────────────────────────────────

    fun hasPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    context, Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }

    // ── Control de tracking ───────────────────────────────────────────────────

    @SuppressLint("MissingPermission")
    fun startLocationUpdates() {
        if (isTracking || !hasPermissions()) return
        fusedClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
        isTracking = true

        // Obtener última ubicación de inmediato
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