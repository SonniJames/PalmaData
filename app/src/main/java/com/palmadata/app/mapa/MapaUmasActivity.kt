package com.palmadata.app.mapa

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.view.WindowManager
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.location.LocationServices
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.palmadata.app.R
import com.palmadata.app.data.model.UmaData
import com.palmadata.app.utils.DatabaseHelper
import com.palmadata.app.utils.SessionManager
import com.palmadata.app.utils.UmaDetectionEngine
import org.json.JSONArray
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
 * Pantalla del módulo de fertilización.
 *
 * IMPORTANTE: esta pantalla ya NO detecta las umas por su cuenta. La detección
 * (posición, cambio de uma, alerta, notificación con dosis) corre en el
 * UmaDetectionEngine alimentado por el TrackingService, que es un servicio en
 * primer plano con wakelock — por eso TODO sigue funcionando con la pantalla
 * bloqueada. Esta pantalla solo se registra como oyente del motor y pinta.
 */
class MapaUmasActivity : AppCompatActivity(), UmaDetectionEngine.Listener {

    private lateinit var mapView: MapView
    private lateinit var tvNombre: TextView
    private lateinit var tvInfo: TextView
    private lateinit var tvDosis: TextView
    private lateinit var btnCentrar: FloatingActionButton
    private lateinit var btnDescargarMapa: FloatingActionButton
    private lateinit var tvFertilizanteSelector: TextView
    private lateinit var tvLimpiarFertilizante: TextView

    // Overlay único que dibuja todas las umas (optimización de rendimiento)
    private val umasOverlay = UmasOverlay()
    private var umasDibujadas = false

    // Bounding box de todas las umas (para centrar y para la descarga offline)
    private var umasMinLat =  90.0; private var umasMaxLat = -90.0
    private var umasMinLon = 180.0; private var umasMaxLon = -180.0

    // Fertilizantes seleccionados: lista de ids activos
    // Vacía = sin filtro (muestra aviso rojo)
    private val fertilizantesSeleccionados = mutableSetOf<Int>()

    // Uma actual para refrescar caja cuando cambia el filtro
    private var umaActual: UmaData? = null

    companion object {
        /**
         * Mapa base SATELITAL (Esri World Imagery).
         *
         * Se usa en lugar de MAPNIK por dos razones:
         * 1. MAPNIK (servidores gratuitos de OpenStreetMap) prohíbe la descarga
         *    masiva de teselas — osmdroid lanza TileSourcePolicyException al
         *    intentar bajar el mapa offline (ese era el cierre de la app).
         *    Esri World Imagery sí permite descargar la zona para uso offline.
         * 2. Para la plantación, la imagen satelital muestra las hileras de
         *    palma reales; el mapa de calles solo mostraba un parche verde.
         */
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

        // Mantener la pantalla encendida MIENTRAS esta pantalla esté visible.
        // (Si el trabajador la bloquea con el botón, la detección sigue viva
        // en el servicio: alerta, vibración y notificación con la dosis.)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        Configuration.getInstance().apply {
            userAgentValue = packageName
            osmdroidBasePath = filesDir
            osmdroidTileCache = cacheDir
            // Las teselas descargadas no expiran en 1 año → el mapa funciona offline
            expirationOverrideDuration = 365L * 24 * 60 * 60 * 1000
            // Caché grande para que quepa toda la plantación descargada
            tileFileSystemCacheMaxBytes  = 1024L * 1024 * 1024   // 1 GB
            tileFileSystemCacheTrimBytes = 900L  * 1024 * 1024
        }

        setContentView(R.layout.activity_mapa_umas)

        mapView                = findViewById(R.id.mapView)
        tvNombre               = findViewById(R.id.tvUmaNombre)
        tvInfo                 = findViewById(R.id.tvUmaInfo)
        tvDosis                = findViewById(R.id.tvUmaDosis)
        btnCentrar             = findViewById(R.id.btnCentrar)
        btnDescargarMapa       = findViewById(R.id.btnDescargarMapa)
        tvFertilizanteSelector = findViewById(R.id.tvFertilizanteSelector)
        tvLimpiarFertilizante  = findViewById(R.id.tvLimpiarFertilizante)

