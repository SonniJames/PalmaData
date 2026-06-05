package com.palmadata.app.utils

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
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
            // ── Subir pendientes ──────────────────────────────────────────────
            val subidosCenso = subirPendientes(baseUrl, "censo_enfermedades", db.getCensoEnfPendientes()) { id ->
                db.eliminarCensoEnf(id)
            }
            val subidosTrat = subirPendientes(baseUrl, "tratamientos", db.getTratamientosPendientes()) { id ->
                db.eliminarTratamiento(id)
            }
            val subidosPoli = subirPendientes(baseUrl, "polinizacion", db.getPolinizacionPendientes()) { id ->
                db.eliminarPolinizacion(id)
            }

            // ── Descargar maestros ────────────────────────────────────────────
            val plantaciones = fetchLista(baseUrl, "plantaciones") { obj -> Pair(obj.getInt("id"), obj.getString("nombre")) }
            db.reemplazarPlantaciones(plantaciones)

            val trabajadores = fetchLista(baseUrl, "trabajadores") { obj -> Pair(obj.getInt("id"), obj.getString("nombre")) }
            db.reemplazarTrabajadores(trabajadores)

            val sectores = fetchLista(baseUrl, "sectores") { obj -> Triple(obj.getInt("id"), obj.getString("nombre"), obj.getInt("plantacion_id")) }
            db.reemplazarSectores(sectores)

            val lotes = fetchLista(baseUrl, "lotes") { obj -> Triple(obj.getInt("id"), obj.getString("nombre"), obj.getInt("sector_id")) }
            db.reemplazarLotes(lotes)

            val enfermedades = fetchLista(baseUrl, "enfermedades") { obj -> Pair(obj.getInt("id"), obj.getString("nombre")) }
            db.reemplazarEnfermedades(enfermedades)

            val eventos = fetchLista(baseUrl, "eventos") { obj -> Triple(obj.getInt("id"), obj.getString("codigo"), obj.getInt("enfermedad_id")) }
            db.reemplazarEventos(eventos)

            val tratEventos = fetchLista(baseUrl, "tratamientos_eventos") { obj -> Pair(obj.getInt("id"), obj.getString("codigo")) }
            db.reemplazarTratamientosEventos(tratEventos)

            guardarFechaSincronizacion(context)

            ResultadoSync(
                exitoso  = true,
                detalles = mapOf(
                    "Censo enfermedades" to subidosCenso,
                    "Tratamientos"       to subidosTrat,
                    "Polinización"       to subidosPoli,
                    "Plantaciones"       to plantaciones.size,
                    "Trabajadores"       to trabajadores.size,
                    "Sectores"           to sectores.size,
                    "Lotes"              to lotes.size,
                    "Enfermedades"       to enfermedades.size,
                    "Eventos"            to eventos.size,
                    "Trat. eventos"      to tratEventos.size
                )
            )
        } catch (e: Exception) {
            ResultadoSync(exitoso = false, mensaje = e.message ?: "Error desconocido")
        }
    }

    private fun subirPendientes(baseUrl: String, endpoint: String, pendientes: List<Map<String, Any>>, onExito: (String) -> Unit): Int {
        var subidos = 0
        pendientes.forEach { registro ->
            if (subirRegistro(baseUrl, endpoint, registro)) {
                onExito(registro["id"].toString())
                subidos++
            }
        }
        return subidos
    }

    private fun subirRegistro(baseUrl: String, endpoint: String, registro: Map<String, Any>): Boolean {
        return try {
            val url = URL("$baseUrl/$endpoint")
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 10_000; connection.readTimeout = 10_000
            connection.requestMethod = "POST"; connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            connection.connect()
            connection.outputStream.bufferedWriter().use { it.write(JSONObject(registro).toString()) }
            val code = connection.responseCode
            val response = connection.inputStream.bufferedReader().readText()
            connection.disconnect()
            code == 200 && !JSONObject(response).has("error")
        } catch (e: Exception) { false }
    }

    private fun <T> fetchLista(baseUrl: String, endpoint: String, mapper: (JSONObject) -> T): List<T> {
        val url = URL("$baseUrl/$endpoint")
        val connection = url.openConnection() as HttpURLConnection
        connection.connectTimeout = 10_000; connection.readTimeout = 10_000
        connection.requestMethod = "GET"; connection.connect()
        if (connection.responseCode != 200) throw Exception("Error en /$endpoint: ${connection.responseCode}")
        val response = connection.inputStream.bufferedReader().readText()
        connection.disconnect()
        val array = JSONArray(response)
        return (0 until array.length()).map { mapper(array.getJSONObject(it)) }
    }

    private const val PREFS_SYNC    = "palma_sync"
    private const val KEY_LAST_SYNC = "ultima_sincronizacion"

    private fun guardarFechaSincronizacion(context: Context) {
        val ahora = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        context.getSharedPreferences(PREFS_SYNC, Context.MODE_PRIVATE).edit().putString(KEY_LAST_SYNC, ahora).apply()
    }

    fun getUltimaSincronizacion(context: Context): String =
        context.getSharedPreferences(PREFS_SYNC, Context.MODE_PRIVATE).getString(KEY_LAST_SYNC, "Sin sincronización aún") ?: "Sin sincronización aún"
}