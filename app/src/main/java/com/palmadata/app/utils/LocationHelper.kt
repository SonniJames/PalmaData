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

    companion object {
        // ── Frecuencia del GPS (cada cuánto llega una posición) ────────────────
        // Normal: 5 s (equilibra precisión de recorrido y batería).
        const val INTERVALO_NORMAL_MS = 4_000L
        // Fertilización: 1 s, igual que OruxMaps. Sirve para DETECTAR el cambio
        // de UMA rápido: con 3 fixes de confirmación, la alerta baja de 15 s a
        // ~3 s. NO significa más tracks guardados (ver PERIODO_GUARDADO_MS).
        const val INTERVALO_RAPIDO_MS = 1_000L

        // ── Frecuencia de GUARDADO de tracks (independiente del GPS) ───────────
        // Siempre 5 s, en TODOS los módulos. Aunque en fertilización el GPS
        // entregue una posición por segundo, solo se guarda un track cada 5 s:
        // así la detección es rápida sin multiplicar por 5 el volumen de tracks.
        const val PERIODO_GUARDADO_MS = 4_000L
    }

    // Intervalo actual del GPS (cambia en caliente al entrar/salir de fertilización)
    private var intervaloActualMs = INTERVALO_NORMAL_MS

    // Momento del último track guardado (para el portero de los 5 s)
    private var ultimoGuardadoMs = 0L

    private fun construirLocationRequest(intervaloMs: Long): LocationRequest =
        LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervaloMs).apply {
            setMinUpdateIntervalMillis(intervaloMs)
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

            // ── 1. Última ubicación para formularios: umbral laxo, cada fix ────
            if (location.accuracy <= MAX_ACCURACY_ULTIMA_UBICACION_METROS) {
                SessionManager.saveLastLocation(context, location.latitude, location.longitude)
                // Este callback alimenta la DETECCIÓN de UMAs del TrackingService:
                // se invoca en CADA fix (a 1 s en fertilización) para que el cambio
                // de UMA se confirme rápido.
                onLocationUpdate(location.latitude, location.longitude)
            }

            // ── 2. Guardado de tracks: umbral estricto + portero de 5 s ────────
            if (location.accuracy > MAX_ACCURACY_TRACKS_METROS) return
            if (!enHorarioLaboral()) return
            if (SessionManager.isJornadaCerradaHoy(context)) return  // cerrada = no más tracks hoy
            // Aunque el GPS venga a 1 s, solo se guarda un track cada 5 s.
            // Así fertilización NO genera más tracks que los demás módulos.
            // El margen de 500 ms evita descartar un track legítimo del modo
            // normal (GPS a 5 s) por unos milisegundos de desfase del sistema.
            val ahoraMs = System.currentTimeMillis()
            if (ahoraMs - ultimoGuardadoMs < PERIODO_GUARDADO_MS - 500L) return
            ultimoGuardadoMs = ahoraMs

            val track = construirTrack(location)
            TrackStorage.guardarTrack(context, track)
            onTrackGuardado?.invoke(track)
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
        fusedClient.requestLocationUpdates(
            construirLocationRequest(intervaloActualMs), locationCallback, Looper.getMainLooper()
        )
        isTracking = true
        fusedClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                SessionManager.saveLastLocation(context, it.latitude, it.longitude)
                onLocationUpdate(it.latitude, it.longitude)
            }
        }
    }

    /**
     * Cambia el intervalo de GPS en caliente. El TrackingService lo usa para
     * pasar a 2 s cuando el operario entra al módulo de fertilización (alerta de
     * cambio de UMA más rápida) y volver a 5 s al salir (cuidar batería).
     * Reinicia los updates solo si el intervalo realmente cambió.
     */
    @SuppressLint("MissingPermission")
    fun setIntervalo(intervaloMs: Long) {
        if (intervaloMs == intervaloActualMs) return
        intervaloActualMs = intervaloMs
        if (isTracking) {
            fusedClient.removeLocationUpdates(locationCallback)
            fusedClient.requestLocationUpdates(
                construirLocationRequest(intervaloActualMs), locationCallback, Looper.getMainLooper()
            )
        }
    }

    fun stopLocationUpdates() {
        if (!isTracking) return
        fusedClient.removeLocationUpdates(locationCallback)
        isTracking = false
    }

    fun isTracking(): Boolean = isTracking
}