package com.palmadata.app.utils

import com.palmadata.app.data.model.LoteMapa
import org.json.JSONArray
import org.json.JSONObject

/**
 * Polígono de un lote listo para consultar: anillos ya parseados desde el
 * GeoJSON y bounding box precalculado.
 *
 * Mismo enfoque que UmaPoligono: el parseo se hace UNA vez al cargar, no en
 * cada fix del GPS. Con la posición llegando cada segundo, volver a leer el
 * JSON de cientos de lotes por fix sería inviable.
 */
class LotePoligono(val lote: LoteMapa) {

    /** Lista de anillos; cada anillo es una lista de pares (lon, lat) */
    val anillos: List<List<Pair<Double, Double>>>

    var minLat = 90.0;   private set
    var maxLat = -90.0;  private set
    var minLon = 180.0;  private set
    var maxLon = -180.0; private set

    init {
        val lista = mutableListOf<List<Pair<Double, Double>>>()
        val obj = JSONObject(lote.geojson)
        val tipo = obj.getString("type")
        val coords = obj.getJSONArray("coordinates")

        when (tipo) {
            "Polygon" -> lista.add(leerAnillo(coords.getJSONArray(0)))
            "MultiPolygon" -> {
                for (i in 0 until coords.length()) {
                    lista.add(leerAnillo(coords.getJSONArray(i).getJSONArray(0)))
                }
            }
        }
        anillos = lista

        anillos.forEach { anillo ->
            anillo.forEach { (lon, lat) ->
                if (lat < minLat) minLat = lat
                if (lat > maxLat) maxLat = lat
                if (lon < minLon) minLon = lon
                if (lon > maxLon) maxLon = lon
            }
        }
    }

    private fun leerAnillo(arr: JSONArray): List<Pair<Double, Double>> {
        val puntos = ArrayList<Pair<Double, Double>>(arr.length())
        for (i in 0 until arr.length()) {
            val p = arr.getJSONArray(i)
            puntos.add(Pair(p.getDouble(0), p.getDouble(1)))
        }
        return puntos
    }

    /** ¿El punto cae dentro? Primero descarta por bounding box (barato). */
    fun contiene(lat: Double, lon: Double): Boolean {
        if (lat < minLat || lat > maxLat || lon < minLon || lon > maxLon) return false
        return anillos.any { rayCasting(it, lat, lon) }
    }

    private fun rayCasting(anillo: List<Pair<Double, Double>>, lat: Double, lon: Double): Boolean {
        var dentro = false
        var j = anillo.size - 1
        for (i in anillo.indices) {
            val (loni, lati) = anillo[i]
            val (lonj, latj) = anillo[j]
            if ((lati > lat) != (latj > lat) &&
                lon < (lonj - loni) * (lat - lati) / (latj - lati) + loni
            ) dentro = !dentro
            j = i
        }
        return dentro
    }
}

object LoteLocator {
    fun loteEn(poligonos: List<LotePoligono>, lat: Double, lon: Double): LotePoligono? =
        poligonos.firstOrNull { it.contiene(lat, lon) }
}