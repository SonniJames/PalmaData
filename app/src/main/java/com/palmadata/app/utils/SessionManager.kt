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
    private const val KEY_LAST_LAT    = "last_latitude"
    private const val KEY_LAST_LON    = "last_longitude"
    private const val KEY_EQUIPO_ID   = "equipo_id"

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

    // ── Trabajador ────────────────────────────────────────────────────────────

    fun getCurrentWorker(context: Context): Worker? {
        val p = prefs(context)
        val id = p.getString(KEY_WORKER_ID, null) ?: return null
        return Worker(
            id   = id,
            name = p.getString(KEY_WORKER_NAME, "") ?: "",
            code = p.getString(KEY_WORKER_CODE, "") ?: ""
        )
    }

    fun setCurrentWorker(context: Context, worker: Worker) {
        prefs(context).edit()
            .putString(KEY_WORKER_ID,   worker.id)
            .putString(KEY_WORKER_NAME, worker.name)
            .putString(KEY_WORKER_CODE, worker.code)
            .apply()
    }

    fun clearWorker(context: Context) {
        prefs(context).edit()
            .remove(KEY_WORKER_ID)
            .remove(KEY_WORKER_NAME)
            .remove(KEY_WORKER_CODE)
            .apply()
    }

    fun hasWorker(context: Context): Boolean = getCurrentWorker(context) != null

    // ── GPS ───────────────────────────────────────────────────────────────────

    fun wasGpsPermissionRequested(context: Context): Boolean =
        prefs(context).getBoolean(KEY_GPS_REQUESTED, false)

    fun markGpsPermissionRequested(context: Context) {
        prefs(context).edit().putBoolean(KEY_GPS_REQUESTED, true).apply()
    }

    fun saveLastLocation(context: Context, lat: Double, lon: Double) {
        prefs(context).edit()
            .putFloat(KEY_LAST_LAT, lat.toFloat())
            .putFloat(KEY_LAST_LON, lon.toFloat())
            .apply()
    }

    fun getLastLatitude(context: Context): Double =
        prefs(context).getFloat(KEY_LAST_LAT, 0f).toDouble()

    fun getLastLongitude(context: Context): Double =
        prefs(context).getFloat(KEY_LAST_LON, 0f).toDouble()
}