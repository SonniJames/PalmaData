package com.palmadata.app.mapa

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Point
import com.palmadata.app.data.model.TrampaMapa
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay

/**
 * Dibuja las trampas de la plantación (unas 200) como puntos con su código.
 *
 * A diferencia de las palmas, aquí sí van etiquetas: son pocas, están
 * separadas y el código es justamente lo que el operario necesita leer. Se
 * pintan con halo blanco para que se lean sobre la imagen satelital. Las
 * etiquetas aparecen desde el zoom 14; los puntos, siempre.
 */
class TrampasOverlay : Overlay() {

    companion object { private const val ZOOM_MIN_ETIQUETA = 14.0 }

    @Volatile private var trampas: List<TrampaMapa> = emptyList()

    private val relleno = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.rgb(255, 152, 0)       // naranja: distinto de palmas (azul) y de mi posición (rojo)
    }
    private val borde = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = 2f; color = Color.WHITE
    }
    private val texto = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE; textSize = 28f; isFakeBoldText = true
    }
    private val halo = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK; textSize = 28f; isFakeBoldText = true
        style = Paint.Style.STROKE; strokeWidth = 5f
    }
    private val punto = Point()
    private val geo   = GeoPoint(0.0, 0.0)

    fun setTrampas(lista: List<TrampaMapa>) { trampas = lista }
    fun limpiar() { trampas = emptyList() }
    fun hayTrampas(): Boolean = trampas.isNotEmpty()

    override fun draw(canvas: Canvas, mapView: MapView, shadow: Boolean) {
        if (shadow) return
        val lista = trampas
        if (lista.isEmpty()) return
        val proj  = mapView.projection
        val ancho = mapView.width
        val alto  = mapView.height
        val zoom  = mapView.zoomLevelDouble
        val radio = if (zoom >= 17) 14f else if (zoom >= 15) 10f else 7f
        val conEtiqueta = zoom >= ZOOM_MIN_ETIQUETA

        for (t in lista) {
            geo.setCoords(t.lat, t.lon)
            proj.toPixels(geo, punto)
            // Fuera de pantalla: no se dibuja
            if (punto.x < -80 || punto.x > ancho + 80 || punto.y < -80 || punto.y > alto + 80) continue
            val x = punto.x.toFloat(); val y = punto.y.toFloat()
            canvas.drawCircle(x, y, radio, relleno)
            canvas.drawCircle(x, y, radio, borde)
            if (conEtiqueta) {
                canvas.drawText(t.codigo, x + radio + 6f, y + 10f, halo)
                canvas.drawText(t.codigo, x + radio + 6f, y + 10f, texto)
            }
        }
    }
}
