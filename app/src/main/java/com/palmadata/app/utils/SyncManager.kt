package com.palmadata.app.utils

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import com.palmadata.app.data.model.UmaData

object SyncManager {

    data class ResultadoSync(
        val exitoso: Boolean,
        val mensaje: String = "",
        val detalles: Map<String, Int> = emptyMap()
    )

    fun sincronizar(context: Context): ResultadoSync {
        val baseUrl = ServerConfig.getBaseUrl(context)
        val db      = DatabaseHelper.getInstance(context)

        // ── Subir pendientes ──────────────────────────────────────────────────
        val subidosCenso        = subirPendientes(baseUrl, "censo_enfermedades",   db.getCensoEnfPendientes())     { id -> db.eliminarCensoEnf(id) }
        val subidosTrat         = subirPendientes(baseUrl, "tratamientos",         db.getTratamientosPendientes()) { id -> db.eliminarTratamiento(id) }
        val subidosPoli         = subirPendientes(baseUrl, "polinizacion",         db.getPolinizacionPendientes()) { id -> db.eliminarPolinizacion(id) }
        val subidosPolen        = subirPendientes(baseUrl, "polen_inicial_final",  db.getPolenPendientes())        { id -> db.eliminarPolen(id) }
        val subidosStrategus    = subirPendientes(baseUrl, "sanstrategus",         db.getStrateguspendientes())    { id -> db.eliminarStrategus(id) }
        val subidosTrampas      = subirPendientes(baseUrl, "censo_trampas",        db.getTrampasPendientes())      { id -> db.eliminarTrampa(id) }
        val subidosPlagas       = subirPendientes(baseUrl, "muestreo_plagas",      db.getPlagasPendientes())       { id -> db.eliminarPlagas(id) }
        val subidosSuperCosecha = subirPendientes(baseUrl, "super_cosecha",        db.getSuperCosechaPendientes(), idKey = "id_unico") { id -> db.eliminarSuperCosecha(id) }
        val subidosMaquinaria   = subirPendientes(baseUrl, "maquinaria_sesion",    db.getMaquinariaPendientes(),   idKey = "id_unico") { id -> db.eliminarMaquinaria(id) }
        val subidosCosechaVagon = subirPendientes(baseUrl, "super_cosecha_vagon",  db.getSuperCosechaVagonPendientes(), idKey = "id_unico") { id -> db.eliminarSuperCosechaVagon(id) }
        val subidosSuperPoli    = subirPendientes(baseUrl, "super_poli",           db.getSuperPoliPendientes(),    idKey = "id_unico") { id -> db.eliminarSuperPoli(id) }
        val subidosSuperTiempos = subirPendientes(baseUrl, "super_tiempos",        db.getSuperTiemposPendientes(), idKey = "id_unico") { id -> db.eliminarSuperTiempos(id) }
        val subidosTracks       = subirTracks(baseUrl, context)
        // ── Descargar maestros ────────────────────────────────────────────────
        // ── Descargar maestros (cada uno independiente, con 1 reintento) ──────
        val fallidos = mutableListOf<String>()

        descargar("Plantaciones", fallidos) {
            db.reemplazarPlantaciones(fetchLista(baseUrl, "plantaciones") { o -> Pair(o.getInt("id"), o.getString("nombre")) })
        }
        descargar("Trabajadores", fallidos) {
            db.reemplazarTrabajadores(fetchLista(baseUrl, "trabajadores") { o -> Triple(o.getInt("id"), o.getString("nombre"), o.optInt("supervisor", 0)) })
        }
        descargar("Sectores", fallidos) {
            db.reemplazarSectores(fetchLista(baseUrl, "sectores") { o -> Triple(o.getInt("id"), o.getString("nombre"), o.getInt("plantacion_id")) })
        }
        descargar("Lotes", fallidos) {
            db.reemplazarLotes(fetchLista(baseUrl, "lotes") { o -> Triple(o.getInt("id"), o.getString("nombre"), o.getInt("sector_id")) })
        }
        descargar("Enfermedades", fallidos) {
            db.reemplazarEnfermedades(fetchLista(baseUrl, "enfermedades") { o -> Pair(o.getInt("id"), o.getString("nombre")) })
        }
        descargar("Eventos", fallidos) {
            db.reemplazarEventos(fetchLista(baseUrl, "eventos") { o -> Triple(o.getInt("id"), o.getString("codigo"), o.getInt("enfermedad_id")) })
        }
        descargar("Trat. eventos", fallidos) {
            db.reemplazarTratamientosEventos(fetchLista(baseUrl, "tratamientos_eventos") { o -> Pair(o.getInt("id"), o.getString("codigo")) })
        }
        descargar("Trampas", fallidos) {
            db.reemplazarTrampas(fetchLista(baseUrl, "trampas") { o -> Pair(o.getInt("id"), o.getString("codigo")) })
        }
        descargar("Insectos", fallidos) {
            db.reemplazarInsectos(fetchLista(baseUrl, "insectos") { o -> Pair(o.getInt("id"), o.getString("insecto")) })
        }
        descargar("Estados insecto", fallidos) {
            db.reemplazarEstadosInsecto(fetchLista(baseUrl, "estados_insecto") { o -> Triple(o.getInt("id"), o.getString("estado"), o.getInt("insecto_id")) })
        }
        descargar("Maquinaria", fallidos) {
            db.reemplazarMaquinaria(fetchLista(baseUrl, "maquinaria") { o -> Pair(o.getInt("id"), o.getString("descripcion")) })
        }
        descargar("Implementos", fallidos) {
            db.reemplazarImplementos(fetchLista(baseUrl, "implementos") { o -> Pair(o.getInt("id"), o.getString("descripcion")) })
        }
        descargar("Labores", fallidos) {
            db.reemplazarLaboresMaquinaria(fetchLista(baseUrl, "labores_maquinaria") { o -> Pair(o.getInt("id"), o.getString("nombre")) })
        }
        descargar("Unidades", fallidos) {
            db.reemplazarUnidadesMaquinaria(fetchLista(baseUrl, "unidades_maquinaria") { o -> Pair(o.getInt("id"), o.getString("descripcion")) })
        }
        descargar("Umas", fallidos) {
            db.reemplazarUmas(fetchLista(baseUrl, "umas", readTimeout = 60_000) { o ->
                UmaData(
                    nutUmaPolId     = o.getInt("nut_uma_pol_id"),
                    nutUmaId        = o.getInt("nut_uma_id"),
                    codigo          = o.getString("codigo"),
                    palmas          = o.optInt("palmas", 0),
                    catPlantacionId = o.optInt("cat_plantacion_id", 0),
                    estado          = o.optInt("estado", 1),
                    simbolo         = o.optString("simbolo", ""),
                    geojson         = o.getString("geojson"),
                    fertilizantes   = o.optString("fertilizantes", "[]")
                )
            })
        }
        descargar("Fertilizantes", fallidos) {
            db.reemplazarFertilizantes(fetchLista(baseUrl, "fertilizantes") { o -> Pair(o.getInt("id"), o.getString("nombre")) })
        }

        descargar("Lotes mapa", fallidos) {
            db.reemplazarLotesMapa(fetchLista(baseUrl, "lotes_mapa", readTimeout = 60_000) { o ->
                com.palmadata.app.data.model.LoteMapa(
                    catLoteId = o.getInt("cat_lote_id"),
                    nombre    = o.getString("nombre"),
                    siembra   = o.optInt("siembra", 0),
                    palmas    = o.optInt("palmas", 0),
                    material  = o.optString("material", ""),
                    geojson   = o.getString("geojson")
                )
            })
        }

        // Palmas: son cientos de miles de filas (varios MB). Se consulta
        // primero una huella diminuta y solo se descargan si cambió algo. Sin
        // esto, cada sincronización arrastraría esos MB para reescribir
        // exactamente los mismos datos, y las palmas cambian muy poco.
        descargar("Palmas", fallidos) {
            val huella = fetchObjeto(baseUrl, "palmas_version")
            val version = "${huella.optInt("total", -1)}|${huella.optString("ultima", "")}"
            if (version != getVersionPalmas(context) || db.contarPalmas() == 0) {
                // Descarga en streaming: ver fetchPalmasStreaming. Con 300.000
                // filas, fetchLista (texto completo + JSONArray) superaba los
                // 200 MB de heap y tumbaba la app en equipos de gama media.
                db.reemplazarPalmas(fetchPalmasStreaming(baseUrl))
                // La versión se guarda DESPUÉS de escribir: si la descarga
                // falla a medias, la próxima sincronización lo reintenta.
                guardarVersionPalmas(context, version)
            }
        }

        descargar("Tipos parada", fallidos) {
            db.reemplazarSuperTiemposTipos(
                fetchLista(baseUrl, "super_tiempos_tipos") { o ->
                    com.palmadata.app.super_tiempos.SuperTiemposTipo(
                        codigo      = o.getString("codigo"),
                        descripcion = o.optString("descripcion", ""),
                        clase       = o.optString("clase", "improductivo"),
                        orden       = o.optInt("orden", 0)
                    )
                }
            )
        }

        guardarFechaSincronizacion(context)

        val detallesFinal = mapOf(
            "Tracks" to subidosTracks, "Censo enfermedades" to subidosCenso,
            "Tratamientos" to subidosTrat, "Polinización" to subidosPoli,
            "Polen inicial/final" to subidosPolen, "Sanstrategus" to subidosStrategus,
            "Censo trampas" to subidosTrampas, "Muestreo plagas" to subidosPlagas,
            "Super cosecha" to subidosSuperCosecha, "Maquinaria" to subidosMaquinaria,
            "Sup. cosecha vagón" to subidosCosechaVagon,
            "Sup. polinización" to subidosSuperPoli,
            "Sup. tiempos" to subidosSuperTiempos
        )

        return if (fallidos.isEmpty()) {
            ResultadoSync(exitoso = true, detalles = detallesFinal)
        } else {
            ResultadoSync(
                exitoso = false,
                mensaje = "No se pudieron descargar: ${fallidos.joinToString(", ")}. Sincroniza de nuevo para completarlos.",
                detalles = detallesFinal
            )
        }
    }

