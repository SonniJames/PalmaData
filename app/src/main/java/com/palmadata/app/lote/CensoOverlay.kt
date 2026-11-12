package com.palmadata.app.mapa

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Point
import android.view.MotionEvent
import com.palmadata.app.data.model.PuntoCenso
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay
import kotlin.math.hypot

/**
 * Capa de censo de enfermedades: las palmas censadas en el mes, cada una con
 * el color de su evento (el mismo de la página web).
 *
 * Se dibujan todas las del mes que pasan el filtro, no solo las del lote
 * actual: lo que interesa es ver dónde se concentran. Con unos miles de puntos
 * sigue siendo fluido porque solo se pintan los que caen en pantalla y no
 * hay etiquetas. El detalle se consulta tocando el punto.
 *
 * El orden de dibujo sigue la lista de eventos: los primeros de la lista
 * (los de color fijo, como PC-Emisión Enferma) quedan encima.
 */
class CensoOverlay : Overlay() {

    companion object {
        /** Zoom mínimo para tocar un punto: más lejos, las palmas vecinas se confunden. */
        const val ZOOM_MIN_SELECCION = 17.0

        /** Radio del toque, en píxeles. */
        private const val RADIO_TOQUE_PX = 44f
    }

    /** Ya filtrados y ordenados para dibujar (los de abajo primero). */
    @Volatile private var puntos: List<PuntoCenso> = emptyList()
    @Volatile private var colores: Map<Int, Int> = emptyMap()
    @Volatile private var seleccionadoId: Long? = null

    /** Avisa a la pantalla qué punto quedó seleccionado (null = ninguno). */
    var onSeleccion: ((PuntoCenso?) -> Unit)? = null

    /** Se tocó un punto con el mapa demasiado alejado. */
    var onZoomInsuficiente: (() -> Unit)? = null

    // ── Pinturas reutilizadas: cero asignaciones por cuadro ──────────────────
    private val relleno = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val borde = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        color = Color.WHITE                  // contorno para que se vean sobre el satélite
    }
    private val anilloSeleccion = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 5f
        color = Color.BLACK
    }
    private val punto = Point()
    private val geo   = GeoPoint(0.0, 0.0)

    /**
     * @param lista   palmas a dibujar (solo las que tienen ubicación)
     * @param ordenEventos ids de evento en el orden de la lista: el primero queda encima
     * @param coloresPorEvento color de cada evento
     */
    fun setPuntos(lista: List<PuntoCenso>, ordenEventos: List<Int>, coloresPorEvento: Map<Int, Int>) {
        val pos = HashMap<Int, Int>()
        ordenEventos.forEachIndexed { i, id -> pos[id] = i }
        // De último en la lista a primero: el primero se pinta al final, encima.
        puntos = lista.sortedByDescending { pos[it.eventoId] ?: Int.MAX_VALUE }
        colores = coloresPorEvento
        seleccionadoId = null
    }

    fun limpiar() {
        puntos = emptyList()
        seleccionadoId = null
    }

    fun hayPuntos(): Boolean = puntos.isNotEmpty()

    override fun draw(canvas: Canvas, mapView: MapView, shadow: Boolean) {
        if (shadow) return
        val lista = puntos
        if (lista.isEmpty()) return

        val proj  = mapView.projection
        val ancho = mapView.width
        val alto  = mapView.height
        val zoom  = mapView.zoomLevelDouble
        // Pequeños de lejos para ver la distribución; más grandes de cerca
        // para poder tocarlos.
        val radio = when {
            zoom >= 18.0 -> 9f
            zoom >= 16.0 -> 6f
            zoom >= 14.0 -> 4f
            else         -> 3f
        }
        val colorDefecto = Color.rgb(85, 85, 85)
        val cols = colores
        var seleccionado: PuntoCenso? = null

        for (p in lista) {
            val lat = p.lat ?: continue
            val lon = p.lon ?: continue
            geo.setCoords(lat, lon)
            proj.toPixels(geo, punto)
            if (punto.x < -20 || punto.x > ancho + 20 || punto.y < -20 || punto.y > alto + 20) continue
            if (p.id == seleccionadoId) { seleccionado = p; continue }   // se pinta al final
            relleno.color = cols[p.eventoId] ?: colorDefecto
            val x = punto.x.toFloat(); val y = punto.y.toFloat()
            canvas.drawCircle(x, y, radio, relleno)
            if (radio >= 4f) canvas.drawCircle(x, y, radio, borde)
        }

        // La seleccionada, encima de todas y con un anillo negro
        val s = seleccionado
        if (s != null) {
            geo.setCoords(s.lat!!, s.lon!!)
            proj.toPixels(geo, punto)
            val x = punto.x.toFloat(); val y = punto.y.toFloat()
            relleno.color = cols[s.eventoId] ?: colorDefecto
            canvas.drawCircle(x, y, radio + 4f, relleno)
            canvas.drawCircle(x, y, radio + 4f, borde)
            canvas.drawCircle(x, y, radio + 7f, anilloSeleccion)
        }
    }

    /**
     * Selecciona el punto más cercano al toque. Tocar el mismo otra vez lo
     * deselecciona. Devuelve false si no tocó ninguno, para que el toque siga
     * a las capas de abajo (palmas, trampas) o al mapa.
     */
    override fun onSingleTapConfirmed(e: MotionEvent, mapView: MapView): Boolean {
        val lista = puntos
        if (lista.isEmpty()) return false

        val proj = mapView.projection
        var mejor: PuntoCenso? = null
        var mejorDist = Float.MAX_VALUE
        for (p in lista) {
            val lat = p.lat ?: continue
            val lon = p.lon ?: continue
            geo.setCoords(lat, lon)
            proj.toPixels(geo, punto)
            val d = hypot(punto.x - e.x, punto.y - e.y)
            if (d < mejorDist) { mejorDist = d; mejor = p }
        }

        val elegido = mejor
        if (elegido == null || mejorDist > RADIO_TOQUE_PX) return false

        // Solo se avisa si el toque cayó sobre un punto: así tocar el mapa
        // para moverlo no llena la pantalla de mensajes. Se da el toque por
        // atendido para que la capa de palmas, debajo, no repita el aviso.
        if (mapView.zoomLevelDouble < ZOOM_MIN_SELECCION) {
            onZoomInsuficiente?.invoke()
            return true
        }

        seleccionadoId = if (seleccionadoId == elegido.id) {
            onSeleccion?.invoke(null)
            null
        } else {
            onSeleccion?.invoke(elegido)
            elegido.id
        }
        mapView.invalidate()
        return true
    }
}
