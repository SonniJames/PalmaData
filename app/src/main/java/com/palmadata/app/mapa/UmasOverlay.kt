package com.palmadata.app.mapa

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Point
import android.graphics.RectF
import com.palmadata.app.utils.UmaPoligono
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay

/**
 * Overlay único que dibuja TODOS los polígonos de las umas (y sus etiquetas)
 * en una sola pasada de Canvas.
 *
 * Antes: cada uma era un objeto Polygon + un Marker de osmdroid (~900 overlays
 * con 440 umas) y en cada gesto de zoom/scroll osmdroid los recorría todos.
 * Ahora: un (1) solo overlay con pinturas y objetos reutilizados (cero
 * asignaciones de memoria por cuadro), culling por pantalla (lo que no se ve
 * no se dibuja) y etiquetas solo desde zoom 14.5 para no saturar de lejos.
 *
 * La uma actual se resalta en amarillo llamando setUmaActual(id).
 */
class UmasOverlay : Overlay() {

    @Volatile private var poligonos: List<UmaPoligono> = emptyList()
    @Volatile private var umaActualId: Int? = null

    // ── Pinturas reutilizadas (crearlas por cuadro mataría el rendimiento) ────
    private val fillNormal = Paint().apply {
        style = Paint.Style.FILL
        color = Color.argb(60, 76, 175, 80)
        isAntiAlias = true
    }
    private val fillActual = Paint().apply {
        style = Paint.Style.FILL
        color = Color.argb(110, 255, 193, 7)
        isAntiAlias = true
    }
    private val borde = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        // Blanco: sobre la imagen satelital (vegetación verde) el borde verde
        // oscuro se perdía; el blanco es el estándar sobre fotografía aérea
        color = Color.WHITE
        isAntiAlias = true
    }
    private val textoEtiqueta = Paint().apply {
        color = Color.rgb(27, 94, 32)
        textSize = 36f
        isAntiAlias = true
        isFakeBoldText = true
        textAlign = Paint.Align.CENTER
    }
    private val fondoEtiqueta = Paint().apply {
        style = Paint.Style.FILL
        color = Color.argb(170, 255, 255, 255)
        isAntiAlias = true
    }

    // ── Objetos de trabajo reutilizados ────────────────────────────────────────
    private val path  = Path()
    private val punto = Point()
    private val geo   = GeoPoint(0.0, 0.0)
    private val rectEtiqueta = RectF()

    fun setPoligonos(lista: List<UmaPoligono>) { poligonos = lista }

    fun setUmaActual(id: Int?) { umaActualId = id }

    override fun draw(canvas: Canvas, mapView: MapView, shadow: Boolean) {
        if (shadow) return
        val lista = poligonos
        if (lista.isEmpty()) return

        val proj  = mapView.projection
        val ancho = mapView.width.toFloat()
        val alto  = mapView.height.toFloat()
        // De lejos las 440 etiquetas se encimarían; se muestran al acercarse
        val dibujarEtiquetas = mapView.zoomLevelDouble >= 14.5

        lista.forEach { up ->
            // ── Culling: proyectar el bbox de la uma y saltarla si no se ve ────
            geo.setCoords(up.maxLat, up.minLon)
            proj.toPixels(geo, punto)
            val izq = punto.x.toFloat(); val arriba = punto.y.toFloat()
            geo.setCoords(up.minLat, up.maxLon)
            proj.toPixels(geo, punto)
            val der = punto.x.toFloat(); val abajo = punto.y.toFloat()

            if (der < 0f || izq > ancho || abajo < 0f || arriba > alto) return@forEach

            val relleno = if (up.uma.nutUmaPolId == umaActualId) fillActual else fillNormal

            // ── Polígonos de la uma ────────────────────────────────────────────
            up.anillos.forEach { anillo ->
                path.reset()
                anillo.forEachIndexed { i, (lon, lat) ->
                    geo.setCoords(lat, lon)
                    proj.toPixels(geo, punto)
                    if (i == 0) path.moveTo(punto.x.toFloat(), punto.y.toFloat())
                    else        path.lineTo(punto.x.toFloat(), punto.y.toFloat())
                }
                path.close()
                canvas.drawPath(path, relleno)
                canvas.drawPath(path, borde)
            }

            // ── Etiqueta con el código de la uma ───────────────────────────────
            if (dibujarEtiquetas) {
                val cx = (izq + der) / 2f
                val cy = (arriba + abajo) / 2f
                val texto = up.uma.codigo
                val mitad = textoEtiqueta.measureText(texto) / 2f
                rectEtiqueta.set(cx - mitad - 8f, cy - 26f, cx + mitad + 8f, cy + 14f)
                canvas.drawRoundRect(rectEtiqueta, 8f, 8f, fondoEtiqueta)
                canvas.drawText(texto, cx, cy + 6f, textoEtiqueta)
            }
        }
    }
}
