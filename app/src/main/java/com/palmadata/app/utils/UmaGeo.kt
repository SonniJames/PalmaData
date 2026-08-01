package com.palmadata.app.utils

import com.palmadata.app.data.model.UmaData
import org.json.JSONObject
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot

class UmaPoligono(val uma: UmaData) {

    val anillos = mutableListOf<List<Pair<Double, Double>>>()
    var minLat =  90.0; var maxLat = -90.0
    var minLon = 180.0; var maxLon = -180.0

    init {
        val geo    = JSONObject(uma.geojson)
        val tipo   = geo.getString("type")
        val coords = geo.getJSONArray("coordinates")

        // Soporta tanto Polygon como MultiPolygon
        val poligonos = if (tipo == "MultiPolygon") {
            (0 until coords.length()).map { coords.getJSONArray(it).getJSONArray(0) }
        } else {
            listOf(coords.getJSONArray(0))
        }

        poligonos.forEach { exterior ->
            val anillo = mutableListOf<Pair<Double, Double>>()
            for (i in 0 until exterior.length()) {
                val pt  = exterior.getJSONArray(i)
                val lon = pt.getDouble(0)
                val lat = pt.getDouble(1)
                anillo.add(lon to lat)
                if (lat < minLat) minLat = lat
                if (lat > maxLat) maxLat = lat
                if (lon < minLon) minLon = lon
                if (lon > maxLon) maxLon = lon
            }
            anillos.add(anillo)
        }
    }

    fun contiene(lat: Double, lon: Double): Boolean {
        // Bounding box primero (rapidísimo)
        if (lat < minLat || lat > maxLat || lon < minLon || lon > maxLon) return false
        // Ray casting sobre cada anillo
        return anillos.any { rayCasting(it, lat, lon) }
    }

    /**
     * Distancia en metros del punto al BORDE del polígono. 0 si está dentro.
     *
     * Para acotar el costo, primero descarta por bounding box expandido según
     * el umbral: solo los polígonos que podrían estar cerca pagan el recorrido
     * de sus vértices. Devuelve null si está claramente más lejos del umbral.
     *
     * Sobre distancias cortas (decenas de metros) basta con proyectar los
     * grados a metros de forma local, sin trigonometría esférica: el error es
     * de centímetros, muy por debajo de la precisión del GPS.
     */
    fun distanciaMetros(lat: Double, lon: Double, umbralMetros: Double): Double? {
        if (contiene(lat, lon)) return 0.0

        val metrosPorGradoLat = 111_320.0
        val metrosPorGradoLon = 111_320.0 * cos(Math.toRadians(lat))
        if (metrosPorGradoLon <= 0.0) return null

        // Pre-filtro: bounding box expandido por el umbral
        val margenLat = umbralMetros / metrosPorGradoLat
        val margenLon = umbralMetros / metrosPorGradoLon
        if (lat < minLat - margenLat || lat > maxLat + margenLat ||
            lon < minLon - margenLon || lon > maxLon + margenLon) return null

        // Distancia mínima a todos los segmentos de todos los anillos
        var minDist = Double.MAX_VALUE
        anillos.forEach { anillo ->
            if (anillo.size >= 2) {
                var j = anillo.size - 1
                for (i in anillo.indices) {
                    val (lonA, latA) = anillo[j]
                    val (lonB, latB) = anillo[i]
                    // Coordenadas locales en metros, relativas al punto consultado
                    val ax = (lonA - lon) * metrosPorGradoLon
                    val ay = (latA - lat) * metrosPorGradoLat
                    val bx = (lonB - lon) * metrosPorGradoLon
                    val by = (latB - lat) * metrosPorGradoLat
                    val d = distanciaAlSegmento(ax, ay, bx, by)
                    if (d < minDist) minDist = d
                    j = i
                }
            }
        }
        return if (minDist <= umbralMetros) minDist else null
    }

    /** Distancia del origen (0,0) al segmento A-B, en las mismas unidades. */
    private fun distanciaAlSegmento(ax: Double, ay: Double, bx: Double, by: Double): Double {
        val dx = bx - ax
        val dy = by - ay
        if (abs(dx) < 1e-9 && abs(dy) < 1e-9) return hypot(ax, ay)
        // Proyección del origen sobre el segmento, recortada a [0,1]
        var t = -(ax * dx + ay * dy) / (dx * dx + dy * dy)
        t = t.coerceIn(0.0, 1.0)
        return hypot(ax + t * dx, ay + t * dy)
    }
    private fun rayCasting(anillo: List<Pair<Double, Double>>, lat: Double, lon: Double): Boolean {
        var dentro = false
        var j = anillo.size - 1
        for (i in anillo.indices) {
            val (xi, yi) = anillo[i]
            val (xj, yj) = anillo[j]
            if ((yi > lat) != (yj > lat) &&
                lon < (xj - xi) * (lat - yi) / (yj - yi) + xi
            ) dentro = !dentro
            j = i
        }
        return dentro
    }
}

object UmaLocator {
    fun umaEn(poligonos: List<UmaPoligono>, lat: Double, lon: Double): UmaPoligono? =
        poligonos.firstOrNull { it.contiene(lat, lon) }

    /**
     * Uma más cercana dentro del umbral, junto con su distancia en metros.
     * Null si ninguna está a esa distancia. Se usa para el aviso de
     * proximidad; con excluirId se omite la uma en la que ya se está.
     */
    fun umaMasCercana(
        poligonos: List<UmaPoligono>,
        lat: Double,
        lon: Double,
        umbralMetros: Double,
        excluirId: Int? = null
    ): Pair<UmaPoligono, Double>? {
        var mejor: UmaPoligono? = null
        var mejorDist = Double.MAX_VALUE
        poligonos.forEach { p ->
            // Se excluye la uma en la que ya se está: su distancia es 0 y
            // anunciarla sería repetir la que el operario acaba de oír al entrar.
            if (excluirId == null || p.uma.nutUmaPolId != excluirId) {
                val d = p.distanciaMetros(lat, lon, umbralMetros)
                if (d != null && d < mejorDist) { mejorDist = d; mejor = p }
            }
        }
        return mejor?.let { it to mejorDist }
    }
}