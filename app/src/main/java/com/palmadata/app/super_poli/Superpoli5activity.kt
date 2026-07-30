package com.palmadata.app.super_poli

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.palmadata.app.databinding.ActivitySuperPoliTecladoBinding

/**
 * Pantalla 5 — según el modo elegido en la 3:
 *   LP → PALMA: obligatoria, máximo 2 dígitos. Viaja como extra "palma".
 *   L  → CANTIDAD PALMAS: opcional, hasta 3 dígitos. Viaja como "cant_palmas".
 * El valor no usado en cada modo queda en 0 al guardar.
 *
 * El límite de 2 dígitos en PALMA no es arbitrario: el cat_palma_id que arma
 * el trigger en PostgreSQL concatena lote + línea (3 dígitos) + palma (2
 * dígitos). Una palma de 3 dígitos produciría un código ambiguo, incompatible
 * con los cat_palma_id del resto del sistema.
 */
class SuperPoli5Activity : AppCompatActivity() {
    private lateinit var binding: ActivitySuperPoliTecladoBinding
    private var valorActual = ""
    private var esModoLineaPalma = true
    private var maxDigitos = 2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySuperPoliTecladoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val plantacionId     = intent.getIntExtra("plantacion_id", 0)
        val plantacionNombre = intent.getStringExtra("plantacion_nombre") ?: ""
        val sectorId         = intent.getIntExtra("sector_id", 0)
        val sectorNombre     = intent.getStringExtra("sector_nombre") ?: ""
        val loteId           = intent.getIntExtra("lote_id", 0)
        val loteNombre       = intent.getStringExtra("lote_nombre") ?: ""
        val modo             = intent.getStringExtra("modo") ?: SuperPoli3Activity.MODO_LINEA_PALMA
        val linea            = intent.getStringExtra("linea") ?: ""

        esModoLineaPalma = modo == SuperPoli3Activity.MODO_LINEA_PALMA
        maxDigitos       = if (esModoLineaPalma) 2 else 3

        binding.tvTitulo.text  = if (esModoLineaPalma) "PALMA" else "CANTIDAD PALMAS"
        binding.btnAccion.text = "DEJADAS/POLINIZADAS"

        setupTeclado()

        binding.btnAccion.setOnClickListener {
            // En modo LÍNEA-PALMA la palma es obligatoria; en POR LÍNEA la
            // cantidad de palmas puede quedar vacía (llega 0 a la base).
            if (esModoLineaPalma) {
                if (valorActual.isEmpty()) {
                    mostrarError("Debe ingresar un valor para PALMA"); return@setOnClickListener
                }
                if (valorActual.length > 2) {
                    mostrarError("PALMA debe tener máximo 2 dígitos"); return@setOnClickListener
                }
            }

            startActivity(Intent(this, SuperPoli6Activity::class.java).also {
                it.putExtra("plantacion_id", plantacionId)
                it.putExtra("plantacion_nombre", plantacionNombre)
                it.putExtra("sector_id", sectorId)
                it.putExtra("sector_nombre", sectorNombre)
                it.putExtra("lote_id", loteId)
                it.putExtra("lote_nombre", loteNombre)
                it.putExtra("modo", modo)
                it.putExtra("linea", linea)
                it.putExtra("palma",       if (esModoLineaPalma) valorActual else "")
                it.putExtra("cant_palmas", if (esModoLineaPalma) "" else valorActual)
            })
        }
    }

    private fun setupTeclado() {
        val botones = mapOf(
            binding.btn0 to "0", binding.btn1 to "1", binding.btn2 to "2", binding.btn3 to "3",
            binding.btn4 to "4", binding.btn5 to "5", binding.btn6 to "6", binding.btn7 to "7",
            binding.btn8 to "8", binding.btn9 to "9"
        )
        botones.forEach { (btn, v) ->
            // 2 dígitos para PALMA, 3 para CANTIDAD PALMAS
            btn.setOnClickListener { if (valorActual.length < maxDigitos) { valorActual += v; actualizarDisplay() } }
        }
        binding.btnC.setOnClickListener { valorActual = ""; actualizarDisplay(); ocultarError() }
        binding.btnDel.setOnClickListener {
            if (valorActual.isNotEmpty()) { valorActual = valorActual.dropLast(1); actualizarDisplay(); ocultarError() }
        }
    }

    private fun actualizarDisplay() { binding.tvDisplay.text = valorActual }
    private fun mostrarError(msg: String) { binding.tvError.text = msg; binding.tvError.visibility = View.VISIBLE }
    private fun ocultarError() { binding.tvError.visibility = View.GONE }
}