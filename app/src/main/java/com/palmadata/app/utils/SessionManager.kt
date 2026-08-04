package com.palmadata.app.utils

import android.content.Context
import android.content.SharedPreferences
import com.palmadata.app.data.model.Worker
import java.util.UUID

object SessionManager {

    private const val PREFS_NAME      = "palma_data_session"
    private const val KEY_WORKER_ID   = "current_worker_id"
    private const val KEY_WORKER_NAME = "current_worker_name"
    private const val KEY_WORKER_CODE = "current_worker_code"
    private const val KEY_GPS_REQUESTED = "gps_permission_requested"
    private const val KEY_LAST_LAT    = "last_latitude_v2"
    private const val KEY_LAST_LON    = "last_longitude_v2"
    private const val KEY_EQUIPO_ID   = "equipo_id"

    private const val KEY_ID_EQUIPO = "id_equipo_manual"
    private const val KEY_WORKER_SUPERVISOR = "current_worker_supervisor"
    private const val KEY_MAQUINARIA_TRABAJADOR_ACTIVO = "maquinaria_trabajador_activo"

    private const val KEY_LAST_FIX_TIME = "last_fix_time"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // ── Equipo ID ─────────────────────────────────────────────────────────────

    fun getEquipoId(context: Context): String {
        val p = prefs(context)
        var id = p.getString(KEY_EQUIPO_ID, null)
        if (id == null) {
            id = UUID.randomUUID().toString()
            p.edit().putString(KEY_EQUIPO_ID, id).apply()
        }
        return id
    }

    // ── Id Equipo (asignado manualmente en el setup, viaja en cada track) ──────

    fun setIdEquipo(context: Context, idEquipo: String) {
        prefs(context).edit().putString(KEY_ID_EQUIPO, idEquipo).apply()
    }

    fun getIdEquipo(context: Context): String =
        prefs(context).getString(KEY_ID_EQUIPO, "") ?: ""

    // ── Trabajador ────────────────────────────────────────────────────────────

    fun getCurrentWorker(context: Context): Worker? {
        val p = prefs(context)
        val id = p.getString(KEY_WORKER_ID, null) ?: return null
        return Worker(
            id   = id,
            name = p.getString(KEY_WORKER_NAME, "") ?: "",
            code = p.getString(KEY_WORKER_CODE, "") ?: "",
            supervisor = p.getInt(KEY_WORKER_SUPERVISOR, 0)
        )
    }

    fun setCurrentWorker(context: Context, worker: Worker) {
        prefs(context).edit()
            .putString(KEY_WORKER_ID,   worker.id)
            .putString(KEY_WORKER_NAME, worker.name)
            .putString(KEY_WORKER_CODE, worker.code)
            .putInt(KEY_WORKER_SUPERVISOR,   worker.supervisor)
            .apply()
    }

    fun clearWorker(context: Context) {
        prefs(context).edit()
            .remove(KEY_WORKER_ID)
            .remove(KEY_WORKER_NAME)
            .remove(KEY_WORKER_CODE)
            .remove(KEY_WORKER_SUPERVISOR)
            .apply()
    }

    fun hasWorker(context: Context): Boolean = getCurrentWorker(context) != null

    // ── Trabajador activo en Maquinaria ───────────────────────────────────────

    fun setTrabajadorMaquinariaActivo(context: Context, trabajadorId: Int) {
        prefs(context).edit().putInt(KEY_MAQUINARIA_TRABAJADOR_ACTIVO, trabajadorId).apply()
    }

    fun clearTrabajadorMaquinariaActivo(context: Context) {
        prefs(context).edit().remove(KEY_MAQUINARIA_TRABAJADOR_ACTIVO).apply()
    }

    fun getTrabajadorMaquinariaActivo(context: Context): Int =
        prefs(context).getInt(KEY_MAQUINARIA_TRABAJADOR_ACTIVO, 0)

    // ── GPS ───────────────────────────────────────────────────────────────────

    fun wasGpsPermissionRequested(context: Context): Boolean =
        prefs(context).getBoolean(KEY_GPS_REQUESTED, false)

