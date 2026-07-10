package com.palmadata.app.utils

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import androidx.core.app.NotificationCompat
import com.palmadata.app.MainActivity
import com.palmadata.app.R
import com.palmadata.app.data.model.UmaData
import org.json.JSONArray

/**
 * Motor de detección de UMAs para el módulo de fertilización.
 *
 * Vive fuera de cualquier pantalla: lo alimenta el TrackingService (que corre
 * en primer plano con wakelock), por lo que la detección, la alerta de cambio
 * de UMA y la notificación con la dosis SIGUEN FUNCIONANDO con la pantalla
 * bloqueada. La pantalla del mapa solo se registra como oyente para pintar.
 *
 * Se activa mientras el formulario activo sea 25 (fertilización) y se
 * desactiva automáticamente cuando el usuario sale del módulo.
 */
object UmaDetectionEngine {

    private const val TAG = "UmaDetectionEngine"
    private const val FIXES_PARA_CAMBIO = 3

    // Deben coincidir con TrackingService para actualizar SU notificación
    private const val CHANNEL_ID      = "palmadata_tracking_channel"
    private const val NOTIFICATION_ID = 1001

    interface Listener {
        /** Los polígonos terminaron de cargarse (puede llegar en hilo secundario) */
        fun onUmasCargadas()
        /** Cambió la UMA actual (puede llegar en hilo secundario) */
        fun onUmaCambiada(uma: UmaData?)
    }

    @Volatile var poligonos: List<UmaPoligono> = emptyList()
        private set
    @Volatile var activo = false
        private set
    @Volatile private var cargando = false
    @Volatile private var listener: Listener? = null

    // Histéresis (misma lógica que tenía la pantalla del mapa)
    private var umaActualId: Int? = null
    private var umaCandidataId: Int? = null
    private var fixesCandidata = 0

    fun setListener(l: Listener?) { listener = l }

    fun umaActual(): UmaData? =
        poligonos.firstOrNull { it.uma.nutUmaPolId == umaActualId }?.uma

    /** Activa el motor y dispara la carga de polígonos si aún no están en memoria */
    fun activar(context: Context) {
        if (!activo) {
            activo = true
            Log.d(TAG, "Motor de detección activado")
        }
        cargarPoligonosSiFaltan(context.applicationContext)
    }

    /** Desactiva el motor al salir del módulo. Los polígonos quedan en memoria
     *  para que la próxima entrada al módulo sea instantánea. */
    fun desactivar() {
        if (!activo) return
        activo = false
        umaActualId = null
        umaCandidataId = null
        fixesCandidata = 0
        Log.d(TAG, "Motor de detección desactivado")
    }

    private fun cargarPoligonosSiFaltan(appContext: Context) {
        if (poligonos.isNotEmpty() || cargando) return
        cargando = true
        Thread {
            try {
                val cargados = DatabaseHelper.getInstance(appContext).getUmas()
                    .mapNotNull { uma ->
                        try { UmaPoligono(uma) } catch (e: Exception) { null } // geojson inválido: se omite
                    }
                poligonos = cargados
                Log.d(TAG, "Polígonos cargados: ${cargados.size}")
                listener?.onUmasCargadas()
            } catch (e: Exception) {
                Log.e(TAG, "Error cargando umas: ${e.message}")
            } finally {
                cargando = false
            }
        }.start()
    }

    /** Procesa un fix GPS. Lo llama el TrackingService en cada actualización. */
    @Synchronized
    fun procesarPosicion(context: Context, lat: Double, lon: Double) {
        if (!activo || poligonos.isEmpty()) return

        val detectada = UmaLocator.umaEn(poligonos, lat, lon)?.uma?.nutUmaPolId

        if (detectada == umaActualId) {
            umaCandidataId = null; fixesCandidata = 0; return
        }

        if (detectada == umaCandidataId) fixesCandidata++
        else { umaCandidataId = detectada; fixesCandidata = 1 }

        if (fixesCandidata >= FIXES_PARA_CAMBIO) {
            val anterior   = umaActualId
            umaActualId    = umaCandidataId
            umaCandidataId = null
            fixesCandidata = 0

            val uma = umaActual()
            val alertar = anterior != null || umaActualId != null
            if (alertar) alertar(context.applicationContext)
            actualizarNotificacion(context.applicationContext, uma)
            listener?.onUmaCambiada(uma)
        }
    }

    // ── Alerta (vibración + sonido): funciona con pantalla bloqueada ──────────

    @Suppress("DEPRECATION")
    private fun alertar(appContext: Context) {
        try {
            val v = appContext.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            v.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 400, 200, 400), -1))
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            RingtoneManager.getRingtone(appContext, uri)?.play()
        } catch (e: Exception) {
            Log.e(TAG, "Error alertando: ${e.message}")
        }
    }

    // ── Notificación: UMA actual + dosis visible en pantalla de bloqueo ───────

    private fun actualizarNotificacion(appContext: Context, uma: UmaData?) {
        try {
            val titulo = if (uma != null) "📍 UMA ${uma.codigo}" else "Fuera de las umas"
            val texto  = if (uma != null) textoDosis(appContext, uma) else "Sin UMA detectada"

            val pendingIntent = PendingIntent.getActivity(
                appContext, 0,
                Intent(appContext, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
                .setContentTitle(titulo)
                .setContentText(texto)
                .setStyle(NotificationCompat.BigTextStyle().bigText(texto))
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build()

            val nm = appContext.getSystemService(NotificationManager::class.java)
            nm.notify(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            Log.e(TAG, "Error actualizando notificación: ${e.message}")
        }
    }

    /** Arma el texto de dosis de los fertilizantes SELECCIONADOS para esta uma */
    private fun textoDosis(appContext: Context, uma: UmaData): String {
        return try {
            val seleccionadosJson = SessionManager.getFertilizantesActivos(appContext)
            val seleccionados = mutableSetOf<Int>()
            val selArr = JSONArray(seleccionadosJson)
            for (i in 0 until selArr.length()) seleccionados.add(selArr.getInt(i))

            if (seleccionados.isEmpty()) return "⚠ Seleccione fertilizante"

            val jsonArray = JSONArray(uma.fertilizantes)
            val sb = StringBuilder()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                if (!seleccionados.contains(obj.getInt("id"))) continue
                if (sb.isNotEmpty()) sb.append("\n")
                sb.append("${obj.getString("nombre")}: dosis ${obj.getDouble("dosis")} (rondas ${obj.getInt("rondas")})")
            }
            if (sb.isEmpty()) "Este fertilizante no aplica en esta UMA" else sb.toString()
        } catch (e: Exception) {
            "Sin información de fertilización"
        }
    }

    /** Restaura la notificación base del servicio (la llama el TrackingService
     *  cuando el usuario sale del módulo de fertilización). */
    fun restaurarNotificacionBase(appContext: Context) {
        try {
            val pendingIntent = PendingIntent.getActivity(
                appContext, 0,
                Intent(appContext, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE
            )
            val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
                .setContentTitle("PalmaData")
                .setContentText("PalmaData está registrando tu ubicación")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build()
            val nm = appContext.getSystemService(NotificationManager::class.java)
            nm.notify(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            Log.e(TAG, "Error restaurando notificación: ${e.message}")
        }
    }
}