    /** Ejecuta una descarga con 1 reintento; si falla dos veces, anota y continúa. */
    private fun descargar(nombre: String, fallidos: MutableList<String>, accion: () -> Unit) {
        repeat(2) { intento ->
            try { accion(); return } catch (e: Exception) {
                if (intento == 1) fallidos.add(nombre)
            }
        }
    }

    private fun subirTracks(baseUrl: String, context: Context): Int {
        return try {
            val db = DatabaseHelper.getInstance(context)
            val pendientes = db.getTracksPendientes()
            if (pendientes.isEmpty()) return 0

            val tamanoLote  = 300
            val maxIntentos = 3
            var subidosTotal = 0
            pendientes.chunked(tamanoLote).forEach { lote ->
                var exito = false
                var intento = 0
                val ids = mutableListOf<Long>()
                lote.forEach { track -> ids.add((track["id"] as? Long) ?: 0L) }

                while (!exito && intento < maxIntentos) {
                    intento++
                    try {
                        val array = JSONArray()
                        lote.forEach { track ->
                            array.put(JSONObject(track.filter { it.key != "sincronizado" && it.key != "id" }))
                        }
                        val url = URL("$baseUrl/tracks")
                        val connection = url.openConnection() as HttpURLConnection
                        connection.connectTimeout = 15_000
                        // 120 s: insertar 300 filas en una VM pequeña puede tardar.
                        // Con el INSERT multi-fila del servidor esto será casi
                        // instantáneo, pero el margen amplio evita cortar por
                        // timeout un lote que el servidor SÍ está guardando.
                        connection.readTimeout    = 120_000
                        connection.requestMethod  = "POST"
                        connection.doOutput       = true
                        connection.setRequestProperty("Content-Type", "application/json")
                        connection.connect()
                        connection.outputStream.bufferedWriter().use { it.write(array.toString()) }
                        val code = connection.responseCode
                        val stream = if (code in 200..299) connection.inputStream else connection.errorStream
                        stream?.bufferedReader()?.readText()
                        connection.disconnect()
                        if (code == 200) {
                            db.marcarTracksSincronizados(ids)
                            subidosTotal += ids.size
                            exito = true
                        }
                    } catch (e: Exception) {
                        // Timeout u otro error de red. IMPORTANTE: el servidor
                        // pudo haber guardado el lote de todas formas (idempotencia
                        // por idunico lo protege del reenvío). No se cuenta como
                        // subido aquí; si ya estaba, el próximo intento recibe 200
                        // sin duplicar.
                    }

                    // Espera progresiva antes del siguiente intento: 1.5 s, 3 s
                    if (!exito && intento < maxIntentos) {
                        try { Thread.sleep(1_500L * intento) } catch (ie: InterruptedException) { }
                    }
                }
                // Si el lote falló los 3 intentos, queda pendiente para la próxima
                // sincronización; se continúa con el siguiente lote sin crashear.
            }
            if (subidosTotal > 0) db.eliminarTracksSincronizados()
            subidosTotal
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
        // 2 intentos por registro: los formularios pesan poco, un reintento
        // corto resuelve la mayoría de micro-cortes de WiFi.
        repeat(2) { intento ->
            try {
                val url = URL("$baseUrl/$endpoint")
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 10_000
                connection.readTimeout    = 10_000
                connection.requestMethod  = "POST"
                connection.doOutput       = true
                connection.setRequestProperty("Content-Type", "application/json")
                connection.connect()
                connection.outputStream.bufferedWriter().use { it.write(JSONObject(registro).toString()) }
                val code = connection.responseCode
                val stream = if (code in 200..299) connection.inputStream else connection.errorStream
                val response = stream?.bufferedReader()?.readText() ?: "{}"
                connection.disconnect()
                if (code == 200 && !JSONObject(response).has("error")) return true
            } catch (e: Exception) { /* se reintenta una vez */ }
            if (intento == 0) {
                try { Thread.sleep(800) } catch (ie: InterruptedException) { }
            }
        }
        return false
    }

    private fun <T> fetchLista(baseUrl: String, endpoint: String, readTimeout: Int = 30_000, mapper: (JSONObject) -> T): List<T> {
        val url = URL("$baseUrl/$endpoint")
        val connection = url.openConnection() as HttpURLConnection
        connection.connectTimeout = 10_000
        connection.readTimeout    = readTimeout
        connection.requestMethod  = "GET"
        connection.connect()
        if (connection.responseCode != 200) throw Exception("Error en /$endpoint: ${connection.responseCode}")
        val response = connection.inputStream.bufferedReader().readText()
        connection.disconnect()
        val array = JSONArray(response)
        return (0 until array.length()).map { mapper(array.getJSONObject(it)) }
    }


    /** Igual que fetchLista pero para una respuesta que es un objeto suelto,
     *  no un arreglo. Se usa para la huella del catálogo de palmas. */
    private fun fetchObjeto(baseUrl: String, endpoint: String, readTimeout: Int = 15_000): JSONObject {
        val url = URL("$baseUrl/$endpoint")
        val connection = url.openConnection() as HttpURLConnection
        connection.connectTimeout = 10_000
        connection.readTimeout    = readTimeout
        connection.requestMethod  = "GET"
        connection.connect()
        if (connection.responseCode != 200) throw Exception("Error en /$endpoint: ${connection.responseCode}")
        val response = connection.inputStream.bufferedReader().readText()
        connection.disconnect()
        return JSONObject(response)
    }

    /**
     * Descarga /palmas SIN materializar la respuesta como texto ni como
     * JSONArray: JsonReader lee del socket fila a fila y cada palma se
     * convierte directo en su objeto compacto.
     *
     * Por qué no se usa fetchLista: con 300.000 palmas, el camino
     * texto completo -> JSONArray -> lista necesitaba mas de 200 MB de heap
     * (el String en UTF-16 mas un JSONObject con su HashMap por fila) y
     * reventaba con OutOfMemoryError en equipos de gama media; como eso
     * ocurre dentro de descargar(), quedaba como "parcial: Palmas" y se
     * reintentaba en cada sincronizacion con el mismo resultado. Este camino
     * usa ~20 MB (solo la lista final) sin importar cuantas palmas haya.
     *
     * La lista se materializa completa ANTES de tocar la base a proposito:
     * si la red se corta a mitad de descarga, la excepcion salta aqui,
     * reemplazarPalmas nunca se llama y las palmas anteriores quedan
     * intactas. Ademas la transaccion de escritura no queda abierta
     * esperando al socket, momento en que el TrackingService encontraria la
     * base bloqueada.
     *
     * HttpURLConnection negocia gzip solo y lo descomprime de forma
     * transparente, asi que los ~33 MB de JSON viajan como 3-5 MB.
     */
    private fun fetchPalmasStreaming(baseUrl: String): List<com.palmadata.app.data.model.PalmaMapa> {
        val url = URL("$baseUrl/palmas")
        val connection = url.openConnection() as HttpURLConnection
        connection.connectTimeout = 10_000
        connection.readTimeout    = 120_000
        connection.requestMethod  = "GET"
        connection.connect()
        try {
            if (connection.responseCode != 200) throw Exception("Error en /palmas: ${connection.responseCode}")

            val lista = ArrayList<com.palmadata.app.data.model.PalmaMapa>(320_000)
            val reader = android.util.JsonReader(
                java.io.InputStreamReader(connection.inputStream, Charsets.UTF_8)
            )
            reader.use { r ->
                r.beginArray()
                while (r.hasNext()) {
                    var id    = 0L
                    var lote  = 0
                    var linea = 0
                    var palma = 0
                    var lat   = 0.0
                    var lon   = 0.0
                    r.beginObject()
                    while (r.hasNext()) {
                        when (r.nextName()) {
                            "cat_palma_id" -> id    = r.nextLong()
                            "cat_lote_id"  -> lote  = r.nextInt()
                            // Equivalente a optInt(..., 0): un null no revienta la descarga
                            "linea"        -> linea = leerIntONull(r)
                            "palma"        -> palma = leerIntONull(r)
                            "lat"          -> lat   = r.nextDouble()
                            "lon"          -> lon   = r.nextDouble()
                            else           -> r.skipValue()
                        }
                    }
                    r.endObject()
                    lista.add(com.palmadata.app.data.model.PalmaMapa(id, lote, linea, palma, lat, lon))
                }
                r.endArray()
            }
            return lista
        } finally {
            connection.disconnect()
        }
    }

    /** Lee un entero que puede venir como null en el JSON; null se lee como 0. */
    private fun leerIntONull(r: android.util.JsonReader): Int =
        if (r.peek() == android.util.JsonToken.NULL) {
            r.nextNull()
            0
        } else {
            r.nextInt()
        }

    private const val PREFS_SYNC    = "palma_sync"
    private const val KEY_LAST_SYNC = "ultima_sincronizacion"


    private const val KEY_VERSION_PALMAS = "version_palmas"

    private fun getVersionPalmas(context: Context): String =
        context.getSharedPreferences(PREFS_SYNC, Context.MODE_PRIVATE)
            .getString(KEY_VERSION_PALMAS, "") ?: ""

    private fun guardarVersionPalmas(context: Context, version: String) {
        context.getSharedPreferences(PREFS_SYNC, Context.MODE_PRIVATE)
            .edit().putString(KEY_VERSION_PALMAS, version).apply()
    }

    private fun guardarFechaSincronizacion(context: Context) {
        val ahora = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        context.getSharedPreferences(PREFS_SYNC, Context.MODE_PRIVATE).edit().putString(KEY_LAST_SYNC, ahora).apply()
    }

    fun getUltimaSincronizacion(context: Context): String =
        context.getSharedPreferences(PREFS_SYNC, Context.MODE_PRIVATE).getString(KEY_LAST_SYNC, "Sin sincronización aún") ?: "Sin sincronización aún"
}