    fun markGpsPermissionRequested(context: Context) {
        prefs(context).edit().putBoolean(KEY_GPS_REQUESTED, true).apply()
    }

    fun saveLastLocation(context: Context, lat: Double, lon: Double) {
        prefs(context).edit()
            .putLong(KEY_LAST_LAT, java.lang.Double.doubleToRawLongBits(lat))
            .putLong(KEY_LAST_LON, java.lang.Double.doubleToRawLongBits(lon))
            .apply()
    }

    fun getLastLatitude(context: Context): Double =
        java.lang.Double.longBitsToDouble(prefs(context).getLong(KEY_LAST_LAT, 0L))

    fun getLastLongitude(context: Context): Double =
        java.lang.Double.longBitsToDouble(prefs(context).getLong(KEY_LAST_LON, 0L))

    fun saveLastFixTime(context: Context, fixTimeMs: Long) {
        prefs(context).edit().putLong(KEY_LAST_FIX_TIME, fixTimeMs).apply()
    }

    fun getLastFixTime(context: Context): Long =
        prefs(context).getLong(KEY_LAST_FIX_TIME, 0L)
    // ── Formulario activo (módulo en el que está el usuario) ──────────────────
    // Va a la columna `formulario` de cada track según generarFormulariosMovil:
    // 0 = pantalla principal / sin módulo, 1 = censo enfermedades, 2 = plagas,
    // 3 = trampas, 5 = polinización, 6 = strategus, 12 = tratamientos,
    // 14 = supervisión cosecha, 24 = maquinaria, 25 = fertilización.

    private const val KEY_FORMULARIO_ACTIVO = "formulario_activo"

    // ── Jornada cerrada (corte de tracking de la tarde) ───────────────────────
    // El corte de mediodía NO es una acción de una sola vez ("detener el
    // servicio"), sino un ESTADO que debe durar el resto del día: una vez el
    // trabajador sincroniza en la tarde, no se registran más tracks hasta el
    // día siguiente. Se guarda la fecha del cierre; al cambiar de día, el
    // estado deja de aplicar solo (sin necesidad de limpiarlo).

    private const val KEY_JORNADA_CERRADA_FECHA = "jornada_cerrada_fecha"

    private fun hoyStr(): String =
        java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())

    /** Marca la jornada de HOY como cerrada: no se registran más tracks hoy. */
    fun cerrarJornadaHoy(context: Context) {
        prefs(context).edit().putString(KEY_JORNADA_CERRADA_FECHA, hoyStr()).apply()
    }

    /** True solo si la jornada fue cerrada HOY (ayer ya no cuenta). */
    fun isJornadaCerradaHoy(context: Context): Boolean =
        prefs(context).getString(KEY_JORNADA_CERRADA_FECHA, "") == hoyStr()

    fun setFormularioActivo(context: Context, formularioId: Int) {
        prefs(context).edit().putInt(KEY_FORMULARIO_ACTIVO, formularioId).apply()
    }

    fun getFormularioActivo(context: Context): Int =
        prefs(context).getInt(KEY_FORMULARIO_ACTIVO, 0)

    fun clearFormularioActivo(context: Context) {
        prefs(context).edit().remove(KEY_FORMULARIO_ACTIVO).apply()
    }

    // ── Fertilizantes activos (módulo fertilización) ──────────────────────────
    // Se guardan como JSON: "[]" sin selección, "[1,2]" con varios seleccionados.

    private const val KEY_FERTILIZANTES_ACTIVOS = "fertilizantes_activos_json"

    fun setFertilizantesActivos(context: Context, ids: Collection<Int>) {
        val json = org.json.JSONArray(ids.toList()).toString()
        prefs(context).edit().putString(KEY_FERTILIZANTES_ACTIVOS, json).apply()
    }

    /** Devuelve "[]" si no hay selección, o "[1,2,...]" con los ids activos */
    fun getFertilizantesActivos(context: Context): String =
        prefs(context).getString(KEY_FERTILIZANTES_ACTIVOS, "[]") ?: "[]"

    fun clearFertilizantesActivos(context: Context) {
        prefs(context).edit().remove(KEY_FERTILIZANTES_ACTIVOS).apply()
    }
}