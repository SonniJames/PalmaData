package com.palmadata.app.utils

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.GnssStatus
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.palmadata.app.data.model.TrackMovil
import java.text.SimpleDateFormat
import java.util.*

/**
 * Fuente de posición de la app, con estrategia HÍBRIDA: se prefiere el GPS
 * crudo y solo se cae a Fused cuando el GPS no está reportando.
 *
 * Por qué: el GPS crudo entrega la posición tal como la calcula el chip, sin
 * el suavizado que Fused aplica (pensado para navegación urbana). Para trazar
 * el recorrido real por los lotes eso es lo deseable. Pero bajo dosel cerrado
 * el chip puede quedarse sin fijar, y ahí Fused —que complementa con WiFi,
 * torres y sensores— evita que queden huecos en el recorrido.
 *
 * Cómo: se escucha a los DOS proveedores a la vez. Cada fix del GPS refresca
 * un reloj; un fix de Fused solo se acepta si ese reloj lleva rato sin
 * refrescarse. Hay DOS umbrales, porque alertas y tracks piden cosas opuestas:
 * ventanaAlertasMs (corta y relativa a la cadencia, prioriza no quedarse
 * sin posición) y VENTANA_GPS_TRACKS_MS (larga, prioriza no mezclar
 * fuentes en el análisis).
 * La columna `proveedor` de cada track guarda cuál fue el origen real, así se
 * puede medir en la base qué proporción viene de cada uno.
 */
