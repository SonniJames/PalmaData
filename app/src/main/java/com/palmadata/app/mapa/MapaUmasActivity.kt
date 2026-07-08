package com.palmadata.app.mapa

import android.annotation.SuppressLint
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
import com.palmadata.app.R
import com.palmadata.app.utils.DatabaseHelper
import com.palmadata.app.utils.UmaLocator
import com.palmadata.app.utils.UmaPoligono
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

    private var primerFixRecibido = false
    private val poligonos = mutableListOf<UmaPoligono>()
    private val overlayMap = mutableMapOf<Int, MutableList<Polygon>>()

    // Bounding box de todas las umas (para fallback de centrado)
    private var umasMinLat =  90.0; private var umasMaxLat = -90.0
    private var umasMinLon = 180.0; private var umasMaxLon = -180.0

    // Histéresis: solo cambia de uma tras N fixes consecutivos en la nueva
    private var umaActualId: Int? = null
    private var umaCandidataId: Int? = null
    private var fixesCandidata = 0
    private val FIXES_PARA_CAMBIO = 3

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Configuration.getInstance().apply {
            userAgentValue = packageName
            osmdroidBasePath = filesDir
            osmdroidTileCache = cacheDir
        }

        setContentView(R.layout.activity_mapa_umas)

        mapView  = findViewById(R.id.mapView)
        tvNombre = findViewById(R.id.tvUmaNombre)
        tvInfo   = findViewById(R.id.tvUmaInfo)
        tvDosis  = findViewById(R.id.tvUmaDosis)

        // Mismo mapa que OruxMaps: OpenStreetMap Mapnik
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)
        mapView.minZoomLevel = 3.0

        cargarUmas()
        configurarMiUbicacion()
        centrarComoOrux()
        iniciarDeteccion()
    }

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

                // Etiqueta con el código de la uma en el centro del polígono
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
                    setOnMarkerClickListener { _, _ -> true }  // sin popup al tocar
                }
                mapView.overlays.add(etiqueta)

                umasMinLat = minOf(umasMinLat, up.minLat)
                umasMaxLat = maxOf(umasMaxLat, up.maxLat)
                umasMinLon = minOf(umasMinLon, up.minLon)
                umasMaxLon = maxOf(umasMaxLon, up.maxLon)

            } catch (e: Exception) {
                // geojson inválido: se omite esa uma sin crashear
            }
        }
        mapView.invalidate()
    }

    /**
     * Comportamiento OruxMaps: centrar INMEDIATAMENTE al abrir,
     * usando la última posición conocida del sistema (instantánea).
     */
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
                BoundingBox(umasMaxLat, umasMaxLon, umasMinLat, umasMinLon)
                    .increaseByScale(1.3f),
                false
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

    /**
     * Mira roja estilo OruxMaps: círculo con cruz y punto central.
     */
    private fun crearIconoPosicion(): Bitmap {
        val size = 72
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val cx = size / 2f
        val rojo = Color.rgb(211, 47, 47)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = rojo
            style = Paint.Style.STROKE
            strokeWidth = 6f
        }

        // Círculo central
        canvas.drawCircle(cx, cx, 18f, paint)

        // Cruz: 4 líneas que atraviesan el círculo (estilo mira Orux)
        canvas.drawLine(cx, 2f,        cx, cx - 8f,   paint)  // arriba
        canvas.drawLine(cx, size - 2f, cx, cx + 8f,   paint)  // abajo
        canvas.drawLine(2f, cx,        cx - 8f, cx,   paint)  // izquierda
        canvas.drawLine(size - 2f, cx, cx + 8f, cx,   paint)  // derecha

        // Punto central relleno
        paint.style = Paint.Style.FILL
        canvas.drawCircle(cx, cx, 5f, paint)

        return bmp
    }

    @SuppressLint("MissingPermission")
    private fun iniciarDeteccion() {
        val fusedClient = LocationServices.getFusedLocationProviderClient(this)
        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, 3000L
        ).build()
        fusedClient.requestLocationUpdates(request, deteccionCallback, Looper.getMainLooper())
    }

    private val deteccionCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val loc = result.lastLocation ?: return
            if (loc.accuracy > 15f) return  // descarta fixes con más de 20m de error
            procesarPosicion(loc.latitude, loc.longitude)
        }
    }

    private fun procesarPosicion(lat: Double, lon: Double) {
        // Si al abrir no había última posición conocida, centrar en el primer fix real
        if (!primerFixRecibido) {
            primerFixRecibido = true
            mapView.controller.setZoom(17.0)
            mapView.controller.animateTo(GeoPoint(lat, lon))
        }

        val detectada = UmaLocator.umaEn(poligonos, lat, lon)?.uma?.nutUmaPolId

        // Si sigue en la misma uma, resetea el contador candidato
        if (detectada == umaActualId) {
            umaCandidataId = null
            fixesCandidata = 0
            return
        }

        // Acumula fixes en la candidata
        if (detectada == umaCandidataId) {
            fixesCandidata++
        } else {
            umaCandidataId = detectada
            fixesCandidata = 1
        }

        // Confirma el cambio solo tras N fixes consecutivos
        if (fixesCandidata >= FIXES_PARA_CAMBIO) {
            val anterior = umaActualId
            umaActualId    = umaCandidataId
            umaCandidataId = null
            fixesCandidata = 0
            mostrarUma(umaActualId, alertar = anterior != null || umaActualId != null)
        }
    }

    private fun mostrarUma(umaId: Int?, alertar: Boolean) {
        // Resaltar polígono activo en ámbar, resto en verde
        overlayMap.forEach { (id, overlays) ->
            val color = if (id == umaId) Color.argb(110, 255, 193, 7)
            else             Color.argb(60,  76, 175, 80)
            overlays.forEach { it.fillPaint.color = color }
        }
        mapView.invalidate()

        val uma = poligonos.firstOrNull { it.uma.nutUmaPolId == umaId }?.uma

        if (uma != null) {
            tvNombre.text = "📍 UMA ${uma.codigo}"
            tvInfo.text   = "Símbolo: ${uma.simbolo}  |  Palmas: ${uma.palmas}"
            tvDosis.text = uma.dosis  // aquí irán las dosis cuando tengas la tabla

        } else {
            tvNombre.text = "Fuera de las umas"
            tvInfo.text   = "—"
            tvDosis.text  = ""
        }

        if (alertar) alertar()
    }

    @Suppress("DEPRECATION")
    private fun alertar() {
        // Vibración
        val v = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        v.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 400, 200, 400), -1))

        // Sonido del sistema
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val ringtone = RingtoneManager.getRingtone(applicationContext, uri)
        ringtone?.play()
    }

    override fun onResume()  { super.onResume();  mapView.onResume()  }
    override fun onPause()   { super.onPause();   mapView.onPause()   }
}