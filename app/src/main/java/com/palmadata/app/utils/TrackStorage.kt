package com.palmadata.app.utils

import android.content.Context
import android.util.Log
import com.palmadata.app.data.model.TrackMovil
import org.json.JSONArray
import org.json.JSONObject

/**
 * Almacenamiento temporal de tracks en SharedPreferences.
 * En Etapa 4 esto se reemplazará con SQLite + subida al DataLake.
 */
object TrackStorage {

    private const val PREFS_NAME  = "palma_tracks"
    private const val KEY_TRACKS  = "tracks_pendientes"
    private const val MAX_TRACKS  = 5000  // límite para no llenar la memoria
    private const val TAG         = "TrackStorage"

    fun guardarTrack(context: Context, track: TrackMovil) {
        try {
            val prefs  = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val actual = prefs.getString(KEY_TRACKS, "[]") ?: "[]"
            val array  = JSONArray(actual)

            // Límite de seguridad
            if (array.length() >= MAX_TRACKS) {
                Log.w(TAG, "Límite de tracks alcanzado (${MAX_TRACKS})")
                return
            }

            val obj = JSONObject().apply {
                put("x",               track.x)
                put("y",               track.y)
                put("velocidad",       track.velocidad)
                put("precision",       track.precision)
                put("sentido",         track.sentido)
                put("proveedor",       track.proveedor)
                put("fecha",           track.fecha)
                put("hora",            track.hora)
                put("trabajador",      track.trabajador)
                put("plantacion_id",   track.plantacionId)
                put("formulario",      track.formulario)
                put("idunico",         track.idunico)
                put("equipo",          track.equipo)
                put("maquina",         track.maquina)
                put("labormaquina",    track.laborMaquina)
                put("lote_id",         track.loteId)
                put("procesado",       track.procesado)
                put("sesionmaquinaria", track.sesionMaquinaria)
                put("sincronizado",    false)
            }

            array.put(obj)
            prefs.edit().putString(KEY_TRACKS, array.toString()).apply()

            Log.d(TAG, "Track guardado #${array.length()} — ${track.fecha} ${track.hora}")

        } catch (e: Exception) {
            Log.e(TAG, "Error guardando track: ${e.message}")
        }
    }

    fun contarTracks(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val actual = prefs.getString(KEY_TRACKS, "[]") ?: "[]"
        return try { JSONArray(actual).length() } catch (e: Exception) { 0 }
    }

    fun limpiarTracks(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_TRACKS, "[]").apply()
    }
}