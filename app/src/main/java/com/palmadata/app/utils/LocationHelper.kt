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

    // Filtro de precisión para TRACKS — descarta puntos con accuracy peor a 30 metros
    private val MAX_ACCURACY_TRACKS_METROS = 30f
    // Filtro más laxo para ÚLTIMA UBICACIÓN (usada por los formularios al guardar):
    // bajo dosel de palma la precisión empeora, pero un punto de 50 m reciente
    // es mejor que uno perfecto de hace horas (o que 0,0 si nunca hubo fix).
    private val MAX_ACCURACY_ULTIMA_UBICACION_METROS = 50f

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val location: Location = result.lastLocation ?: return

            // Última ubicación para formularios: umbral laxo
            if (location.accuracy <= MAX_ACCURACY_ULTIMA_UBICACION_METROS) {
                SessionManager.saveLastLocation(context, location.latitude, location.longitude)
                onLocationUpdate(location.latitude, location.longitude)
            }

            // Tracks del recorrido: umbral estricto
            if (location.accuracy > MAX_ACCURACY_TRACKS_METROS) return

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
        return hora in 6..15
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

        // Invariante: el fertilizante SOLO acompaña tracks del módulo de
        // fertilización (formulario 25). Cubre el caso borde: estando en el
        // mapa, el operario toca la notificación → MainActivity se abre encima
        // (el mapa NO se destruye, así que los fertilizantes no se limpian) →
        // entra a otro módulo → sin esta guarda, esos tracks saldrían con el
        // formulario del otro módulo pero arrastrando el fertilizante viejo.
        val formularioActivo = SessionManager.getFormularioActivo(context)
        val fertilizantesTrack =
            if (formularioActivo == 25) SessionManager.getFertilizantesActivos(context)  // "[]" o "[1,2,...]"
            else "[]"

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
            formulario   = formularioActivo,  // id del módulo activo, 0 si está en la pantalla principal
            idunico      = UUID.randomUUID().toString(),
            equipo       = SessionManager.getEquipoId(context),
            fertilizante = fertilizantesTrack
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