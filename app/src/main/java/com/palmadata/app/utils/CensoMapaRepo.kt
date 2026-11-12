package com.palmadata.app.utils

import android.content.Context
import android.graphics.Color
import com.palmadata.app.data.model.CatalogoCenso
import com.palmadata.app.data.model.EventoCenso
import com.palmadata.app.data.model.MesCenso
import com.palmadata.app.data.model.PuntoCenso
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Trae del servidor los datos de la capa de censo del módulo Mapas y los
 * guarda en el teléfono.
 *
 * Por qué se guardan: el mapa se usa en campo, donde casi nunca hay conexión
 * con el servidor. La idea es abrir el mes con WiFi (en la oficina) y verlo
 * después en campo sin señal:
 *   - Con conexión: se baja SIEMPRE lo último del servidor (el censo sigue
 *     llegando durante el mes) y se guarda.
 *   - Sin conexión: se usa lo guardado, y la pantalla dice de qué fecha es.
 *
 * Se guarda como archivos JSON en la carpeta privada de la app
 * (files/censo_mapa/), uno por mes. No toca la base local, así que no hace
 * falta cambiar su versión ni migrar nada. Los meses se reemplazan al
 * volverlos a bajar; se conservan los últimos [MAX_MESES_GUARDADOS].
 *
 * Todo esto corre fuera del hilo principal: quien llama debe usar un Thread.
 */
object CensoMapaRepo {

    private const val CARPETA = "censo_mapa"
    private const val ARCHIVO_EVENTOS = "eventos.json"
    private const val MAX_MESES_GUARDADOS = 12

    // Tiempos cortos para conectar: sin red, la respuesta debe ser rápida
    // para pasar a lo guardado sin dejar al operario esperando.
    private const val CONNECT_TIMEOUT_MS = 6_000
    private const val READ_TIMEOUT_MS = 45_000

    /** Error con mensaje para mostrar tal cual en pantalla. */
    class SinDatos(mensaje: String) : Exception(mensaje)

    // ── Catálogo de eventos ───────────────────────────────────────────────────

    fun eventos(context: Context): CatalogoCenso {
        val archivo = File(carpeta(context), ARCHIVO_EVENTOS)
        val errorRed = try {
            val texto = pedir(context, "censo_mapa_eventos")
            val catalogo = leerEventos(texto, System.currentTimeMillis(), sinConexion = false)
            guardar(archivo, texto)
            return catalogo
        } catch (e: Exception) {
            e
        }
        if (archivo.exists()) {
            return leerEventos(archivo.readText(), archivo.lastModified(), sinConexion = true)
        }
        throw SinDatos(
            "No se pudo conectar con el servidor y la lista de eventos no está " +
                    "guardada en el teléfono. Conéctese a la red de la empresa y vuelva a abrir." +
                    detalle(errorRed)
        )
    }

    // ── Censo de un mes ───────────────────────────────────────────────────────

    fun mes(context: Context, anio: Int, mes: Int): MesCenso {
        val archivo = File(carpeta(context), "censo_%04d_%02d.json".format(anio, mes))
        val errorRed = try {
            val texto = pedir(context, "censo_mapa?anio=$anio&mes=$mes")
            val datos = leerMes(texto, anio, mes, System.currentTimeMillis(), sinConexion = false)
            guardar(archivo, texto)
            podar(context)
            return datos
        } catch (e: Exception) {
            e
        }
        if (archivo.exists()) {
            return leerMes(archivo.readText(), anio, mes, archivo.lastModified(), sinConexion = true)
        }
        throw SinDatos(
            "No se pudo conectar con el servidor y este mes no está guardado en el " +
                    "teléfono. Ábralo una vez con conexión a la red de la empresa para " +
                    "poder verlo después en campo." + detalle(errorRed)
        )
    }

    // ── Precarga en la sincronización ─────────────────────────────────────────

    /** Cuántos meses se dejan en el teléfono en cada sincronización: el actual y el anterior. */
    const val MESES_PRECARGA = 2

    /**
     * Se llama al sincronizar: baja la lista de eventos y los últimos
     * [MESES_PRECARGA] meses COMPLETOS (hoy 12 de noviembre: todo octubre y
     * noviembre hasta hoy) y los deja guardados, para que el mapa los tenga
     * en campo sin haberlos abierto antes.
     *
     * A diferencia de [eventos] y [mes], aquí NO se usa lo guardado si falla
     * la red: se lanza el error para que la sincronización lo reporte.
     * Devuelve cuántas palmas quedaron guardadas en total.
     */
    fun precargar(context: Context): Int {
        val textoEventos = pedir(context, "censo_mapa_eventos")
        leerEventos(textoEventos, System.currentTimeMillis(), sinConexion = false)   // valida el JSON
        guardar(File(carpeta(context), ARCHIVO_EVENTOS), textoEventos)

        var total = 0
        val cal = java.util.Calendar.getInstance()
        repeat(MESES_PRECARGA) {
            val anio = cal.get(java.util.Calendar.YEAR)
            val mes = cal.get(java.util.Calendar.MONTH) + 1
            val texto = pedir(context, "censo_mapa?anio=$anio&mes=$mes")
            total += leerMes(texto, anio, mes, System.currentTimeMillis(), sinConexion = false).puntos.size
            guardar(File(carpeta(context), "censo_%04d_%02d.json".format(anio, mes)), texto)
            cal.add(java.util.Calendar.MONTH, -1)
        }
        podar(context)
        return total
    }

