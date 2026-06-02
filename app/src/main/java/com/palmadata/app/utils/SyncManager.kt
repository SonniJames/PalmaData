package com.palmadata.app.utils

import android.content.Context
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

object SyncManager {

    data class ResultadoSync(
        val exitoso: Boolean,
        val mensaje: String = "",
        val detalles: Map<String, Int> = emptyMap()
    )

    fun sincronizar(context: Context): ResultadoSync {
        val baseUrl = ServerConfig.getBaseUrl(context)
        val db      = DatabaseHelper(context)

        return try {
            val plantaciones = fetchLista(baseUrl, "plantaciones") { obj ->
                Pair(obj.getInt("id"), obj.getString("nombre"))
            }
            db.reemplazarPlantaciones(plantaciones)

            val trabajadores = fetchLista(baseUrl, "trabajadores") { obj ->
                Pair(obj.getInt("id"), obj.getString("nombre"))
            }
            db.reemplazarTrabajadores(trabajadores)

            // Sectores ahora incluye plantacion_id
            val sectores = fetchLista(baseUrl, "sectores") { obj ->
                Triple(obj.getInt("id"), obj.getString("nombre"), obj.getInt("plantacion_id"))
            }
            db.reemplazarSectores(sectores)

            val lotes = fetchLista(baseUrl, "lotes") { obj ->
                Triple(obj.getInt("id"), obj.getString("nombre"), obj.getInt("sector_id"))
            }
            db.reemplazarLotes(lotes)

            val enfermedades = fetchLista(baseUrl, "enfermedades") { obj ->
                Pair(obj.getInt("id"), obj.getString("nombre"))
            }
            db.reemplazarEnfermedades(enfermedades)

            val eventos = fetchLista(baseUrl, "eventos") { obj ->
                Triple(obj.getInt("id"), obj.getString("codigo"), obj.getInt("enfermedad_id"))
            }
            db.reemplazarEventos(eventos)

            guardarFechaSincronizacion(context)

            ResultadoSync(
                exitoso  = true,
                mensaje  = "Sincronización exitosa",
                detalles = mapOf(
                    "Plantaciones" to plantaciones.size,
                    "Trabajadores" to trabajadores.size,
                    "Sectores"     to sectores.size,
                    "Lotes"        to lotes.size,
                    "Enfermedades" to enfermedades.size,
                    "Eventos"      to eventos.size
                )
            )
        } catch (e: Exception) {
            ResultadoSync(exitoso = false, mensaje = e.message ?: "Error desconocido")
        }
    }

    private fun <T> fetchLista(baseUrl: String, endpoint: String, mapper: (org.json.JSONObject) -> T): List<T> {
        val url        = URL("$baseUrl/$endpoint")
        val connection = url.openConnection() as HttpURLConnection
        connection.connectTimeout = 10_000
        connection.readTimeout    = 10_000
        connection.requestMethod  = "GET"
        connection.connect()

        if (connection.responseCode != 200)
            throw Exception("Error en /$endpoint: código ${connection.responseCode}")

        val response = connection.inputStream.bufferedReader().readText()
        connection.disconnect()

        val array  = JSONArray(response)
        val result = mutableListOf<T>()
        for (i in 0 until array.length()) result.add(mapper(array.getJSONObject(i)))
        return result
    }

    private const val PREFS_SYNC    = "palma_sync"
    private const val KEY_LAST_SYNC = "ultima_sincronizacion"

    private fun guardarFechaSincronizacion(context: Context) {
        val ahora = java.text.SimpleDateFormat(
            "dd/MM/yyyy HH:mm:ss", java.util.Locale.getDefault()
        ).format(java.util.Date())
        context.getSharedPreferences(PREFS_SYNC, Context.MODE_PRIVATE)
            .edit().putString(KEY_LAST_SYNC, ahora).apply()
    }

    fun getUltimaSincronizacion(context: Context): String {
        return context.getSharedPreferences(PREFS_SYNC, Context.MODE_PRIVATE)
            .getString(KEY_LAST_SYNC, "Sin sincronización aún") ?: "Sin sincronización aún"
    }
}