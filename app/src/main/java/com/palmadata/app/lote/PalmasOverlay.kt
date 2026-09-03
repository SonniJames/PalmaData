package com.palmadata.app.mapa

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Point
import android.view.MotionEvent
import com.palmadata.app.data.model.PalmaMapa
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay
import kotlin.math.hypot

/**
 * Dibuja las palmas del lote actual como puntos y permite seleccionar una.
 *
 * Un punto es mucho más barato que un polígono: dibujar las ~600 palmas de un
 * lote cuesta menos que los polígonos de lotes que ya se pintan. Por eso el
 * enfoque de "solo el lote actual" hace viable algo que con las 300.000 de la
 * plantación sería imposible.
 *
 * Sin etiquetas a propósito: 600 textos superpuestos serían ilegibles y
 * costosos. El detalle se consulta tocando la palma.
 */
class PalmasOverlay : Overlay() {

    companion object {
        /** Por debajo de este zoom las palmas se amontonan y no aportan nada. */
        private const val ZOOM_MIN_DIBUJO = 15.0

        /**
         * Zoom mínimo para poder seleccionar. Las palmas están a ~9 m: en zoom
         * 17 eso son unos 8 píxeles y el toque sería una lotería entre vecinas.
         * En zoom 18 son ~15 px, ya distinguibles.
         */
        const val ZOOM_MIN_SELECCION = 18.0

        /** Radio del toque, en píxeles. */
        private const val RADIO_TOQUE_PX = 44f
    }

    @Volatile private var palmas: List<PalmaMapa> = emptyList()
    @Volatile private var seleccionadaId: Long? = null

    /** Avisa a la pantalla qué palma quedó seleccionada (null = ninguna). */
    var onSeleccion: ((PalmaMapa?) -> Unit)? = null

    /** Se toca una palma con el mapa demasiado alejado. */
    var onZoomInsuficiente: (() -> Unit)? = null

    // ── Pinturas y objetos reutilizados: cero asignaciones por cuadro ─────────
    private val pinturaNormal = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.rgb(33, 150, 243)      // azul
    }
    private val pinturaSeleccion = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.rgb(211, 47, 47)       // rojo
    }
    private val borde = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        color = Color.WHITE                  // contorno para que se vean sobre el satélite
    }
    private val punto = Point()
    private val geo   = GeoPoint(0.0, 0.0)

    fun setPalmas(lista: List<PalmaMapa>) {
        palmas = lista
        seleccionadaId = null
    }

    fun limpiar() {
        palmas = emptyList()
        seleccionadaId = null
    }

    fun hayPalmas(): Boolean = palmas.isNotEmpty()

    override fun draw(canvas: Canvas, mapView: MapView, shadow: Boolean) {
        if (shadow) return
        val lista = palmas
        if (lista.isEmpty()) return
        if (mapView.zoomLevelDouble < ZOOM_MIN_DIBUJO) return

        val proj  = mapView.projection
        val ancho = mapView.width
        val alto  = mapView.height
        // El radio crece con el zoom para que la palma sea tocable de cerca
        // sin convertirse en una mancha cuando se mira el lote completo.
        val radio = if (mapView.zoomLevelDouble >= 18.0) 9f else 6f

        lista.forEach { p ->
            geo.setCoords(p.lat, p.lon)
            proj.toPixels(geo, punto)
            // Culling: si cae fuera de pantalla no se dibuja
            if (punto.x < -20 || punto.x > ancho + 20 || punto.y < -20 || punto.y > alto + 20) {
                return@forEach
            }
            val relleno = if (p.catPalmaId == seleccionadaId) pinturaSeleccion else pinturaNormal
            val r = if (p.catPalmaId == seleccionadaId) radio + 3f else radio
            canvas.drawCircle(punto.x.toFloat(), punto.y.toFloat(), r, relleno)
            canvas.drawCircle(punto.x.toFloat(), punto.y.toFloat(), r, borde)
        }
    }

    /**
     * Selecciona la palma más cercana al toque. Si se toca la que ya estaba
     * seleccionada, se deselecciona. Solo una a la vez.
     *
     * Devuelve true solo cuando hubo acierto: si no, se devuelve false para que
     * el mapa procese el toque como desplazamiento normal.
     */
    override fun onSingleTapConfirmed(e: MotionEvent, mapView: MapView): Boolean {
        val lista = palmas
        if (lista.isEmpty()) return false

        if (mapView.zoomLevelDouble < ZOOM_MIN_SELECCION) {
            onZoomInsuficiente?.invoke()
            return false
        }

        val proj = mapView.projection
        val tx = e.x
        val ty = e.y

        var mejor: PalmaMapa? = null
        var mejorDist = Float.MAX_VALUE

        lista.forEach { p ->
            geo.setCoords(p.lat, p.lon)
            proj.toPixels(geo, punto)
            val d = hypot(punto.x - tx, punto.y - ty)
            if (d < mejorDist) { mejorDist = d; mejor = p }
        }

        if (mejor == null || mejorDist > RADIO_TOQUE_PX) return false

        val elegida = mejor!!
        seleccionadaId = if (seleccionadaId == elegida.catPalmaId) {
            onSeleccion?.invoke(null)          // segundo toque: deseleccionar
            null
        } else {
            onSeleccion?.invoke(elegida)
            elegida.catPalmaId
        }
        mapView.invalidate()
        return true
    }
}