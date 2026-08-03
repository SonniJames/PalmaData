package com.palmadata.app.mapa

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Point
import android.graphics.RectF
import com.palmadata.app.utils.LotePoligono
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay

/**
 * Overlay único que dibuja TODOS los polígonos de los lotes y sus etiquetas en
 * una sola pasada de Canvas. Mismo diseño que UmasOverlay: pinturas y objetos
 * reutilizados (cero asignaciones por cuadro), culling por pantalla y
 * etiquetas solo al acercarse.
 *
 * El lote donde está parado el operario se resalta llamando setLoteActual(id).
 */
class LotesOverlay : Overlay() {

    @Volatile private var poligonos: List<LotePoligono> = emptyList()
    @Volatile private var loteActualId: Int? = null

    // ── Pinturas reutilizadas ─────────────────────────────────────────────────
    private val fillNormal = Paint().apply {
        style = Paint.Style.FILL
        // Azul tenue: distingue este módulo del de fertilización (verde)
        color = Color.argb(50, 33, 150, 243)
        isAntiAlias = true
    }
    private val fillActual = Paint().apply {
        style = Paint.Style.FILL
        // Naranja para el lote donde se está parado
        color = Color.argb(120, 255, 152, 0)
        isAntiAlias = true
    }
    private val borde = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        color = Color.WHITE      // el blanco es el estándar sobre imagen satelital
        isAntiAlias = true
    }
    private val textoEtiqueta = Paint().apply {
        color = Color.rgb(13, 71, 161)
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

    // ── Objetos de trabajo reutilizados ───────────────────────────────────────
    private val path  = Path()
    private val punto = Point()
    private val geo   = GeoPoint(0.0, 0.0)
    private val rectEtiqueta = RectF()

    fun setPoligonos(lista: List<LotePoligono>) { poligonos = lista }

    fun setLoteActual(id: Int?) { loteActualId = id }

    override fun draw(canvas: Canvas, mapView: MapView, shadow: Boolean) {
        if (shadow) return
        val lista = poligonos
        if (lista.isEmpty()) return

        val proj  = mapView.projection
        val ancho = mapView.width.toFloat()
        val alto  = mapView.height.toFloat()
        val dibujarEtiquetas = mapView.zoomLevelDouble >= 14.5

        lista.forEach { lp ->
            // ── Culling: proyectar el bbox del lote y saltarlo si no se ve ────
            geo.setCoords(lp.maxLat, lp.minLon)
            proj.toPixels(geo, punto)
            val izq = punto.x.toFloat(); val arriba = punto.y.toFloat()
            geo.setCoords(lp.minLat, lp.maxLon)
            proj.toPixels(geo, punto)
            val der = punto.x.toFloat(); val abajo = punto.y.toFloat()

            if (der < 0f || izq > ancho || abajo < 0f || arriba > alto) return@forEach

            val relleno = if (lp.lote.catLoteId == loteActualId) fillActual else fillNormal

            lp.anillos.forEach { anillo ->
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

            if (dibujarEtiquetas) {
                val cx = (izq + der) / 2f
                val cy = (arriba + abajo) / 2f
                val texto = lp.lote.nombre
                val mitad = textoEtiqueta.measureText(texto) / 2f
                rectEtiqueta.set(cx - mitad - 8f, cy - 26f, cx + mitad + 8f, cy + 14f)
                canvas.drawRoundRect(rectEtiqueta, 8f, 8f, fondoEtiqueta)
                canvas.drawText(texto, cx, cy + 6f, textoEtiqueta)
            }
        }
    }
}