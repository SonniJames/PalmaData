package com.palmadata.app.utils

import android.content.Context
import android.util.Log
import com.palmadata.app.data.model.TrackMovil

object TrackStorage {

    private const val TAG = "TrackStorage"

    fun guardarTrack(context: Context, track: TrackMovil) {
        try {
            DatabaseHelper(context).guardarTrack(track)
            Log.d(TAG, "Track guardado — ${track.fecha} ${track.hora}")
        } catch (e: Exception) {
            Log.e(TAG, "Error guardando track: ${e.message}")
        }
    }

    fun contarTracks(context: Context): Int {
        return try {
            DatabaseHelper(context).contarTracksPendientes()
        } catch (e: Exception) {
            Log.e(TAG, "Error contando tracks: ${e.message}")
            0
        }
    }

    fun limpiarTracks(context: Context) {
        try {
            DatabaseHelper(context).limpiarTracks()
        } catch (e: Exception) {
            Log.e(TAG, "Error limpiando tracks: ${e.message}")
        }
    }
}