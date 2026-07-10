package com.palmadata.app.mapa

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.media.RingtoneManager
import android.os.Bundle
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.palmadata.app.R
import com.palmadata.app.utils.DatabaseHelper
import com.palmadata.app.utils.SessionManager
import com.palmadata.app.utils.UmaLocator
import com.palmadata.app.utils.UmaPoligono
import org.json.JSONArray
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

class MapaUmasActivity : AppCompatActivity() {

    private lateinit var mapView: MapView
    private lateinit var tvNombre: TextView
    private lateinit var tvInfo: TextView
    private lateinit var tvDosis: TextView
    private lateinit var btnCentrar: FloatingActionButton
    private lateinit var tvFertilizanteSelector: TextView
    private lateinit var tvLimpiarFertilizante: TextView
    private lateinit var fusedClientDeteccion: com.google.android.gms.location.FusedLocationProviderClient

    private var primerFixRecibido = false
    private val poligonos = mutableListOf<UmaPoligono>()
    private val overlayMap = mutableMapOf<Int, MutableList<Polygon>>()

    // Bounding box de todas las umas
    private var umasMinLat =  90.0; private var umasMaxLat = -90.0
    private var umasMinLon = 180.0; private var umasMaxLon = -180.0

    // Histéresis
    private var umaActualId: Int? = null
    private var umaCandidataId: Int? = null
    private var fixesCandidata = 0
    private val FIXES_PARA_CAMBIO = 3

    // Fertilizantes seleccionados: lista de ids activos
    // Vacía = sin filtro (muestra aviso rojo)
    private val fertilizantesSeleccionados = mutableSetOf<Int>()

    // Uma actual para refrescar caja cuando cambia el filtro
    private var umaActual: com.palmadata.app.data.model.UmaData? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Configuration.getInstance().apply {
            userAgentValue = packageName
            osmdroidBasePath = filesDir
            osmdroidTileCache = cacheDir
        }

        setContentView(R.layout.activity_mapa_umas)

        mapView                = findViewById(R.id.mapView)
        tvNombre               = findViewById(R.id.tvUmaNombre)
        tvInfo                 = findViewById(R.id.tvUmaInfo)
        tvDosis                = findViewById(R.id.tvUmaDosis)
        btnCentrar             = findViewById(R.id.btnCentrar)
        tvFertilizanteSelector = findViewById(R.id.tvFertilizanteSelector)
        tvLimpiarFertilizante  = findViewById(R.id.tvLimpiarFertilizante)

        // ── Botón centrar ──────────────────────────────────────────────────────
        @SuppressLint("MissingPermission")
        btnCentrar.setOnClickListener {
            val fusedClient = LocationServices.getFusedLocationProviderClient(this)
            fusedClient.lastLocation.addOnSuccessListener { loc ->
                if (loc != null) mapView.controller.animateTo(GeoPoint(loc.latitude, loc.longitude))
            }
        }

        // ── Selector de fertilizante ───────────────────────────────────────────
        tvFertilizanteSelector.setOnClickListener { mostrarDialogoFertilizantes() }

