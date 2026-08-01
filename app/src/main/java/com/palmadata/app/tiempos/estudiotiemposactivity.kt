package com.palmadata.app.estudio_tiempos

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.palmadata.app.MainActivity
import com.palmadata.app.databinding.ActivityEstudioTiemposBinding
import com.palmadata.app.service.TrackingService
import com.palmadata.app.utils.SessionManager

/**
 * Módulo de ESTUDIO DE TIEMPOS.
 *
 * No captura registros propios: su función es dejar el equipo en esta pantalla
 * durante toda la jornada mientras el servicio de tracking graba el recorrido
 * con formulario = 30, para que los tracks del estudio queden identificados.
 *
 * El control de acceso es del dispositivo, no de la app: los equipos se
 * entregan con el módulo abierto y la pantalla bloqueada con contraseña, que
 * solo conoce el supervisor.
 *
 * FINALIZAR JORNADA cierra el día igual que la sincronización de mediodía:
 * marca la jornada como cerrada, detiene el servicio y sale al grid. A partir
 * de ahí no se graban más tracks hoy; el estado expira solo al cambiar de
 * fecha, así que mañana al abrir la app el registro se reanuda por sí mismo.
 *
 * Complementos por si el equipo llegara a quedar desbloqueado:
 *  - Botón atrás anulado.
 *  - startLockTask(): ancla la app en pantalla.
 */
class EstudioTiemposActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEstudioTiemposBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEstudioTiemposBinding.inflate(layoutInflater)
        setContentView(binding.root)

        anclarPantalla()

        binding.btnFinalizar.setOnClickListener { confirmarFin() }
    }

    /** Ancla la app en esta pantalla. Si falla, el módulo sigue funcionando
     *  igual: el botón atrás queda bloqueado de todas formas. */
    private fun anclarPantalla() {
        try {
            startLockTask()
        } catch (e: Exception) {
            android.util.Log.e("EstudioTiempos", "No se pudo anclar la pantalla: ${e.message}")
        }
    }

    /**
     * Confirmación antes de cerrar. Finalizar detiene el registro por el resto
     * del día: si se pulsa por error a media mañana, se pierden las horas que
     * faltan. Un toque extra evita ese costo.
     */
    private fun confirmarFin() {
        AlertDialog.Builder(this)
            .setTitle("Finalizar jornada")
            .setMessage("El registro de recorrido se detendrá hasta mañana. ¿Desea continuar?")
            .setPositiveButton("Finalizar") { _, _ -> finalizarJornada() }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    /**
     * Cierra la jornada (mismo mecanismo que la sincronización de mediodía),
     * libera el anclaje y regresa al grid, donde el formulario vuelve a 0.
     */
    private fun finalizarJornada() {
        // Estado que dura el resto del día: ni onStart ni el propio servicio
        // reviven el tracking hasta mañana.
        SessionManager.cerrarJornadaHoy(this)
        stopService(Intent(this, TrackingService::class.java))

        try {
            stopLockTask()
        } catch (e: Exception) {
            android.util.Log.e("EstudioTiempos", "No se pudo liberar el anclaje: ${e.message}")
        }

        Toast.makeText(
            this,
            "Fin de jornada: el registro de recorrido se detiene y reinicia mañana al abrir la app.",
            Toast.LENGTH_LONG
        ).show()

        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        startActivity(intent)
        finish()
    }

    /**
     * El botón atrás no hace nada: la única salida del módulo es FINALIZAR
     * JORNADA.
     */
    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        // Intencionalmente vacío: no se llama a super.onBackPressed()
        Toast.makeText(this, "Use FINALIZAR JORNADA para salir", Toast.LENGTH_SHORT).show()
    }
}