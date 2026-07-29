package com.palmadata.app.super_poli

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.palmadata.app.databinding.ActivitySuperPoliTecladoBinding

/**
 * Pantallas 7 a 11 en una sola clase, encadenadas por el extra "paso".
 * Todas son el mismo teclado numérico sin restricciones (pueden quedar en
 * blanco → 0 al guardar); solo cambian el título, el texto del botón y la
 * columna destino.
 *
 * Cada paso se apila como una instancia independiente, así el botón atrás
 * recorre las pantallas una por una igual que si fueran clases distintas.
 */
class SuperPoliValorActivity : AppCompatActivity() {

    companion object {
        const val PASO_HOJA_SIN_MARCAR   = 1
        const val PASO_ESPATA_SIN_ABRIR  = 2
        const val PASO_ESPATA_ABIERTA    = 3
        const val PASO_ESPATA_PARCIAL    = 4
        const val PASO_MALA_COBERTURA    = 5

        /** titulo, texto del botón que lleva al siguiente, clave del extra */
        private val PASOS = mapOf(
            PASO_HOJA_SIN_MARCAR  to Triple("HOJA SIN MARCAR",          "BRÁCTEA SIN ABRIR",        "hoja_sin_marcar"),
            PASO_ESPATA_SIN_ABRIR to Triple("BRÁCTEA SIN ABRIR",        "BRÁCTEA ABIERTA",          "espata_sin_abrir"),
            PASO_ESPATA_ABIERTA   to Triple("BRÁCTEA ABIERTA",          "BRÁCTEA PARCIAL ABIERTA",  "espata_abierta"),
            PASO_ESPATA_PARCIAL   to Triple("BRÁCTEA PARCIAL ABIERTA",  "MALA COBERTURA APLICACIÓN","espata_parcial"),
            PASO_MALA_COBERTURA   to Triple("MALA COBERTURA APLICACIÓN","POLINIZADOR",              "mala_cobertura_aplicacion")
        )
    }

    private lateinit var binding: ActivitySuperPoliTecladoBinding
    private var valorActual = ""
    private var paso = PASO_HOJA_SIN_MARCAR

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySuperPoliTecladoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        paso = intent.getIntExtra("paso", PASO_HOJA_SIN_MARCAR)
        val (titulo, textoBoton, claveExtra) = PASOS[paso] ?: PASOS[PASO_HOJA_SIN_MARCAR]!!

        binding.tvTitulo.text  = titulo
        binding.btnAccion.text = textoBoton

        setupTeclado()

        binding.btnAccion.setOnClickListener {
            // Se arrastran todos los extras acumulados y se agrega el de este paso
            val extras = Bundle(intent.extras ?: Bundle())
            extras.putString(claveExtra, valorActual)

            if (paso < PASO_MALA_COBERTURA) {
                extras.putInt("paso", paso + 1)
                startActivity(Intent(this, SuperPoliValorActivity::class.java).putExtras(extras))
            } else {
                startActivity(Intent(this, SuperPoli12Activity::class.java).putExtras(extras))
            }
        }
    }

    private fun setupTeclado() {
        val botones = mapOf(
            binding.btn0 to "0", binding.btn1 to "1", binding.btn2 to "2", binding.btn3 to "3",
            binding.btn4 to "4", binding.btn5 to "5", binding.btn6 to "6", binding.btn7 to "7",
            binding.btn8 to "8", binding.btn9 to "9"
        )
        botones.forEach { (btn, v) ->
            btn.setOnClickListener { if (valorActual.length < 6) { valorActual += v; actualizarDisplay() } }
        }
        binding.btnC.setOnClickListener { valorActual = ""; actualizarDisplay() }
        binding.btnDel.setOnClickListener {
            if (valorActual.isNotEmpty()) { valorActual = valorActual.dropLast(1); actualizarDisplay() }
        }
    }

    private fun actualizarDisplay() { binding.tvDisplay.text = valorActual }
}