class LocationHelper(
    private val context: Context,
    private val onLocationUpdate: (lat: Double, lon: Double) -> Unit,
    private val onTrackGuardado: ((TrackMovil) -> Unit)? = null
) {

    private val fusedClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    // ── Diagnóstico GNSS ─────────────────────────────────────────────────────
    // Se actualiza por callback del sistema y se adjunta a cada track. Es el
    // termómetro objetivo de la señal bajo dosel: permite descartar fixes malos
    // en el análisis con un criterio duro en vez de una corazonada.
    @Volatile private var satelitesUsados = 0
    @Volatile private var satelitesVisibles = 0

    private val gnssCallback = object : GnssStatus.Callback() {
        override fun onSatelliteStatusChanged(status: GnssStatus) {
            var usados = 0
            for (i in 0 until status.satelliteCount) if (status.usedInFix(i)) usados++
            satelitesUsados = usados
            satelitesVisibles = status.satelliteCount
        }
    }

    private val locationManager: LocationManager? =
        context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

    companion object {
        // ── Frecuencia del GPS (cada cuánto llega una posición) ────────────────
        // Normal: 4 s (equilibra precisión de recorrido y batería).
        const val INTERVALO_NORMAL_MS = 4_000L
        // Fertilización y mapas: 1 s, igual que OruxMaps. Sirve para DETECTAR el
        // cambio de UMA rápido: con 3 fixes de confirmación, la alerta baja de
        // 15 s a ~3 s. NO significa más tracks guardados (ver PERIODO_GUARDADO_MS).
        const val INTERVALO_RAPIDO_MS = 1_000L

        // ── Frecuencia de GUARDADO de tracks (independiente del GPS) ───────────
        // Siempre 4 s, en TODOS los módulos. Aunque en fertilización el GPS
        // entregue una posición por segundo, solo se guarda un track cada 4 s:
        // así la detección es rápida sin multiplicar el volumen de tracks.
        const val PERIODO_GUARDADO_MS = 4_000L

        // Estudio de tiempos: 2 s de GPS Y de guardado. Es el módulo del que
        // saldrán los modelos de productividad, así que necesita el doble de
        // resolución. Se lo puede permitir porque la jornada medida es corta
        // (se corta al presionar FINALIZAR JORNADA) y no corre todo el día.
        const val INTERVALO_TIEMPOS_MS       = 2_000L
        const val PERIODO_GUARDADO_TIEMPOS_MS = 2_000L

        // Ids de formulario que cambian la cadencia. Deben coincidir con
        // ModuleRegistry.
        const val FORMULARIO_FERTILIZACION = 25
        const val FORMULARIO_TIEMPOS       = 30

        // ── Estrategia híbrida: DOS ventanas, no una ──────────────────────────
        // Las alertas y el guardado de tracks tienen requisitos opuestos, así
        // que no pueden compartir umbral.
        //
        // ALERTAS (fertilización, detección de uma, punto en el mapa): lo que
        // importa es la latencia. Un fix de Fused, aunque sea menos preciso, es
        // infinitamente mejor que quedarse sin ninguna posición mientras el
        // operario camina aplicando la dosis equivocada. Bajo dosel cerrado el
        // GNSS se pierde con frecuencia, así que la espera debe ser corta.
        //
        // La ventana NO puede ser un valor fijo: debe superar siempre la
        // cadencia vigente del GPS. Si fuera menor, el silencio NORMAL entre
        // dos fixes sanos contaría como "GNSS caído" y Fused se colaría con
        // señal plena — en modo normal (GPS a 4 s) una ventana de 3 s dejaba
        // entrar una posición contaminada cada pocos segundos. Se calcula como
        // cadencia + margen: normal 6 s, tiempos 4 s, fertilización 3 s.
        const val MARGEN_VENTANA_ALERTAS_MS = 2_000L

        // TRACKS (analítica de recorrido): lo que importa es la pureza de la
        // fuente. Al perderse el GNSS, Fused rellena con red y sensores y esa
        // posición puede estar a decenas de metros; al volver el GNSS el salto
        // entre ambas fuentes aparece en los datos como un desplazamiento que
        // nunca ocurrió. Para el análisis de micromovimiento un hueco honesto
        // vale más que un punto inventado, así que aquí se es paciente.
        const val VENTANA_GPS_TRACKS_MS = 10_000L

        // Un fix más viejo que esto se descarta: Android puede servir una
        // posición guardada en caché que ya no representa dónde está el
        // operario.
        //
        // La edad se mide con elapsedRealtimeNanos (reloj monotónico desde el
        // arranque del equipo), NO con location.time. Ese detalle importa: la
        // hora del fix GPS viene de los satélites y la del equipo puede estar
        // desfasada; comparándolas, una tablet con el reloj corrido dejaría de
        // guardar tracks en silencio. El reloj monotónico es inmune a eso, así
        // que se puede usar un umbral estricto sin riesgo.
        const val EDAD_MAXIMA_FIX_NS = 20_000_000_000L   // 20 s reales

        private const val TAG = "LocationHelper"

        const val PROVEEDOR_GPS   = "gps"
        const val PROVEEDOR_FUSED = "fused"
    }

    // Intervalo actual del GPS (cambia en caliente al entrar/salir de fertilización)
    private var intervaloActualMs = INTERVALO_NORMAL_MS

    /** Ventana de alertas vigente: siempre por encima de la cadencia del GPS,
     *  para que un hueco esperado entre fixes sanos no se confunda con una
     *  pérdida de señal. */
    private val ventanaAlertasMs: Long
        get() = intervaloActualMs + MARGEN_VENTANA_ALERTAS_MS

    // Momento del último track guardado (para el portero de los 4 s)
    private var ultimoGuardadoMs = 0L

    // Momento del último fix recibido del GPS crudo. Es el que decide si un
    // fix de Fused se acepta o se descarta.
    @Volatile private var ultimoFixGpsMs = 0L

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

    // ── Entradas de los dos proveedores ───────────────────────────────────────

    /** Fixes del GPS crudo (LocationManager). Tienen prioridad absoluta. */
    private val gpsListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            procesarUbicacion(location, PROVEEDOR_GPS)
        }
        // Firmas heredadas de versiones antiguas: deben existir para compilar
        // contra minSdk bajo, aunque el sistema ya no las invoque.
        @Deprecated("Requerido por la interfaz en APIs antiguas")
        override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) { }
        override fun onProviderEnabled(provider: String) { }
        override fun onProviderDisabled(provider: String) { }
    }

    /** Fixes de Fused. Solo se usan cuando el GPS lleva rato sin reportar. */
    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val location: Location = result.lastLocation ?: return
            procesarUbicacion(location, PROVEEDOR_FUSED)
        }
    }

    /**
     * Punto único por donde pasan las posiciones de ambos proveedores.
     *
     * El filtro de proveedor se aplica en DOS puntos distintos, no en uno:
     * primero con la ventana corta (alertas) y más abajo con la larga (tracks).
     * Así, cuando el GNSS se pierde bajo dosel, la detección de umas se recupera
     * en segundos mientras el recorrido guardado conserva un único origen.
     */
    private fun procesarUbicacion(location: Location, proveedor: String) {
        val ahoraMs = System.currentTimeMillis()

        // Fix servido desde caché del sistema: no dice dónde está el operario
        // ahora. Se descarta antes de tocar nada.
        if (esFixViejo(location)) return

        // Cuánto lleva el GNSS sin reportar. Se calcula ANTES de actualizar el
        // reloj, para que un fix de GPS no invalide su propia medición.
        val silencioGpsMs = ahoraMs - ultimoFixGpsMs

        if (proveedor == PROVEEDOR_GPS) {
            ultimoFixGpsMs = ahoraMs
        } else if (silencioGpsMs < ventanaAlertasMs) {
            // El GNSS está vivo: este fix de Fused no aporta nada y solo
            // metería ruido. Se descarta por completo.
            return
        }

        // ── 1. Última ubicación y ALERTAS ─────────────────────────────────────
        // Llegan aquí los fixes del GNSS y, cuando este lleva más de la ventana
        // corta callado, también los de Fused. Es el camino que alimenta la
        // detección de umas del TrackingService: se invoca en CADA fix (a 1 s en
        // fertilización) para que el cambio de uma se confirme rápido.
        if (location.accuracy <= MAX_ACCURACY_ULTIMA_UBICACION_METROS) {
            SessionManager.saveLastLocation(context, location.latitude, location.longitude)
            // Hora del fix, para medir el desfase del reloj del equipo. Va aquí
            // y no en el guardado de tracks: el módulo de supervisión de tiempos
            // la necesita en CADA fix, también fuera del horario laboral o con
            // la jornada ya cerrada, cuando no se graban tracks.
            SessionManager.saveLastFixTime(context, location.time)
            onLocationUpdate(location.latitude, location.longitude)
        }

        // ── 2. Guardado de tracks ─────────────────────────────────────────────
        // Aquí sí se exige la ventana larga: un fix de Fused solo se graba si el
        // GNSS lleva medio minuto sin dar señales. Los que quedaron fuera ya
        // sirvieron para alertar, que es lo urgente.
        if (proveedor != PROVEEDOR_GPS && silencioGpsMs < VENTANA_GPS_TRACKS_MS) return

        if (location.accuracy > MAX_ACCURACY_TRACKS_METROS) return
        if (!enHorarioLaboral()) return
        if (SessionManager.isJornadaCerradaHoy(context)) return  // cerrada = no más tracks hoy
        // Aunque el GPS venga a 1 s, solo se guarda un track cada 4 s.
        // El margen de 500 ms evita descartar un track legítimo del modo
        // normal por unos milisegundos de desfase del sistema.
        if (ahoraMs - ultimoGuardadoMs < periodoGuardadoActual() - 500L) return
        ultimoGuardadoMs = ahoraMs

        val track = construirTrack(location, proveedor)
        TrackStorage.guardarTrack(context, track)
        onTrackGuardado?.invoke(track)
    }

    /**
     * Cada cuánto se guarda un track, según el módulo activo.
     *
     * Solo el estudio de tiempos baja a 2 s; los demás mantienen 4 s. Se
     * consulta en cada fix, así que entrar o salir del módulo cambia la
     * cadencia sin necesidad de reiniciar nada.
     */
    private fun periodoGuardadoActual(): Long =
        if (SessionManager.getFormularioActivo(context) == FORMULARIO_TIEMPOS)
            PERIODO_GUARDADO_TIEMPOS_MS
        else
            PERIODO_GUARDADO_MS

    /** ¿El fix es demasiado viejo para representar la posición actual?
     *  Usa el reloj monotónico del sistema, inmune al desfase de hora. */
    private fun esFixViejo(location: Location): Boolean {
        val marcaNs = location.elapsedRealtimeNanos
        if (marcaNs <= 0L) return false          // sin marca: no se puede juzgar
        return SystemClock.elapsedRealtimeNanos() - marcaNs > EDAD_MAXIMA_FIX_NS
    }

    private var isTracking = false

    fun enHorarioLaboral(): Boolean {
        val cal  = Calendar.getInstance()
        val hora = cal.get(Calendar.HOUR_OF_DAY)
        return hora in 6..15
    }

    private fun construirTrack(location: Location, proveedor: String): TrackMovil {
        // Marca de tiempo DEL FIX, no del momento de guardar: entre que el chip
        // calcula la posición y llega este callback hay latencia variable, y el
        // sistema puede entregar varios fixes seguidos. Si el fix no trae hora
        // (caso raro), se usa la del equipo.
        val instanteFix = if (location.time > 0L) Date(location.time) else Date()
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
            // hasSpeed()/hasBearing() distinguen "el chip no reportó el dato"
            // de "el dato es cero". Sin la comprobación Android devuelve 0.0 en
            // ambos casos, y un operario detenido se vuelve indistinguible de
            // uno cuya velocidad simplemente no se midió — justo la señal sobre
            // la que se detectan los descansos.
            //
            // Importa especialmente desde el cambio a GPS crudo: Fused siempre
            // entregaba un valor (salida de su filtro), mientras que el chip
            // GNSS marca hasSpeed() = false cuando no tiene enganche Doppler
            // suficiente. Es más honesto, pero exige registrar ese "no sé".
            //
            // Se guarda NULL, no un centinela: la columna admite nulos, así que
            // el pipeline distingue con "velocidad IS NULL" sin convenciones
            // que haya que recordar. Un AVG(velocidad) ignora los nulos solo.
            velocidad    = if (location.hasSpeed()) location.speed.toDouble() else null,
            precision    = location.accuracy.toDouble(),
            sentido      = if (location.hasBearing()) location.bearing.toDouble() else null,
            // Cuánto confiar en esa velocidad. Sin este dato, 0,3 m/s puede ser
            // movimiento real o ruido del chip, y no hay forma de saberlo.
            precisionVelocidad =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && location.hasSpeedAccuracy())
                    location.speedAccuracyMetersPerSecond.toDouble()
                else null,
            satelites    = satelitesUsados,
            satVisibles  = satelitesVisibles,
            // Origen real del fix: permite medir en la base qué proporción del
            // recorrido vino del GPS crudo y cuánta del respaldo.
            proveedor    = proveedor,
            fecha        = fmtFecha.format(instanteFix),
            hora         = fmtHora.format(instanteFix),
            trabajador   = trabajadorFinal,
            plantacionId = 0L,
            formulario   = formularioActivo,  // id del módulo activo, 0 si está en la pantalla principal
            idunico      = UUID.randomUUID().toString(),
            equipo       = SessionManager.getEquipoId(context),
            idEquipo     = SessionManager.getIdEquipo(context),
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

    /** El GPS crudo exige FINE_LOCATION. Con solo COARSE la app funciona,
     *  pero el 100% de los tracks vendría de Fused: conviene poder detectarlo. */
    fun tienePermisoFino(): Boolean =
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    fun startLocationUpdates() {
        if (isTracking || !hasPermissions()) return

        // Fused: respaldo. Se pide a la MITAD de cadencia que el GPS: como sus
        // fixes se descartan mientras el GNSS reporte, pedirlos al mismo ritmo
        // solo gastaría batería.
        fusedClient.requestLocationUpdates(
            construirLocationRequest(intervaloActualMs * 2), locationCallback, Looper.getMainLooper()
        )

        // GPS crudo: la fuente preferida.
        pedirGps()

        isTracking = true

        // Semilla inicial para que los formularios no arranquen sin posición.
        // Se prefiere el último fix del GNSS, PERO exigiéndole lo mismo que a
        // cualquier otro: getLastKnownLocation devuelve el último fix guardado
        // sin límite de antigüedad — puede ser del parqueadero de la semana
        // pasada. Sin este filtro se alimentaría al detector de umas y a los
        // formularios con una posición falsa.
        val ultimaGnss = try {
            locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?.takeIf { !esFixViejo(it) && it.accuracy <= MAX_ACCURACY_ULTIMA_UBICACION_METROS }
        } catch (e: Exception) { null }

        if (ultimaGnss != null) {
            SessionManager.saveLastLocation(context, ultimaGnss.latitude, ultimaGnss.longitude)
            onLocationUpdate(ultimaGnss.latitude, ultimaGnss.longitude)
        } else {
            fusedClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    SessionManager.saveLastLocation(context, it.latitude, it.longitude)
                    onLocationUpdate(it.latitude, it.longitude)
                }
            }
        }
    }

    /** Suscribe el GPS crudo. Si el proveedor no existe o está apagado, la app
     *  sigue funcionando solo con Fused. */
    @SuppressLint("MissingPermission")
    private fun pedirGps() {
        val lm = locationManager
        if (lm == null) {
            android.util.Log.w(TAG, "Sin LocationManager: todo el recorrido vendrá de Fused")
            return
        }
        if (!tienePermisoFino()) {
            // Con solo COARSE el GPS crudo no arranca. Sin este aviso, el equipo
            // correría meses al 100% Fused sin que nadie lo note.
            android.util.Log.w(TAG, "Sin permiso FINE_LOCATION: todo el recorrido vendrá de Fused")
            return
        }
        if (!lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            android.util.Log.w(TAG, "GPS apagado en el equipo: todo el recorrido vendrá de Fused")
            return
        }
        try {
            lm.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                intervaloActualMs,
                0f,                       // sin filtro de distancia: lo hace el portero de guardado
                gpsListener,
                Looper.getMainLooper()
            )
            // Estado de satélites: alimenta las columnas satelites/sat_visibles
            try { lm.registerGnssStatusCallback(gnssCallback, Handler(Looper.getMainLooper())) }
            catch (e: Exception) { android.util.Log.w(TAG, "Sin estado GNSS: ${e.message}") }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "No se pudo suscribir el GPS crudo: ${e.message}")
        }
    }

    private fun quitarGps() {
        try { locationManager?.removeUpdates(gpsListener) } catch (e: Exception) { }
        try { locationManager?.unregisterGnssStatusCallback(gnssCallback) } catch (e: Exception) { }
    }

    /**
     * Cambia el intervalo de posición en caliente. El TrackingService lo usa
     * para pasar a 1 s cuando el operario entra al módulo de fertilización
     * (alerta de cambio de UMA más rápida) y volver a 4 s al salir (batería).
     * Reinicia AMBOS proveedores solo si el intervalo realmente cambió.
     */
    @SuppressLint("MissingPermission")
    fun setIntervalo(intervaloMs: Long) {
        if (intervaloMs == intervaloActualMs) return
        intervaloActualMs = intervaloMs
        if (isTracking) {
            fusedClient.removeLocationUpdates(locationCallback)
            fusedClient.requestLocationUpdates(
                construirLocationRequest(intervaloActualMs * 2), locationCallback, Looper.getMainLooper()
            )
            quitarGps()
            pedirGps()
        }
    }

    fun stopLocationUpdates() {
        if (!isTracking) return
        fusedClient.removeLocationUpdates(locationCallback)
        quitarGps()
        isTracking = false
    }

    fun isTracking(): Boolean = isTracking
}