package com.palmadata.app.estudio_tiempos

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.palmadata.app.MainActivity
import com.palmadata.app.databinding.ActivityEstudioTiemposBinding

/**
 * Módulo de ESTUDIO DE TIEMPOS.
 *
 * No captura registros propios: su única función es dejar el equipo en esta
 * pantalla durante toda la jornada, mientras el servicio de tracking sigue
 * grabando los recorridos con formulario = 30. Así los tracks del estudio de
 * tiempos quedan identificados y la medición no se interrumpe.
 *
 * El control de acceso es del dispositivo, no de la app: los equipos se
 * entregan con el módulo abierto y la pantalla bloqueada con contraseña, que
 * solo conoce el supervisor. Al final de la jornada el supervisor desbloquea
 * el teléfono y presiona FINALIZAR JORNADA para volver al grid de módulos.
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

        binding.btnFinalizar.setOnClickListener { finalizarJornada() }
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

    /** Libera el anclaje y regresa al grid de módulos, donde el formulario
     *  vuelve a 0 automáticamente. */
    private fun finalizarJornada() {
        try {
            stopLockTask()
        } catch (e: Exception) {
            android.util.Log.e("EstudioTiempos", "No se pudo liberar el anclaje: ${e.message}")
        }

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