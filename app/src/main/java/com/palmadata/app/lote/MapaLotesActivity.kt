package com.palmadata.app.mapa

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import android.os.SystemClock
import android.view.WindowManager
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.palmadata.app.R
import com.palmadata.app.data.model.LoteMapa
import com.palmadata.app.data.model.PalmaMapa
import com.palmadata.app.utils.DatabaseHelper
import com.palmadata.app.utils.LoteLocator
import com.palmadata.app.utils.LotePoligono
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.cachemanager.CacheManager
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

/**
 * Módulo MAPAS: ubicación en la plantación.
 *
 * Es informativo, no de labor. Muestra los polígonos de los lotes sobre la
 * imagen satelital, ubica al operario y le dice en qué lote está parado con
 * sus características (siembra, palmas, material).
 *
 * Diferencias con el módulo de fertilización:
 *  - formularioId = 0: no marca los tracks, porque no corresponde a una labor.
 *  - Sin voz, sin vibración, sin notificación: solo actualiza la caja en
 *    pantalla. Por lo mismo NO necesita un motor que sobreviva a la pantalla
 *    apagada; la detección vive aquí y se apaga al salir.
 *  - Sin filtro de fertilizantes.
 *
 * La posición la pide esta pantalla directamente al proveedor (1 s, igual que
 * fertilización) y NO guarda tracks: de eso sigue encargándose el
 * TrackingService por su cuenta, con su propia cadencia.
 */
class MapaLotesActivity : AppCompatActivity() {

    private lateinit var mapView: MapView
    private lateinit var tvNombre: TextView
    private lateinit var tvInfo: TextView
    private lateinit var tvDetalle: TextView
    private lateinit var btnCentrar: FloatingActionButton
    private lateinit var btnDescargarMapa: FloatingActionButton
    private lateinit var btnActualizarLotes: FloatingActionButton
    private lateinit var btnPalmas: FloatingActionButton
    private lateinit var tvPalmaInfo: TextView

    private val lotesOverlay = LotesOverlay()
    private val palmasOverlay = PalmasOverlay()
    private var lotesDibujados = false

    @Volatile private var poligonos: List<LotePoligono> = emptyList()

    // Bounding box de todos los lotes (para centrar y para la descarga offline)
    private var lotesMinLat =  90.0; private var lotesMaxLat = -90.0
    private var lotesMinLon = 180.0; private var lotesMaxLon = -180.0

    // ── Detección con histéresis ──────────────────────────────────────────────
    // Sin ella, estando sobre el lindero el GPS salta unos metros y la caja
    // parpadearía entre dos lotes. Se exige confirmación de 3 fixes seguidos.
    private var loteActualId: Int? = null
    private var loteCandidatoId: Int? = null
    private var fixesCandidato = 0

    private lateinit var fusedClient: FusedLocationProviderClient
    private var locationManager: LocationManager? = null
    private var pidiendoUbicacion = false

    /** Momento del último fix del GPS crudo: decide si se acepta uno de Fused. */
    private var ultimoFixGpsMs = 0L

