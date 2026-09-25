package com.palmadata.app.tratamientos

import android.content.Intent
import org.json.JSONArray
import org.json.JSONObject

/**
 * Selección de productos del tratamiento, tal como viaja entre las pantallas
 * PRODUCTOS → UNIDADES → CANTIDADES → REMISIÓN → observaciones, en el extra
 * "productos_sel" (JSON). Cada pantalla completa su parte:
 *
 *   PRODUCTOS   producto_id, producto_nombre
 *   UNIDADES    + unidad_aplicacion_id, unidad_nombre   (pueden faltar)
 *   CANTIDADES  + cantidad                              (puede faltar)
 *   REMISIÓN    + remision                              (puede faltar)
 *
 * Al guardar, [aBaseDeDatos] deja SOLO los ids, la cantidad y la remisión,
 * que es lo que
 * se escribe en la columna jsonb `producto` de san_enf_tratamiento:
 *   [{"producto_id":12,"unidad_aplicacion_id":3,"cantidad":1.5,"remision":2015}, ...]
 *
 * La remisión es POR PRODUCTO, no una sola por registro: en una misma
 * aplicación cada producto pudo llegar en una remisión distinta.
 * Los nombres solo sirven para mostrar; nunca van a la base.
 */
object ProductosSeleccion {
    const val EXTRA = "productos_sel"

    fun leer(intent: Intent): JSONArray =
        try { JSONArray(intent.getStringExtra(EXTRA) ?: "[]") } catch (e: Exception) { JSONArray() }

    /** JSON final para la columna `producto`; null si no se seleccionó ninguno. */
    fun aBaseDeDatos(sel: JSONArray): String? {
        if (sel.length() == 0) return null
        val salida = JSONArray()
        for (i in 0 until sel.length()) {
            val p = sel.getJSONObject(i)
            salida.put(JSONObject().apply {
                put("producto_id", p.getInt("producto_id"))
                put("unidad_aplicacion_id", if (p.has("unidad_aplicacion_id")) p.getInt("unidad_aplicacion_id") else JSONObject.NULL)
                put("cantidad", if (p.has("cantidad")) p.getDouble("cantidad") else JSONObject.NULL)
                put("remision", if (p.has("remision")) p.getLong("remision") else JSONObject.NULL)
            })
        }
        return salida.toString()
    }
}