        // ── Limpiar fertilizante ───────────────────────────────────────────────
        tvLimpiarFertilizante.setOnClickListener {
            fertilizantesSeleccionados.clear()
            SessionManager.clearFertilizanteActivo(this)
            actualizarFranjaFertilizante()
            refrescarCajaDosis()
        }

        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)
        mapView.minZoomLevel = 3.0

        cargarUmas()
        configurarMiUbicacion()
        centrarComoOrux()
        iniciarDeteccion()
        actualizarFranjaFertilizante()
    }

    // ── Diálogo selector de fertilizantes ─────────────────────────────────────


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
                // Guardar el primer id seleccionado en SessionManager (para los tracks)
                val primero = fertilizantesSeleccionados.firstOrNull()
                if (primero != null) {
                    val nombre = lista.first { it.first == primero }.second
                    SessionManager.setFertilizanteActivo(this, primero, nombre)
                } else {
                    SessionManager.clearFertilizanteActivo(this)
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
            tvFertilizanteSelector.setTextColor(Color.WHITE)
            tvLimpiarFertilizante.alpha = 1.0f
        }
    }

    // ── Cargar umas ────────────────────────────────────────────────────────────
    private fun cargarUmas() {
        val db = DatabaseHelper.getInstance(this)

        db.getUmas().forEach { uma ->
            try {
                val up = UmaPoligono(uma)
                poligonos.add(up)
                val listaOverlays = mutableListOf<Polygon>()

                up.anillos.forEach { anillo ->
                    val ov = Polygon(mapView).apply {
                        points = anillo.map { (lon, lat) -> GeoPoint(lat, lon) }
                        fillPaint.color    = Color.argb(60, 76, 175, 80)
                        outlinePaint.color = Color.rgb(46, 125, 50)
                        outlinePaint.strokeWidth = 3f
                    }
                    mapView.overlays.add(ov)
                    listaOverlays.add(ov)
                }

                overlayMap[uma.nutUmaPolId] = listaOverlays

                val etiqueta = Marker(mapView).apply {
                    position = GeoPoint(
                        (up.minLat + up.maxLat) / 2.0,
                        (up.minLon + up.maxLon) / 2.0
                    )
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    setTextLabelFontSize(36)
                    setTextLabelForegroundColor(Color.rgb(27, 94, 32))
                    setTextLabelBackgroundColor(Color.argb(170, 255, 255, 255))
                    setTextIcon(uma.codigo)
                    setOnMarkerClickListener { _, _ -> true }
                }
                mapView.overlays.add(etiqueta)

                umasMinLat = minOf(umasMinLat, up.minLat)
                umasMaxLat = maxOf(umasMaxLat, up.maxLat)
                umasMinLon = minOf(umasMinLon, up.minLon)
                umasMaxLon = maxOf(umasMaxLon, up.maxLon)

            } catch (e: Exception) { /* geojson inválido: se omite */ }
        }
        mapView.invalidate()
    }

    // ── Centrar estilo OruxMaps ────────────────────────────────────────────────
    @SuppressLint("MissingPermission")
    private fun centrarComoOrux() {
        val fusedClient = LocationServices.getFusedLocationProviderClient(this)
        fusedClient.lastLocation
            .addOnSuccessListener { loc ->
                if (loc != null) {
                    primerFixRecibido = true
                    mapView.controller.setZoom(17.0)
                    mapView.controller.setCenter(GeoPoint(loc.latitude, loc.longitude))
                } else {
                    centrarEnUmas()
                }
            }
            .addOnFailureListener { centrarEnUmas() }
    }

    private fun centrarEnUmas() {
        if (poligonos.isEmpty()) return
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

    @SuppressLint("MissingPermission")
    private fun iniciarDeteccion() {
        fusedClientDeteccion = LocationServices.getFusedLocationProviderClient(this)
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000L).build()
        fusedClientDeteccion.requestLocationUpdates(request, deteccionCallback, Looper.getMainLooper())
    }


    private val deteccionCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val loc = result.lastLocation ?: return
            if (loc.accuracy > 20f) return
            procesarPosicion(loc.latitude, loc.longitude)
        }
    }

    private fun procesarPosicion(lat: Double, lon: Double) {
        if (!primerFixRecibido) {
            primerFixRecibido = true
            mapView.controller.setZoom(17.0)
            mapView.controller.animateTo(GeoPoint(lat, lon))
        }

        val detectada = UmaLocator.umaEn(poligonos, lat, lon)?.uma?.nutUmaPolId

        if (detectada == umaActualId) {
            umaCandidataId = null; fixesCandidata = 0; return
        }

        if (detectada == umaCandidataId) fixesCandidata++
        else { umaCandidataId = detectada; fixesCandidata = 1 }

        if (fixesCandidata >= FIXES_PARA_CAMBIO) {
            val anterior = umaActualId
            umaActualId    = umaCandidataId
            umaCandidataId = null
            fixesCandidata = 0
            mostrarUma(umaActualId, alertar = anterior != null || umaActualId != null)
        }
    }

    private fun mostrarUma(umaId: Int?, alertar: Boolean) {
        overlayMap.forEach { (id, overlays) ->
            val color = if (id == umaId) Color.argb(110, 255, 193, 7)
            else             Color.argb(60,  76, 175, 80)
            overlays.forEach { it.fillPaint.color = color }
        }
        mapView.invalidate()

        umaActual = poligonos.firstOrNull { it.uma.nutUmaPolId == umaId }?.uma

        if (umaActual != null) {
            tvNombre.text = "📍 UMA ${umaActual!!.codigo}"
            tvInfo.text   = "Símbolo: ${umaActual!!.simbolo}  |  Palmas: ${umaActual!!.palmas}"
        } else {
            tvNombre.text = "Fuera de las umas"
            tvInfo.text   = "—"
        }

        refrescarCajaDosis()
        if (alertar) alertar()
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

    @Suppress("DEPRECATION")
    private fun alertar() {
        val v = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        v.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 400, 200, 400), -1))
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        RingtoneManager.getRingtone(applicationContext, uri)?.play()
    }

    override fun onResume()  { super.onResume();  mapView.onResume()  }
    override fun onPause()   { super.onPause();   mapView.onPause()   }
    override fun onDestroy() {
        super.onDestroy()
        // Detener el GPS propio del módulo (el TrackingService sigue intacto)
        if (::fusedClientDeteccion.isInitialized) {
            fusedClientDeteccion.removeLocationUpdates(deteccionCallback)
        }
        // El fertilizante solo aplica dentro de este módulo:
        // al salir, los tracks de los demás módulos vuelven a fertilizante = 0
        fertilizantesSeleccionados.clear()
        SessionManager.clearFertilizanteActivo(this)
    }
}