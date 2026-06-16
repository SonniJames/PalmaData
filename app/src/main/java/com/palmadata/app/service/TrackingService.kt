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
import androidx.core.app.NotificationCompat
import com.palmadata.app.MainActivity
import com.palmadata.app.R
import com.palmadata.app.utils.LocationHelper

class TrackingService : Service() {

    private lateinit var locationHelper: LocationHelper

    companion object {
        const val CHANNEL_ID = "palmadata_tracking_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "com.palmadata.app.action.START_TRACKING"
        const val ACTION_STOP  = "com.palmadata.app.action.STOP_TRACKING"
    }

    override fun onCreate() {
        super.onCreate()
        locationHelper = LocationHelper(
            context = this,
            onLocationUpdate = { _, _ -> },
            onTrackGuardado = null
        )
        crearCanalNotificacion()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                detenerServicio()
                return START_NOT_STICKY
            }
            else -> {
                iniciarComoForeground()
                if (locationHelper.hasPermissions()) {
                    locationHelper.startLocationUpdates()
                }
            }
        }
        return START_STICKY
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
        locationHelper.stopLocationUpdates()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        locationHelper.stopLocationUpdates()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}