package com.palmadata.app.utils

import com.palmadata.app.data.model.UmaData
import org.json.JSONObject

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
}