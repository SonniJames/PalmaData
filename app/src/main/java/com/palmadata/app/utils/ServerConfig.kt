package com.palmadata.app.utils

import android.content.Context

/**
 * Guarda y recupera la configuración del servidor (IP y puerto).
 * Se configura una sola vez al instalar la app.
 */
object ServerConfig {

    private const val PREFS_NAME  = "palma_server_config"
    private const val KEY_IP      = "server_ip"
    private const val KEY_PUERTO  = "server_puerto"
    private const val KEY_CONFIGURADO = "servidor_configurado"

    /** Guarda la IP y puerto del servidor */
    fun guardar(context: Context, ip: String, puerto: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_IP, ip.trim())
            .putString(KEY_PUERTO, puerto.trim())
            .putBoolean(KEY_CONFIGURADO, true)
            .apply()
    }

    /** Devuelve la IP guardada */
    fun getIp(context: Context): String =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_IP, "") ?: ""

    /** Devuelve el puerto guardado */
    fun getPuerto(context: Context): String =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_PUERTO, "8080") ?: "8080"

    /** Devuelve la URL base completa: http://IP:PUERTO */
    fun getBaseUrl(context: Context): String {
        val ip     = getIp(context)
        val puerto = getPuerto(context)
        return "http://$ip:$puerto"
    }

    /** True si ya se configuró el servidor (no es primer inicio) */
    fun estaConfigurado(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_CONFIGURADO, false)

    /** Resetea la configuración (útil para pruebas) */
    fun resetear(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .clear().apply()
    }
}