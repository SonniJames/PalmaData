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
            val subidosTracks = subirTracks(baseUrl, context)
            val subidosCenso = subirPendientes(baseUrl, "censo_enfermedades", db.getCensoEnfPendientes()) { id -> db.eliminarCensoEnf(id) }
            val subidosTrat = subirPendientes(baseUrl, "tratamientos", db.getTratamientosPendientes()) { id -> db.eliminarTratamiento(id) }
            val subidosPoli = subirPendientes(baseUrl, "polinizacion", db.getPolinizacionPendientes()) { id -> db.eliminarPolinizacion(id) }
            val subidosPolen = subirPendientes(baseUrl, "polen_inicial_final", db.getPolenPendientes()) { id -> db.eliminarPolen(id) }
            val subidosStrategus = subirPendientes(baseUrl, "sanstrategus", db.getStrateguspendientes()) { id -> db.eliminarStrategus(id) }
            val subidosTrampas = subirPendientes(baseUrl, "censo_trampas", db.getTrampasPendientes()) { id -> db.eliminarTrampa(id) }
            val subidosPlagas = subirPendientes(baseUrl, "muestreo_plagas", db.getPlagasPendientes()) { id -> db.eliminarPlagas(id) }
            // Super cosecha usa id_unico como clave
            val subidosSuperCosecha = subirPendientes(baseUrl, "super_cosecha", db.getSuperCosechaPendientes(), idKey = "id_unico") { id -> db.eliminarSuperCosecha(id) }

            // ── Descargar maestros ────────────────────────────────────────────
            val plantaciones = fetchLista(baseUrl, "plantaciones") { obj -> Pair(obj.getInt("id"), obj.getString("nombre")) }
            db.reemplazarPlantaciones(plantaciones)

            val trabajadores = fetchLista(baseUrl, "trabajadores") { obj -> Triple(obj.getInt("id"), obj.getString("nombre"), obj.optInt("supervisor", 0)) }
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

            val trampas = fetchLista(baseUrl, "trampas") { obj -> Pair(obj.getInt("id"), obj.getString("codigo")) }
            db.reemplazarTrampas(trampas)

            val insectos = fetchLista(baseUrl, "insectos") { obj -> Pair(obj.getInt("id"), obj.getString("insecto")) }
            db.reemplazarInsectos(insectos)

            val estadosInsecto = fetchLista(baseUrl, "estados_insecto") { obj -> Triple(obj.getInt("id"), obj.getString("estado"), obj.getInt("insecto_id")) }
            db.reemplazarEstadosInsecto(estadosInsecto)

            guardarFechaSincronizacion(context)

            ResultadoSync(
                exitoso  = true,
                detalles = mapOf(
                    "Tracks"              to subidosTracks,
                    "Censo enfermedades"  to subidosCenso,
                    "Tratamientos"        to subidosTrat,
                    "Polinización"        to subidosPoli,
                    "Polen inicial/final" to subidosPolen,
                    "Sanstrategus"        to subidosStrategus,
                    "Censo trampas"       to subidosTrampas,
                    "Muestreo plagas"     to subidosPlagas,
                    "Super cosecha"       to subidosSuperCosecha,
                    "Plantaciones"        to plantaciones.size,
                    "Trabajadores"        to trabajadores.size,
                    "Sectores"            to sectores.size,
                    "Lotes"               to lotes.size,
                    "Enfermedades"        to enfermedades.size,
                    "Eventos"             to eventos.size,
                    "Trat. eventos"       to tratEventos.size,
                    "Trampas"             to trampas.size,
                    "Insectos"            to insectos.size,
                    "Estados insecto"     to estadosInsecto.size
                )
            )
        } catch (e: Exception) {
            ResultadoSync(exitoso = false, mensaje = e.message ?: "Error desconocido")
        }
    }

    private fun subirTracks(baseUrl: String, context: Context): Int {
        return try {
            val prefs = context.getSharedPreferences("palma_tracks", Context.MODE_PRIVATE)
            val json  = prefs.getString("tracks_pendientes", "[]") ?: "[]"
            val array = JSONArray(json)
            if (array.length() == 0) return 0
            val url        = URL("$baseUrl/tracks")
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 15_000; connection.readTimeout = 15_000
            connection.requestMethod = "POST"; connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            connection.connect()
            connection.outputStream.bufferedWriter().use { it.write(array.toString()) }
            val code     = connection.responseCode
            val response = connection.inputStream.bufferedReader().readText()
            connection.disconnect()
            if (code == 200 && !JSONObject(response).has("error")) {
                prefs.edit().putString("tracks_pendientes", "[]").apply()
                array.length()
            } else 0
        } catch (e: Exception) { 0 }
    }

    private fun subirPendientes(
        baseUrl: String, endpoint: String,
        pendientes: List<Map<String, Any>>,
        idKey: String = "id",
        onExito: (String) -> Unit
    ): Int {
        var subidos = 0
        pendientes.forEach { registro ->
            if (subirRegistro(baseUrl, endpoint, registro)) {
                onExito(registro[idKey].toString())
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