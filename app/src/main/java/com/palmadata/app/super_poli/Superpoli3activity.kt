package com.palmadata.app.super_poli

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.palmadata.app.databinding.ActivitySuperPoli3Binding

/**
 * Selector de flujo. Define el resto del recorrido:
 *   MODO_LINEA_PALMA → pantalla 5 pide PALMA (obligatoria)
 *   MODO_POR_LINEA   → pantalla 5 pide CANTIDAD PALMAS (opcional)
 * El modo viaja como extra hasta el guardado, donde decide si el valor
 * capturado va a la columna `palma` o a `cant_palmas`.
 */
class SuperPoli3Activity : AppCompatActivity() {

    companion object {
        const val MODO_LINEA_PALMA = "LP"
        const val MODO_POR_LINEA   = "L"
    }

    private lateinit var binding: ActivitySuperPoli3Binding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySuperPoli3Binding.inflate(layoutInflater)
        setContentView(binding.root)

        val plantacionId     = intent.getIntExtra("plantacion_id", 0)
        val plantacionNombre = intent.getStringExtra("plantacion_nombre") ?: ""
        val sectorId         = intent.getIntExtra("sector_id", 0)
        val sectorNombre     = intent.getStringExtra("sector_nombre") ?: ""
        val loteId           = intent.getIntExtra("lote_id", 0)
        val loteNombre       = intent.getStringExtra("lote_nombre") ?: ""

        fun continuar(modo: String) {
            startActivity(Intent(this, SuperPoli4Activity::class.java).also {
                it.putExtra("plantacion_id", plantacionId)
                it.putExtra("plantacion_nombre", plantacionNombre)
                it.putExtra("sector_id", sectorId)
                it.putExtra("sector_nombre", sectorNombre)
                it.putExtra("lote_id", loteId)
                it.putExtra("lote_nombre", loteNombre)
                it.putExtra("modo", modo)
            })
        }

        binding.btnLineaPalma.setOnClickListener { continuar(MODO_LINEA_PALMA) }
        binding.btnPorLinea.setOnClickListener   { continuar(MODO_POR_LINEA) }
    }
}