package com.palmadata.app.utils

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Exporta los registros pendientes a archivos .xlsx en Descargas/PalmaData,
 * para plantaciones donde el celular NUNCA tiene red y los datos llegan a la
 * base por un pipeline que lee esos archivos.
 *
 * Es el espejo de SyncManager: recorre los mismos pendientes, y tras escribir
 * cada archivo borra los registros locales igual que hace la sincronización
 * tras un 200. Por eso son excluyentes: si se sincronizó no hay nada que
 * descargar, y si se descargó no hay nada que sincronizar.
 *
 * ── Contrato del archivo ────────────────────────────────────────────────────
 * Nombre:   <tabla_postgres>_<yyyyMMdd>_<n>.xlsx, con n = número de descarga
 *           del día en ese equipo (1 la primera, 2 si vuelven a registrar y
 *           descargar el mismo día...). Todos los archivos de una misma
 *           descarga comparten n.
 * Hoja:     una, llamada como la tabla.
 * Columnas: EXACTAMENTE las que la app envía al servidor en el JSON de cada
 *           endpoint (las que reciben los modelos Pydantic), en el mismo
 *           orden. Se excluye siempre `sincronizado` (estado local) y, en
 *           tracks y polen, el `id` local autoincremental que el servidor
 *           ignora. En los demás módulos `id` SE CONSERVA: es el UUID con el
 *           que el servidor hace ON CONFLICT, y el pipeline debe usarlo igual.
 *
 * ── Garantías ───────────────────────────────────────────────────────────────
 * Cada módulo es atómico: primero se escribe y confirma el archivo, y SOLO
 * después se borran sus registros. Si falla la escritura, los registros
 * siguen en el teléfono y el resultado sale como parcial. Nunca se borra
 * nada que no haya quedado en disco.
 *
 * ── Permisos ────────────────────────────────────────────────────────────────
 * Android 10+ (API 29): MediaStore.Downloads, sin permiso alguno.
 * Android 8 y 9:        carpeta pública de Descargas, requiere
 *                       WRITE_EXTERNAL_STORAGE (lo pide MainActivity).
 */
object ExportManager {

    data class ResultadoExport(
        val exitoso: Boolean,
        val mensaje: String = "",
        /** tabla -> registros exportados (solo módulos con datos) */
        val detalles: Map<String, Int> = emptyMap(),
        val archivos: List<String> = emptyList(),
        /** Número de descarga del día que llevan estos archivos */
        val secuencia: Int = 0
    )

    /** Subcarpeta dentro de Descargas donde quedan los archivos. */
    const val CARPETA = "PalmaData"
    const val MIME_XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"

    private const val PREFS_EXPORT   = "palma_export"
    private const val KEY_SEQ_PREFIX = "seq_"   // seq_yyyyMMdd -> último n usado ese día

    /**
     * Un módulo exportable. `tabla` es el nombre real de la tabla en Postgres
     * (mismo destino del INSERT de su endpoint), y es el nombre del archivo.
     */
    private class Modulo(
        val tabla: String,
        val pendientes: () -> List<Map<String, Any>>,
        /** columna del mapa con la clave para borrar el registro tras exportar */
        val idKey: String,
        val eliminar: (String) -> Unit,
        /** columnas que NO van al archivo (además de `sincronizado`) */
        val excluir: Set<String> = emptySet()
    )