    // ── Red ───────────────────────────────────────────────────────────────────

    private fun pedir(context: Context, ruta: String): String {
        val url = URL("${ServerConfig.getBaseUrl(context)}/$ruta")
        val con = url.openConnection() as HttpURLConnection
        try {
            con.connectTimeout = CONNECT_TIMEOUT_MS
            con.readTimeout = READ_TIMEOUT_MS
            con.requestMethod = "GET"
            // HttpURLConnection pide gzip y lo descomprime solo: el servidor
            // ya tiene GZipMiddleware, así que un mes viaja comprimido.
            val codigo = con.responseCode
            if (codigo != HttpURLConnection.HTTP_OK) {
                val cuerpo = try { con.errorStream?.bufferedReader()?.readText() ?: "" } catch (e: Exception) { "" }
                throw Exception("El servidor respondió $codigo ${cuerpo.take(200)}")
            }
            return con.inputStream.bufferedReader(Charsets.UTF_8).readText()
        } finally {
            con.disconnect()
        }
    }

    private fun detalle(e: Exception): String =
        if (e.message.isNullOrBlank()) "" else "\n\n(${e.message})"

    // ── Lectura del JSON ──────────────────────────────────────────────────────

    private fun leerEventos(texto: String, guardadoMs: Long, sinConexion: Boolean): CatalogoCenso {
        val o = JSONObject(texto)
        val arr = o.getJSONArray("eventos")
        val lista = ArrayList<EventoCenso>(arr.length())
        for (i in 0 until arr.length()) {
            val e = arr.getJSONObject(i)
            lista.add(
                EventoCenso(
                    id = e.getInt("id"),
                    codigo = e.optString("codigo", ""),
                    enfermedad = e.optString("enfermedad", ""),
                    color = colorSeguro(e.optString("color", ""))
                )
            )
        }
        return CatalogoCenso(
            eventos = lista,
            desde = textoONull(o, "desde"),
            hasta = textoONull(o, "hasta"),
            guardadoMs = guardadoMs,
            sinConexion = sinConexion
        )
    }

    private fun leerMes(texto: String, anio: Int, mes: Int, guardadoMs: Long, sinConexion: Boolean): MesCenso {
        val o = JSONObject(texto)
        val arr = o.getJSONArray("filas")
        val lista = ArrayList<PuntoCenso>(arr.length())
        for (i in 0 until arr.length()) {
            val f = arr.getJSONObject(i)
            lista.add(
                PuntoCenso(
                    id = f.optLong("id", 0L),
                    fecha = f.optString("fecha", ""),
                    hora = f.optString("hora", ""),
                    lote = f.optString("lote", ""),
                    linea = f.optInt("linea", 0),
                    palma = f.optInt("palma", 0),
                    enfermedad = f.optString("enfermedad", ""),
                    eventoId = f.optInt("evento_id", 0),
                    trabajador = f.optString("trabajador", ""),
                    lat = if (f.isNull("lat")) null else f.optDouble("lat"),
                    lon = if (f.isNull("lon")) null else f.optDouble("lon"),
                    registros = f.optInt("registros", 1)
                )
            )
        }
        return MesCenso(anio, mes, lista, guardadoMs, sinConexion)
    }

    private fun textoONull(o: JSONObject, clave: String): String? =
        if (o.isNull(clave)) null else o.optString(clave, "").ifBlank { null }

    /** "#rrggbb" a entero; gris si viene vacío o mal escrito. */
    private fun colorSeguro(texto: String): Int =
        try { Color.parseColor(texto) } catch (e: Exception) { Color.rgb(85, 85, 85) }

    // ── Archivos ──────────────────────────────────────────────────────────────

    private fun carpeta(context: Context): File =
        File(context.filesDir, CARPETA).apply { if (!exists()) mkdirs() }

    /** Escribe a un temporal y lo renombra: un corte a mitad no deja un JSON roto. */
    private fun guardar(archivo: File, texto: String) {
        try {
            val tmp = File(archivo.parentFile, archivo.name + ".tmp")
            tmp.writeText(texto)
            if (archivo.exists()) archivo.delete()
            tmp.renameTo(archivo)
        } catch (e: Exception) {
            // Si no se pudo guardar, el mapa se ve igual: solo no quedará
            // disponible sin conexión.
            android.util.Log.w("CensoMapaRepo", "No se guardó ${archivo.name}: ${e.message}")
        }
    }

    /** Deja solo los meses abiertos más recientemente. */
    private fun podar(context: Context) {
        try {
            val meses = carpeta(context).listFiles { f -> f.name.startsWith("censo_") && f.name.endsWith(".json") }
                ?: return
            meses.sortedByDescending { it.lastModified() }
                .drop(MAX_MESES_GUARDADOS)
                .forEach { it.delete() }
        } catch (e: Exception) { /* no es grave */ }
    }
}