        // Restaurar la selección de fertilizantes si la pantalla fue recreada
        // (p. ej. el sistema la mató con la app de fondo y el usuario volvió)
        restaurarFertilizantesDeSesion()

        // ── Botón centrar ──────────────────────────────────────────────────────
        @SuppressLint("MissingPermission")
        btnCentrar.setOnClickListener {
            val fusedClient = LocationServices.getFusedLocationProviderClient(this)
            fusedClient.lastLocation.addOnSuccessListener { loc ->
                if (loc != null) mapView.controller.animateTo(GeoPoint(loc.latitude, loc.longitude))
            }
        }

        // ── Botón descargar mapa offline ───────────────────────────────────────
        btnDescargarMapa.setOnClickListener { confirmarDescargaMapaOffline() }

        // ── Selector de fertilizante ───────────────────────────────────────────
        tvFertilizanteSelector.setOnClickListener { mostrarDialogoFertilizantes() }

        // ── Limpiar fertilizante ───────────────────────────────────────────────
        tvLimpiarFertilizante.setOnClickListener {
            fertilizantesSeleccionados.clear()
            SessionManager.clearFertilizantesActivos(this)
            actualizarFranjaFertilizante()
            refrescarCajaDosis()
        }

        mapView.setTileSource(FUENTE_SATELITAL)
        mapView.setMultiTouchControls(true)
        mapView.minZoomLevel = 3.0

        configurarMiUbicacion()
        actualizarFranjaFertilizante()