    /** GPS crudo: fuente preferida. */
    private val gpsListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            recibirUbicacion(location, esGps = true)
        }
        @Deprecated("Requerido por la interfaz en APIs antiguas")
        override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) { }
        override fun onProviderEnabled(provider: String) { }
        override fun onProviderDisabled(provider: String) { }
    }

    /** Fused: respaldo, solo cuando el GPS lleva rato callado. */
    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val loc = result.lastLocation ?: return
            recibirUbicacion(loc, esGps = false)
        }
    }

    /**
     * Punto único por donde pasan las posiciones de ambos proveedores: aquí se
     * aplica la preferencia por GPS y se descartan los fixes viejos de caché.
     */
    private fun recibirUbicacion(location: Location, esGps: Boolean) {
        if (esFixViejo(location)) return

        val ahoraMs = System.currentTimeMillis()
        if (esGps) {
            ultimoFixGpsMs = ahoraMs
        } else {
            if (ahoraMs - ultimoFixGpsMs < VENTANA_GPS_MS) return
        }
        procesarPosicion(location.latitude, location.longitude)
    }

    /** ¿El fix es demasiado viejo para representar la posición actual? */
    private fun esFixViejo(location: Location): Boolean {
        val marcaNs = location.elapsedRealtimeNanos
        if (marcaNs <= 0L) return false
        return SystemClock.elapsedRealtimeNanos() - marcaNs > EDAD_MAXIMA_FIX_NS
    }

    companion object {
        private const val FIXES_PARA_CAMBIO = 3
        private const val INTERVALO_MS = 1_000L

        // Estrategia híbrida, igual que LocationHelper: se prefiere el GPS
        // crudo y solo se acepta Fused cuando el chip lleva rato sin reportar.
        // La ventana es corta (3 s) porque este módulo es informativo: para
        // responder "en qué lote estoy" es mejor una posición aproximada que
        // ninguna. Los tracks que se analizan usan una ventana más larga.
        private const val VENTANA_GPS_MS = 3_000L

        // Un fix más viejo que esto se descarta. Se mide con el reloj
        // monotónico, inmune al desfase de hora del equipo.
        private const val EDAD_MAXIMA_FIX_NS = 20_000_000_000L   // 20 s reales

        private const val TAG = "MapaLotes"

        /** Mapa base satelital (Esri World Imagery), el mismo de fertilización:
         *  permite descarga offline y muestra las hileras de palma reales. */
        private val FUENTE_SATELITAL = object : OnlineTileSourceBase(
            "EsriWorldImagery",
            0, 19, 256, "",
            arrayOf("https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/"),
            "Esri, Maxar, Earthstar Geographics"
        ) {
            override fun getTileURLString(pMapTileIndex: Long): String =
                baseUrl +
                        MapTileIndex.getZoom(pMapTileIndex) + "/" +
                        MapTileIndex.getY(pMapTileIndex) + "/" +
                        MapTileIndex.getX(pMapTileIndex)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        Configuration.getInstance().apply {
            userAgentValue = packageName
            osmdroidBasePath = filesDir
            osmdroidTileCache = cacheDir
            expirationOverrideDuration = 365L * 24 * 60 * 60 * 1000
            tileFileSystemCacheMaxBytes  = 1024L * 1024 * 1024   // 1 GB
            tileFileSystemCacheTrimBytes = 900L  * 1024 * 1024
        }

        setContentView(R.layout.activity_mapa_lotes)

        mapView            = findViewById(R.id.mapView)
        tvNombre           = findViewById(R.id.tvLoteNombre)
        tvInfo             = findViewById(R.id.tvLoteInfo)
        tvDetalle          = findViewById(R.id.tvLoteDetalle)
        btnCentrar         = findViewById(R.id.btnCentrar)
        btnDescargarMapa   = findViewById(R.id.btnDescargarMapa)
        btnActualizarLotes = findViewById(R.id.btnActualizarLotes)
        btnPalmas          = findViewById(R.id.btnPalmas)
        tvPalmaInfo        = findViewById(R.id.tvPalmaInfo)

        fusedClient = LocationServices.getFusedLocationProviderClient(this)
        locationManager = getSystemService(Context.LOCATION_SERVICE) as? LocationManager

        @SuppressLint("MissingPermission")
        btnCentrar.setOnClickListener {
            fusedClient.lastLocation.addOnSuccessListener { loc ->
                if (loc != null) mapView.controller.animateTo(GeoPoint(loc.latitude, loc.longitude))
            }
        }
        btnDescargarMapa.setOnClickListener { confirmarDescargaMapaOffline() }
        btnActualizarLotes.setOnClickListener { recargarLotes() }
        btnPalmas.setOnClickListener { cargarPalmasDelLoteActual() }

        // El overlay avisa qué palma se tocó; la pantalla solo actualiza el panel.
        palmasOverlay.onSeleccion = { palma -> mostrarPalma(palma) }
        palmasOverlay.onZoomInsuficiente = {
            android.widget.Toast.makeText(this,
                "Acerque el mapa para seleccionar una palma",
                android.widget.Toast.LENGTH_SHORT).show()
        }

        mapView.setTileSource(FUENTE_SATELITAL)
        mapView.setMultiTouchControls(true)
        mapView.minZoomLevel = 3.0
        // Hasta 20 aunque solo haya teselas hasta 17: osmdroid escala la del
        // zoom vecino, así siempre hay imagen en vez del fondo gris.
        mapView.maxZoomLevel = 20.0
        mapView.setUseDataConnection(true)

        configurarMiUbicacion()

        tvNombre.text = "Cargando lotes..."
        cargarLotes()
    }

    // ── Carga de polígonos desde la BD local ──────────────────────────────────

    private fun cargarLotes() {
        Thread {
            val cargados = try {
                DatabaseHelper.getInstance(this).getLotesMapa().mapNotNull { lote ->
                    try { LotePoligono(lote) } catch (e: Exception) { null }  // geojson inválido: se omite
                }
            } catch (e: Exception) {
                emptyList()
            }
            poligonos = cargados
            runOnUiThread {
                dibujarLotes()
                centrarComoOrux()
            }
        }.start()
    }

    private fun recargarLotes() {
        mapView.overlays.remove(lotesOverlay)
        // El mismo botón redibuja ambas capas: si los lotes cambiaron, las
        // palmas que estaban pintadas pueden pertenecer a un lote que ya no
        // existe o cambió de forma.
        quitarPalmas()
        lotesDibujados = false
        lotesMinLat =  90.0; lotesMaxLat = -90.0
        lotesMinLon = 180.0; lotesMaxLon = -180.0
        loteActualId = null; loteCandidatoId = null; fixesCandidato = 0

        tvNombre.text = "Recargando lotes..."
        android.widget.Toast.makeText(this, "Recargando lotes...", android.widget.Toast.LENGTH_SHORT).show()
        cargarLotes()
    }

    private fun dibujarLotes() {
        if (lotesDibujados) return
        if (poligonos.isEmpty()) {
            tvNombre.text = "Sin lotes cargados"
            tvInfo.text   = "Sincronice para descargarlos"
            tvDetalle.text = ""
            return
        }
        lotesDibujados = true

        lotesOverlay.setPoligonos(poligonos)
        // Índice 0: debajo del overlay de mi ubicación, para que la mira roja
        // quede siempre visible encima de los polígonos
        mapView.overlays.add(0, lotesOverlay)

        poligonos.forEach { lp ->
            lotesMinLat = minOf(lotesMinLat, lp.minLat)
            lotesMaxLat = maxOf(lotesMaxLat, lp.maxLat)
            lotesMinLon = minOf(lotesMinLon, lp.minLon)
            lotesMaxLon = maxOf(lotesMaxLon, lp.maxLon)
        }
        mapView.invalidate()
        mostrarLote(null)
    }

    // ── Detección de lote ─────────────────────────────────────────────────────

    private fun procesarPosicion(lat: Double, lon: Double) {
        if (poligonos.isEmpty()) return

        val detectado = LoteLocator.loteEn(poligonos, lat, lon)?.lote?.catLoteId

        if (detectado == loteActualId) {
            loteCandidatoId = null; fixesCandidato = 0
            return
        }

        if (detectado == loteCandidatoId) fixesCandidato++
        else { loteCandidatoId = detectado; fixesCandidato = 1 }

        if (fixesCandidato >= FIXES_PARA_CAMBIO) {
            loteActualId = loteCandidatoId
            loteCandidatoId = null
            fixesCandidato = 0
            mostrarLote(poligonos.firstOrNull { it.lote.catLoteId == loteActualId }?.lote)
            // Las palmas dibujadas son de otro lote: se quitan para no mostrar
            // puntos que ya no corresponden a donde está el operario. Vuelve a
            // presionar el botón para ver las del lote nuevo.
            if (palmasOverlay.hayPalmas()) quitarPalmas()
        }
    }

    private fun mostrarLote(lote: LoteMapa?) {
        lotesOverlay.setLoteActual(lote?.catLoteId)
        mapView.invalidate()

        if (lote != null) {
            tvNombre.text  = "📍 Lote ${lote.nombre}"
            tvInfo.text    = "Siembra: ${if (lote.siembra > 0) lote.siembra.toString() else "—"}" +
                    "   |   Palmas: ${lote.palmas}"
            tvDetalle.text = "Material: ${lote.material.ifBlank { "—" }}"
        } else {
            tvNombre.text  = "Fuera de los lotes"
            tvInfo.text    = "—"
            tvDetalle.text = ""
        }
    }

    // ── Capa de palmas ────────────────────────────────────────────────────────

    /**
     * Carga las palmas del lote donde está parado el operario.
     *
     * Solo las de ESE lote: son ~600 puntos en vez de las 300.000 de la
     * plantación. Ahí está la diferencia entre algo fluido en un celular de
     * gama baja y algo imposible de dibujar.
     *
     * Es manual a propósito: al cambiar de lote el operario vuelve a
     * presionar. Recargar solo al detectar el cambio sería más "listo", pero
     * dispararía trabajo cada vez que se cruza un lindero.
     */
    private fun cargarPalmasDelLoteActual() {
        // Segundo toque con palmas ya puestas: se apaga la capa.
        if (palmasOverlay.hayPalmas()) {
            quitarPalmas()
            android.widget.Toast.makeText(this, "Palmas ocultas", android.widget.Toast.LENGTH_SHORT).show()
            return
        }

        val loteId = loteActualId
        if (loteId == null) {
            android.widget.Toast.makeText(this,
                "Está fuera de los lotes: no hay palmas que mostrar",
                android.widget.Toast.LENGTH_SHORT).show()
            return
        }

        android.widget.Toast.makeText(this, "Cargando palmas...", android.widget.Toast.LENGTH_SHORT).show()
        Thread {
            val lista = try {
                DatabaseHelper.getInstance(this).getPalmasPorLote(loteId)
            } catch (e: Exception) { emptyList<PalmaMapa>() }

            runOnUiThread {
                if (lista.isEmpty()) {
                    android.widget.Toast.makeText(this,
                        "Sin palmas para este lote. Sincronice para descargarlas.",
                        android.widget.Toast.LENGTH_LONG).show()
                    return@runOnUiThread
                }
                palmasOverlay.setPalmas(lista)
                // Índice 1: encima de los lotes (que van en 0) y debajo del
                // overlay de mi ubicación, para no tapar la mira roja.
                if (!mapView.overlays.contains(palmasOverlay)) {
                    mapView.overlays.add(1, palmasOverlay)
                }
                mostrarPalma(null)
                mapView.invalidate()
                android.widget.Toast.makeText(this,
                    "${lista.size} palmas · toque una para ver su línea",
                    android.widget.Toast.LENGTH_SHORT).show()
            }
        }.start()
    }

    private fun quitarPalmas() {
        palmasOverlay.limpiar()
        mapView.overlays.remove(palmasOverlay)
        mostrarPalma(null)
        mapView.invalidate()
    }

    /** Muestra línea y palma de la seleccionada, o esconde la línea si no hay. */
    private fun mostrarPalma(palma: PalmaMapa?) {
        if (palma != null) {
            tvPalmaInfo.text = "Palma: ${palma.palma}\nLínea: ${palma.linea}"
            tvPalmaInfo.visibility = android.view.View.VISIBLE
        } else {
            tvPalmaInfo.text = ""
            tvPalmaInfo.visibility = android.view.View.GONE
        }
    }

    // ── Descarga del mapa base para uso offline ───────────────────────────────

    private fun confirmarDescargaMapaOffline() {
        if (poligonos.isEmpty()) {
            android.widget.Toast.makeText(this, "Espere a que carguen los lotes (o sincronice primero).", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val bb = BoundingBox(lotesMaxLat, lotesMaxLon, lotesMinLat, lotesMinLon).increaseByScale(1.2f)
            val cacheManager = CacheManager(mapView)
            val zoomMin = 13; val zoomMax = 17
            val totalTeselas = cacheManager.possibleTilesInArea(bb, zoomMin, zoomMax)

            AlertDialog.Builder(this)
                .setTitle("Descargar mapa offline")
                .setMessage(
                    "Se descargará la imagen satelital de toda la zona de los lotes " +
                            "(~$totalTeselas imágenes, zoom $zoomMin–$zoomMax).\n\n" +
                            "Hágalo con WiFi. Después el mapa se verá en campo sin señal."
                )
                .setPositiveButton("Descargar") { _, _ ->
                    iniciarDescargaMapa(cacheManager, bb, zoomMin, zoomMax)
                }
                .setNegativeButton("Cancelar", null)
                .show()
        } catch (e: Exception) {
            android.widget.Toast.makeText(this, "No se pudo preparar la descarga: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
        }
    }

    private fun iniciarDescargaMapa(cacheManager: CacheManager, bb: BoundingBox, zoomMin: Int, zoomMax: Int) {
        val tvProgreso = TextView(this).apply {
            text = "Iniciando descarga..."
            setPadding(60, 40, 60, 20)
            textSize = 15f
        }
        val dialogo = AlertDialog.Builder(this)
            .setTitle("Descargando mapa satelital")
            .setView(tvProgreso)
            .setCancelable(false)
            .setNegativeButton("Ocultar", null)   // la descarga sigue en segundo plano
            .create()
        dialogo.show()

        try {
            cacheManager.downloadAreaAsyncNoUI(this, bb, zoomMin, zoomMax,
                object : CacheManager.CacheManagerCallback {
                    override fun onTaskComplete() {
                        runOnUiThread {
                            if (dialogo.isShowing) dialogo.dismiss()
                            android.widget.Toast.makeText(this@MapaLotesActivity,
                                "✅ Mapa offline descargado", android.widget.Toast.LENGTH_LONG).show()
                        }
                    }

                    override fun onTaskFailed(errors: Int) {
                        runOnUiThread {
                            if (dialogo.isShowing) dialogo.dismiss()
                            android.widget.Toast.makeText(this@MapaLotesActivity,
                                "Descarga terminada con $errors imágenes fallidas. " +
                                        "Puede repetirla: solo bajará las que faltan.",
                                android.widget.Toast.LENGTH_LONG).show()
                        }
                    }

                    override fun updateProgress(progress: Int, currentZoomLevel: Int, zoomMin: Int, zoomMax: Int) {
                        runOnUiThread {
                            tvProgreso.text = "Imágenes descargadas: $progress\nNivel de zoom: $currentZoomLevel de $zoomMax"
                        }
                    }

                    override fun downloadStarted() { }

                    override fun setPossibleTilesInArea(total: Int) {
                        runOnUiThread { tvProgreso.text = "Total a descargar: $total imágenes" }
                    }
                })
        } catch (e: Exception) {
            if (dialogo.isShowing) dialogo.dismiss()
            android.widget.Toast.makeText(this, "Error en la descarga: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
        }
    }

    // ── Centrado y posición ───────────────────────────────────────────────────

    @SuppressLint("MissingPermission")
    private fun centrarComoOrux() {
        fusedClient.lastLocation
            .addOnSuccessListener { loc ->
                if (loc != null) {
                    mapView.controller.setZoom(17.0)
                    mapView.controller.setCenter(GeoPoint(loc.latitude, loc.longitude))
                } else {
                    centrarEnLotes()
                }
            }
            .addOnFailureListener { centrarEnLotes() }
    }

    private fun centrarEnLotes() {
        if (poligonos.isEmpty()) return
        mapView.post {
            mapView.zoomToBoundingBox(
                BoundingBox(lotesMaxLat, lotesMaxLon, lotesMinLat, lotesMinLon).increaseByScale(1.3f), false
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun configurarMiUbicacion() {
        val icono = crearIconoPosicion()
        val overlay = MyLocationNewOverlay(GpsMyLocationProvider(this), mapView)
        overlay.setPersonIcon(icono)
        overlay.setDirectionIcon(icono)
        overlay.setPersonHotspot(icono.width / 2f, icono.height / 2f)
        overlay.enableMyLocation()
        overlay.enableFollowLocation()
        mapView.overlays.add(overlay)
    }

    private fun crearIconoPosicion(): Bitmap {
        val size = 72
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val cx = size / 2f
        val rojo = Color.rgb(211, 47, 47)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = rojo; style = Paint.Style.STROKE; strokeWidth = 6f
        }
        canvas.drawCircle(cx, cx, 18f, paint)
        canvas.drawLine(cx, 2f,        cx, cx - 8f, paint)
        canvas.drawLine(cx, size - 2f, cx, cx + 8f, paint)
        canvas.drawLine(2f, cx,        cx - 8f, cx, paint)
        canvas.drawLine(size - 2f, cx, cx + 8f, cx, paint)
        paint.style = Paint.Style.FILL
        canvas.drawCircle(cx, cx, 5f, paint)
        return bmp
    }

    // ── Ciclo de vida ─────────────────────────────────────────────────────────

    @SuppressLint("MissingPermission")
    private fun iniciarUbicacion() {
        if (pidiendoUbicacion) return

        // Fused: respaldo, a la mitad de cadencia. Sus fixes se descartan
        // mientras el GPS reporte, así que pedirlos al mismo ritmo solo
        // gastaría batería.
        try {
            val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, INTERVALO_MS * 2)
                .setMinUpdateIntervalMillis(INTERVALO_MS * 2)
                .build()
            fusedClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
        } catch (e: Exception) {
            // Sin permisos: el mapa y los lotes se ven igual, solo que sin
            // detección del lote actual.
            android.util.Log.w(TAG, "Sin Fused: ${e.message}")
        }

        // GPS crudo: la fuente preferida.
        try {
            val lm = locationManager
            if (lm != null && lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                lm.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    INTERVALO_MS,
                    0f,
                    gpsListener,
                    Looper.getMainLooper()
                )
            } else {
                android.util.Log.w(TAG, "GPS apagado o no disponible: se usará solo Fused")
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "No se pudo suscribir el GPS crudo: ${e.message}")
        }

        pidiendoUbicacion = true
    }

    private fun detenerUbicacion() {
        if (!pidiendoUbicacion) return
        try { fusedClient.removeLocationUpdates(locationCallback) } catch (e: Exception) { }
        try { locationManager?.removeUpdates(gpsListener) } catch (e: Exception) { }
        // Se reinicia para que al volver a entrar no se dé por bueno un fix
        // de GPS de la sesión anterior.
        ultimoFixGpsMs = 0L
        pidiendoUbicacion = false
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
        iniciarUbicacion()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
        // Módulo informativo: al salir de pantalla no tiene sentido seguir
        // consultando la posición cada segundo. Los tracks los sigue grabando
        // el TrackingService por su cuenta.
        detenerUbicacion()
    }

    override fun onDestroy() {
        super.onDestroy()
        detenerUbicacion()
    }
}