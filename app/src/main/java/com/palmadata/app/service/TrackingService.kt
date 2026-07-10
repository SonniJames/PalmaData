package com.palmadata.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.palmadata.app.MainActivity
import com.palmadata.app.R
import com.palmadata.app.utils.LocationHelper
import com.palmadata.app.utils.SessionManager
import com.palmadata.app.utils.UmaDetectionEngine

class TrackingService : Service() {

    private lateinit var locationHelper: LocationHelper
    private var wakeLock: PowerManager.WakeLock? = null

    companion object {
        const val CHANNEL_ID = "palmadata_tracking_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "com.palmadata.app.action.START_TRACKING"
        const val ACTION_STOP  = "com.palmadata.app.action.STOP_TRACKING"

        // Id del módulo de fertilización según generarFormulariosMovil
        private const val FORMULARIO_FERTILIZACION = 25
    }

    override fun onCreate() {
        super.onCreate()
        locationHelper = LocationHelper(
            context = this,
            onLocationUpdate = { lat, lon -> manejarPosicion(lat, lon) },
            onTrackGuardado = null
        )
        crearCanalNotificacion()
    }

    /**
     * En cada fix GPS: si el trabajador está dentro del módulo de fertilización
     * (formulario = 25), el motor de detección de UMAs corre AQUÍ, en el
     * servicio en primer plano — así el cálculo de posición, el cambio de UMA
     * y la alerta siguen funcionando con la pantalla bloqueada.
     */
    private fun manejarPosicion(lat: Double, lon: Double) {
        val formulario = SessionManager.getFormularioActivo(this)
        if (formulario == FORMULARIO_FERTILIZACION) {
            UmaDetectionEngine.activar(this)
            UmaDetectionEngine.procesarPosicion(this, lat, lon)
        } else if (UmaDetectionEngine.activo) {
            // Salió del módulo: apagar el motor y volver a la notificación base
            UmaDetectionEngine.desactivar()
            UmaDetectionEngine.restaurarNotificacionBase(this)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                detenerServicio()
                return START_NOT_STICKY
            }
            else -> {
                iniciarComoForeground()
                adquirirWakeLock()
                if (locationHelper.hasPermissions()) {
                    locationHelper.startLocationUpdates()
                }
            }
        }
        return START_STICKY
    }

    private fun adquirirWakeLock() {
        // Si ya hay un wakelock activo no se crea otro: MainActivity reinicia el
        // servicio en cada onStart y sin esta guarda se acumularían candados
        // abiertos drenando la batería.
        if (wakeLock?.isHeld == true) return
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "PalmaData::TrackingWakeLock"
        ).also {
            it.acquire(10 * 60 * 60 * 1000L) // máximo 10 horas (toda la jornada)
        }
    }

    private fun liberarWakeLock() {
        wakeLock?.let {
            if (it.isHeld) it.release()
        }
        wakeLock = null
    }

    private fun iniciarComoForeground() {
        val notification = construirNotificacion()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun construirNotificacion(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("PalmaData")
            .setContentText("PalmaData está registrando tu ubicación")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun crearCanalNotificacion() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Seguimiento PalmaData",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notificación persistente mientras se registra la ubicación en campo"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun detenerServicio() {
        UmaDetectionEngine.desactivar()
        locationHelper.stopLocationUpdates()
        liberarWakeLock()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        UmaDetectionEngine.desactivar()
        locationHelper.stopLocationUpdates()
        liberarWakeLock()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}