        // ── Motor de detección ────────────────────────────────────────────────
        // Se registra como oyente y activa el motor (la carga de polígonos
        // ocurre en segundo plano; si ya estaban en memoria, se pinta de una)
        UmaDetectionEngine.setListener(this)
        UmaDetectionEngine.activar(this)
        if (UmaDetectionEngine.poligonos.isNotEmpty()) {
            dibujarUmas()
            sincronizarConMotor()
            centrarComoOrux()
        } else {
            tvNombre.text = "Cargando umas..."
        }
    }

    // ── Callbacks del motor (pueden llegar en hilo secundario) ─────────────────

    override fun onUmasCargadas() {
        runOnUiThread {
            dibujarUmas()
            sincronizarConMotor()
            centrarComoOrux()
        }
    }

    override fun onUmaCambiada(uma: UmaData?) {
        // La alerta (vibración + sonido) ya la hizo el motor; aquí solo se pinta
        runOnUiThread { mostrarUma(uma) }
    }

    /** Pone la UI en el estado actual del motor (al abrir o volver a la pantalla) */
    private fun sincronizarConMotor() {
        mostrarUma(UmaDetectionEngine.umaActual())
    }

    // ── Diálogo selector de fertilizantes ─────────────────────────────────────

    private fun restaurarFertilizantesDeSesion() {
        try {
            val arr = JSONArray(SessionManager.getFertilizantesActivos(this))
            for (i in 0 until arr.length()) fertilizantesSeleccionados.add(arr.getInt(i))
        } catch (e: Exception) { /* sin selección previa */ }
    }

    private fun mostrarDialogoFertilizantes() {
        val db = DatabaseHelper.getInstance(this)
        val lista = db.getFertilizantes()  // List<Pair<Int, String>>

        if (lista.isEmpty()) {
            android.widget.Toast.makeText(this, "Sin fertilizantes disponibles. Sincroniza primero.", android.widget.Toast.LENGTH_SHORT).show()
            return
        }

        val nombres  = lista.map { it.second }.toTypedArray()
        val checked  = BooleanArray(lista.size) { fertilizantesSeleccionados.contains(lista[it].first) }

        AlertDialog.Builder(this)
            .setTitle("Seleccione fertilizante(s)")
            .setMultiChoiceItems(nombres, checked) { _, which, isChecked ->
                val id = lista[which].first
                if (isChecked) fertilizantesSeleccionados.add(id)
                else fertilizantesSeleccionados.remove(id)
            }
            .setPositiveButton("Aceptar") { _, _ ->
                // Guardar TODOS los ids seleccionados (los tracks llevan "[1,2,...]")
                if (fertilizantesSeleccionados.isNotEmpty()) {
                    SessionManager.setFertilizantesActivos(this, fertilizantesSeleccionados)
                } else {
                    SessionManager.clearFertilizantesActivos(this)
                }
                actualizarFranjaFertilizante()
                refrescarCajaDosis()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun actualizarFranjaFertilizante() {
        if (fertilizantesSeleccionados.isEmpty()) {
            tvFertilizanteSelector.text = "Seleccione fertilizante"
            tvFertilizanteSelector.setTextColor(resources.getColor(R.color.worker_not_set, theme))
            tvLimpiarFertilizante.alpha = 0.4f
        } else {
            val db = DatabaseHelper.getInstance(this)
            val lista = db.getFertilizantes()
            val nombres = fertilizantesSeleccionados
                .mapNotNull { id -> lista.firstOrNull { it.first == id }?.second }
                .joinToString(", ")
            tvFertilizanteSelector.text = nombres
            // Verde oscuro: el blanco no se veía sobre la franja clara
            tvFertilizanteSelector.setTextColor(Color.parseColor("#1B5E20"))
            tvLimpiarFertilizante.alpha = 1.0f
        }
    }

    // ── Dibujar umas (un solo overlay: los polígonos vienen del motor) ─────────

    private fun dibujarUmas() {
        if (umasDibujadas) return
        umasDibujadas = true

        val poligonos = UmaDetectionEngine.poligonos
        umasOverlay.setPoligonos(poligonos)
        // Índice 0: debajo del overlay de mi ubicación, para que la mira roja
        // siempre quede visible encima de los polígonos
        mapView.overlays.add(0, umasOverlay)

        poligonos.forEach { up ->
            umasMinLat = minOf(umasMinLat, up.minLat)
            umasMaxLat = maxOf(umasMaxLat, up.maxLat)
            umasMinLon = minOf(umasMinLon, up.minLon)
            umasMaxLon = maxOf(umasMaxLon, up.maxLon)
        }
        mapView.invalidate()
        if (tvNombre.text == "Cargando umas...") tvNombre.text = "Fuera de las umas"
    }

    // ── Descarga del mapa base para uso offline ────────────────────────────────
    private fun confirmarDescargaMapaOffline() {
        if (UmaDetectionEngine.poligonos.isEmpty()) {
            android.widget.Toast.makeText(this, "Espere a que carguen las umas (o sincronice primero).", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val bb = BoundingBox(umasMaxLat, umasMaxLon, umasMinLat, umasMinLon).increaseByScale(1.2f)
            val cacheManager = CacheManager(mapView)
            val zoomMin = 13; val zoomMax = 17
            val totalTeselas = cacheManager.possibleTilesInArea(bb, zoomMin, zoomMax)

            AlertDialog.Builder(this)
                .setTitle("Descargar mapa offline")
                .setMessage(
                    "Se descargará la imagen satelital de toda la zona de las umas " +
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
        // Diálogo de progreso propio (el interno de osmdroid causaba cierres)
        val tvProgreso = TextView(this).apply {
            text = "Iniciando descarga..."
            setPadding(60, 40, 60, 20)
            textSize = 15f
        }
        val dialogo = AlertDialog.Builder(this)
            .setTitle("Descargando mapa satelital")
            .setView(tvProgreso)
            .setCancelable(false)
            .setNegativeButton("Ocultar", null)  // la descarga sigue en segundo plano
            .create()
        dialogo.show()

        try {
            cacheManager.downloadAreaAsyncNoUI(this, bb, zoomMin, zoomMax,
                object : CacheManager.CacheManagerCallback {
                    override fun onTaskComplete() {
                        runOnUiThread {
                            if (dialogo.isShowing) dialogo.dismiss()
                            android.widget.Toast.makeText(this@MapaUmasActivity,
                                "✅ Mapa offline descargado", android.widget.Toast.LENGTH_LONG).show()
                        }
                    }

                    override fun onTaskFailed(errors: Int) {
                        runOnUiThread {
                            if (dialogo.isShowing) dialogo.dismiss()
                            android.widget.Toast.makeText(this@MapaUmasActivity,
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

    // ── Centrar estilo OruxMaps ────────────────────────────────────────────────
    @SuppressLint("MissingPermission")
    private fun centrarComoOrux() {
        val fusedClient = LocationServices.getFusedLocationProviderClient(this)
        fusedClient.lastLocation
            .addOnSuccessListener { loc ->
                if (loc != null) {
                    mapView.controller.setZoom(17.0)
                    mapView.controller.setCenter(GeoPoint(loc.latitude, loc.longitude))
                } else {
                    centrarEnUmas()
                }
            }
            .addOnFailureListener { centrarEnUmas() }
    }

    private fun centrarEnUmas() {
        if (UmaDetectionEngine.poligonos.isEmpty()) return
        mapView.post {
            mapView.zoomToBoundingBox(
                BoundingBox(umasMaxLat, umasMaxLon, umasMinLat, umasMinLon).increaseByScale(1.3f), false
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

    // ── Pintar la uma actual (sin alertar: la alerta la hace el motor) ─────────

    private fun mostrarUma(uma: UmaData?) {
        // El overlay unificado se encarga del resaltado amarillo
        umasOverlay.setUmaActual(uma?.nutUmaPolId)
        mapView.invalidate()

        umaActual = uma

        if (uma != null) {
            tvNombre.text = "📍 UMA ${uma.codigo}"
            tvInfo.text   = "Símbolo: ${uma.simbolo}  |  Palmas: ${uma.palmas}"
        } else {
            tvNombre.text = "Fuera de las umas"
            tvInfo.text   = "—"
        }

        refrescarCajaDosis()
    }

    // ── Caja de dosis con filtro ───────────────────────────────────────────────
    private fun refrescarCajaDosis() {
        val uma = umaActual

        if (uma == null) {
            tvDosis.text = ""
            tvDosis.setTextColor(Color.parseColor("#33691E"))
            return
        }

        // Sin fertilizante seleccionado → aviso rojo
        if (fertilizantesSeleccionados.isEmpty()) {
            tvDosis.text = "⚠ Seleccione fertilizante"
            tvDosis.setTextColor(Color.RED)
            tvDosis.textSize = 16f
            return
        }

        // Parsear el JSON de fertilizantes de la uma
        try {
            val jsonArray = JSONArray(uma.fertilizantes)
            val sb = StringBuilder()

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val id  = obj.getInt("id")

                // Solo mostrar los fertilizantes seleccionados
                if (!fertilizantesSeleccionados.contains(id)) continue

                if (sb.isNotEmpty()) sb.append("\n\n")
                sb.append("Fertilizante: ${obj.getString("nombre")}\n")
                sb.append("Rondas: ${obj.getInt("rondas")}\n")
                sb.append("Dosis: ${obj.getDouble("dosis")}")
            }

            if (sb.isEmpty()) {
                // Seleccionó fertilizantes pero ninguno aplica en esta uma
                tvDosis.text = "Este fertilizante no aplica en esta UMA"
                tvDosis.setTextColor(Color.parseColor("#E65100"))
            } else {
                tvDosis.text = sb.toString()
                tvDosis.setTextColor(Color.parseColor("#33691E"))
            }
            tvDosis.textSize = 15f

        } catch (e: Exception) {
            tvDosis.text = "Sin información de fertilización"
            tvDosis.setTextColor(Color.parseColor("#33691E"))
        }
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
        // Reafirmar el módulo activo: si el operario tocó la notificación,
        // MainActivity se abrió encima y su onResume limpió el formulario a 0
        // (apagando el motor). Al volver aquí con atrás, se restaura el 25 y
        // la detección revive en el siguiente fix del servicio.
        SessionManager.setFormularioActivo(this, 25)
        // Al volver (p. ej. tras desbloquear), ponerse al día con lo que el
        // motor detectó mientras la pantalla estaba apagada
        UmaDetectionEngine.setListener(this)
        if (umasDibujadas) sincronizarConMotor()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        UmaDetectionEngine.setListener(null)
        // El fertilizante solo aplica dentro de este módulo:
        // al salir, los tracks de los demás módulos vuelven a fertilizante = []
        // (el motor se desactiva solo cuando el servicio ve formulario ≠ 25)
        fertilizantesSeleccionados.clear()
        SessionManager.clearFertilizantesActivos(this)
    }
}