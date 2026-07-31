package com.palmadata.app.estudio_tiempos

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.palmadata.app.MainActivity
import com.palmadata.app.databinding.ActivityEstudioTiemposBinding

/**
 * Módulo de ESTUDIO DE TIEMPOS.
 *
 * No captura registros propios: su única función es dejar el equipo anclado en
 * esta pantalla durante toda la jornada, mientras el servicio de tracking sigue
 * grabando los recorridos con formulario = 30. Así los tracks del estudio de
 * tiempos quedan claramente identificados y el operario no puede salirse de la
 * app por accidente ni interrumpir la medición.
 *
 * Para salir hay que presionar FINALIZAR JORNADA e ingresar la contraseña de
 * supervisor. En ese momento el formulario vuelve a 0 (lo hace MainActivity al
 * volver al grid de módulos).
 *
 * Bloqueo de salida, en dos capas:
 *  - Botón atrás: anulado por completo (onBackPressed vacío).
 *  - Home y multitarea: startLockTask(). Si la app NO es Device Owner, Android
 *    activa el "anclaje de pantalla": se sale manteniendo Atrás + Multitarea a
 *    la vez. Si algún día se configura como Device Owner, este mismo código
 *    bloquea de forma hermética sin cambiar nada.
 */
class EstudioTiemposActivity : AppCompatActivity() {

    companion object {
        // Contraseña de supervisor para finalizar la jornada.
        // Cámbiala por la definitiva antes de compilar el APK de producción.
        private const val PASSWORD_SUPERVISOR = "p41m3r45_*"
    }

    private lateinit var binding: ActivityEstudioTiemposBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEstudioTiemposBinding.inflate(layoutInflater)
        setContentView(binding.root)

        anclarPantalla()

        binding.btnFinalizar.setOnClickListener { pedirContrasena() }
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

    private fun pedirContrasena() {
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            hint = "Contraseña"
            setPadding(48, 32, 48, 32)
        }

        AlertDialog.Builder(this)
            .setTitle("Finalizar jornada")
            .setMessage("Ingrese la contraseña de supervisor para salir del módulo.")
            .setView(input)
            .setPositiveButton("Finalizar") { _, _ ->
                if (input.text.toString() == PASSWORD_SUPERVISOR) {
                    finalizarJornada()
                } else {
                    Toast.makeText(this, "❌ Contraseña incorrecta", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .setCancelable(false)
            .show()
    }

    /** Libera el anclaje y regresa al grid de módulos. */
    private fun finalizarJornada() {
        try {
            stopLockTask()
        } catch (e: Exception) {
            android.util.Log.e("EstudioTiempos", "No se pudo liberar el anclaje: ${e.message}")
        }
        // Ocultar el teclado si quedó abierto tras escribir la contraseña
        try {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            imm.hideSoftInputFromWindow(binding.root.windowToken, 0)
        } catch (e: Exception) { /* sin importancia */ }

        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        startActivity(intent)
        finish()
    }

    /**
     * El botón atrás no hace nada: la única salida del módulo es FINALIZAR
     * JORNADA con la contraseña.
     */
    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        // Intencionalmente vacío: no se llama a super.onBackPressed()
        Toast.makeText(this, "Use FINALIZAR JORNADA para salir", Toast.LENGTH_SHORT).show()
    }
}