    fun exportar(context: Context): ResultadoExport {
        val db = DatabaseHelper.getInstance(context)
        val fecha = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
        val secuencia = siguienteSecuencia(context, fecha)

        // Misma lista y mismo orden que la subida de SyncManager.sincronizar().
        // Si se agrega un módulo allá, se agrega aquí: si no, sus registros
        // nunca saldrían del teléfono en una plantación sin red.
        val modulos = listOf(
            Modulo("san_enf_lectura",                { db.getCensoEnfPendientes() },          "id",       { db.eliminarCensoEnf(it) }),
            Modulo("san_enf_tratamiento",            { db.getTratamientosPendientes() },      "id",       { db.eliminarTratamiento(it) }),
            Modulo("propolinizacion",                { db.getPolinizacionPendientes() },      "id",       { db.eliminarPolinizacion(it) }),
            Modulo("propoleninicialfinal",           { db.getPolenPendientes() },             "id",       { db.eliminarPolen(it) }, excluir = setOf("id")),
            Modulo("sanstrategus",                   { db.getStrateguspendientes() },         "id",       { db.eliminarStrategus(it) }),
            Modulo("santrampalectura",               { db.getTrampasPendientes() },           "id",       { db.eliminarTrampa(it) }),
            Modulo("sanplagaslectura",               { db.getPlagasPendientes() },            "id",       { db.eliminarPlagas(it) }),
            Modulo("supercosechalote",               { db.getSuperCosechaPendientes() },      "id_unico", { db.eliminarSuperCosecha(it) }),
            Modulo("sesionesmaquinaria",             { db.getMaquinariaPendientes() },        "id_unico", { db.eliminarMaquinaria(it) }),
            Modulo("supercosechavagon",              { db.getSuperCosechaVagonPendientes() }, "id_unico", { db.eliminarSuperCosechaVagon(it) }),
            Modulo("pro_ordenes_super_poli_detalle", { db.getSuperPoliPendientes() },         "id_unico", { db.eliminarSuperPoli(it) }),
            Modulo("super_tiempos",                  { db.getSuperTiemposPendientes() },      "id_unico", { db.eliminarSuperTiempos(it) })
        )

        val detalles = linkedMapOf<String, Int>()
        val archivos = mutableListOf<String>()
        val fallidos = mutableListOf<String>()

        for (m in modulos) {
            try {
                val filas = m.pendientes()
                if (filas.isEmpty()) continue
                val nombre = nombreArchivo(m.tabla, fecha, secuencia)
                val columnas = columnasDe(filas, m.excluir)
                escribirArchivo(context, nombre) { XlsxWriter.escribir(it, m.tabla, columnas, filas) }
                // Archivo confirmado en disco: ahora sí se borran los registros
                filas.forEach { fila -> m.eliminar(fila[m.idKey].toString()) }
                detalles[m.tabla] = filas.size
                archivos.add(nombre)
            } catch (e: Exception) {
                android.util.Log.e("ExportManager", "Falló ${m.tabla}: ${e.message}", e)
                fallidos.add(m.tabla)
            }
        }

        // Tracks: se exportan siempre que haya. Usan marcar + eliminar en dos
        // pasos, igual que la subida, porque el servicio de tracking puede
        // estar escribiendo tracks nuevos mientras se genera el archivo: solo
        // se borran los que quedaron en el archivo, nunca los que llegaron
        // después.
        try {
            val tracks = db.getTracksPendientes()
            if (tracks.isNotEmpty()) {
                val nombre = nombreArchivo("tracksmoviltemp", fecha, secuencia)
                val columnas = columnasDe(tracks, setOf("id"))
                escribirArchivo(context, nombre) { XlsxWriter.escribir(it, "tracksmoviltemp", columnas, tracks) }
                val ids = tracks.map { (it["id"] as? Long) ?: 0L }
                db.marcarTracksSincronizados(ids)
                db.eliminarTracksSincronizados()
                detalles["tracksmoviltemp"] = tracks.size
                archivos.add(nombre)
            } else {
                detalles["tracksmoviltemp"] = 0
            }
        } catch (e: Exception) {
            android.util.Log.e("ExportManager", "Falló tracks: ${e.message}", e)
            fallidos.add("tracksmoviltemp")
        }

        // El número del día solo avanza si algo quedó escrito: una descarga
        // sin registros no debe "gastar" un número.
        if (archivos.isNotEmpty()) guardarSecuencia(context, fecha, secuencia)

        return if (fallidos.isEmpty()) {
            ResultadoExport(
                exitoso   = true,
                mensaje   = if (archivos.isEmpty()) "No había registros pendientes para descargar."
                else "Archivos guardados en Descargas/$CARPETA",
                detalles  = detalles,
                archivos  = archivos,
                secuencia = secuencia
            )
        } else {
            ResultadoExport(
                exitoso   = false,
                mensaje   = "No se pudieron generar: ${fallidos.joinToString(", ")}. " +
                        "Esos registros siguen en el teléfono; intente de nuevo.",
                detalles  = detalles,
                archivos  = archivos,
                secuencia = secuencia
            )
        }
    }

