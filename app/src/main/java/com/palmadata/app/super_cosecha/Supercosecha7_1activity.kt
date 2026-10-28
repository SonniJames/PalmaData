package com.palmadata.app.supercosecha

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.palmadata.app.databinding.ActivitySuperCosecha71Binding

/**
 * Pantalla 7.1 — PORCENTAJE BASURA. Mismo teclado de LÍNEA/PALMA pero con
 * punto decimal. Destino: supercosechalote.porcentajebasura, DECIMAL(5,2).
 *
 * Es OPCIONAL: si queda en blanco viaja "" y en la base queda NULL.
 * El formato se limita a lo que cabe en DECIMAL(5,2) — máximo 3 enteros y
 * 2 decimales — y además a 100 como tope por ser un porcentaje. Un valor
 * que no cupiera en la columna haría fallar el INSERT en el servidor y el
 * registro quedaría atascado en el teléfono sin poder sincronizar.
 *
 * Arrastra todos los extras acumulados (igual que super_poli) y agrega
 * "porcentaje_basura".
 */
class SuperCosecha7_1Activity : AppCompatActivity() {
    private lateinit var binding: ActivitySuperCosecha71Binding
    private var valorActual = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySuperCosecha71Binding.inflate(layoutInflater)
        setContentView(binding.root)
        actualizarDisplay()
        setupTeclado()

        binding.btnAccion.setOnClickListener {
            val valor = valorActual.toDoubleOrNull()
            when {
                // Vacío permitido → NULL en la base
                valorActual.isNotEmpty() && valor == null -> mostrarError("Valor inválido para PORCENTAJE BASURA")
                valor != null && valor > 100.0            -> mostrarError("PORCENTAJE BASURA no puede ser mayor a 100")
                else -> {
                    val extras = Bundle(intent.extras ?: Bundle())
                    extras.putString("porcentaje_basura", valorActual)
                    startActivity(Intent(this, SuperCosecha7_2Activity::class.java).putExtras(extras))
                }
            }
        }
    }

    private fun setupTeclado() {
        val botones = mapOf(binding.btn0 to "0", binding.btn1 to "1", binding.btn2 to "2", binding.btn3 to "3", binding.btn4 to "4", binding.btn5 to "5", binding.btn6 to "6", binding.btn7 to "7", binding.btn8 to "8", binding.btn9 to "9")
        botones.forEach { (btn, v) -> btn.setOnClickListener { agregarDigito(v) } }
        binding.btnPunto.setOnClickListener {
            if (!valorActual.contains(".")) {
                valorActual = if (valorActual.isEmpty()) "0." else "$valorActual."
                actualizarDisplay(); ocultarError()
            }
        }
        binding.btnC.setOnClickListener { valorActual = ""; actualizarDisplay(); ocultarError() }
        binding.btnDel.setOnClickListener { if (valorActual.isNotEmpty()) { valorActual = valorActual.dropLast(1); actualizarDisplay(); ocultarError() } }
    }

    /** Máximo 3 dígitos enteros y 2 decimales (DECIMAL(5,2)). */
    private fun agregarDigito(d: String) {
        val punto = valorActual.indexOf('.')
        val permitido = if (punto >= 0) valorActual.length - punto - 1 < 2 else valorActual.length < 3
        if (permitido) { valorActual += d; actualizarDisplay(); ocultarError() }
    }

    private fun actualizarDisplay() { binding.tvDisplay.text = valorActual }
    private fun mostrarError(msg: String) { binding.tvError.text = msg; binding.tvError.visibility = View.VISIBLE }
    private fun ocultarError() { binding.tvError.visibility = View.GONE }
}