    // ── Nombres y columnas ───────────────────────────────────────────────────

    private fun nombreArchivo(tabla: String, fecha: String, secuencia: Int) =
        "${tabla}_${fecha}_$secuencia.xlsx"

    /**
     * Columnas en el orden del mapa (= orden de las columnas en SQLite, que es
     * el mismo con que viajan en el JSON). Se toman de la primera fila: todas
     * las filas de una tabla traen las mismas claves.
     */
    private fun columnasDe(filas: List<Map<String, Any>>, excluir: Set<String>): List<String> =
        filas.first().keys.filter { it != "sincronizado" && it !in excluir }

    // ── Secuencia del día ────────────────────────────────────────────────────

    private fun siguienteSecuencia(context: Context, fecha: String): Int =
        context.getSharedPreferences(PREFS_EXPORT, Context.MODE_PRIVATE)
            .getInt(KEY_SEQ_PREFIX + fecha, 0) + 1

    private fun guardarSecuencia(context: Context, fecha: String, secuencia: Int) {
        context.getSharedPreferences(PREFS_EXPORT, Context.MODE_PRIVATE)
            .edit().putInt(KEY_SEQ_PREFIX + fecha, secuencia).apply()
    }

    // ── Escritura en Descargas/PalmaData ─────────────────────────────────────

    /**
     * Abre el destino, entrega el OutputStream a `escritor` y confirma el
     * archivo. Si `escritor` lanza, el archivo a medias se elimina y la
     * excepción sigue hacia arriba (el módulo queda como fallido).
     */
    private fun escribirArchivo(context: Context, nombre: String, escritor: (OutputStream) -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            escribirMediaStore(context, nombre, escritor)
        } else {
            escribirArchivoClasico(nombre, escritor)
        }
    }

    /** Android 10+: sin permisos, la entrada aparece en la app de Archivos al instante. */
    private fun escribirMediaStore(context: Context, nombre: String, escritor: (OutputStream) -> Unit) {
        val resolver = context.contentResolver
        val valores = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, nombre)
            put(MediaStore.Downloads.MIME_TYPE, MIME_XLSX)
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/" + CARPETA)
            // Pendiente mientras se escribe: otras apps no lo ven a medias
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val uri: Uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, valores)
            ?: throw IllegalStateException("MediaStore no devolvió URI para $nombre")
        try {
            resolver.openOutputStream(uri)?.use { escritor(it) }
                ?: throw IllegalStateException("No se pudo abrir el flujo de salida de $nombre")
            resolver.update(uri, ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }, null, null)
        } catch (e: Exception) {
            try { resolver.delete(uri, null, null) } catch (_: Exception) { }
            throw e
        }
    }

    /** Android 8 y 9: carpeta pública clásica. Requiere WRITE_EXTERNAL_STORAGE. */
    private fun escribirArchivoClasico(nombre: String, escritor: (OutputStream) -> Unit) {
        @Suppress("DEPRECATION")
        val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), CARPETA)
        if (!dir.exists() && !dir.mkdirs()) throw IllegalStateException("No se pudo crear ${dir.absolutePath}")
        val archivo = File(dir, nombre)
        try {
            FileOutputStream(archivo).use { fos ->
                escritor(fos)   // deja el ZIP terminado pero el archivo abierto
                fos.flush()
                fos.fd.sync()   // en disco de verdad antes de borrar registros
            }
        } catch (e: Exception) {
            archivo.delete()
            throw e
        }
    }
}