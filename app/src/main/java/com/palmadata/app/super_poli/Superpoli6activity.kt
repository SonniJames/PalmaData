package com.palmadata.app.super_poli

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.palmadata.app.databinding.ActivitySuperPoli6Binding

/**
 * Pantalla 6 — DEJADAS/POLINIZADAS. Tres contadores (Primera, Segunda,
 * Tercera) que alimentan flor_polini_aplicacion1/2/3. Compartida por los
 * dos flujos: el extra "modo" solo pasa de largo hacia las siguientes.
 */
class SuperPoli6Activity : AppCompatActivity() {
    private lateinit var binding: ActivitySuperPoli6Binding

    private var primera = 0
    private var segunda = 0
    private var tercera = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySuperPoli6Binding.inflate(layoutInflater)
        setContentView(binding.root)

        val plantacionId     = intent.getIntExtra("plantacion_id", 0)
        val plantacionNombre = intent.getStringExtra("plantacion_nombre") ?: ""
        val sectorId         = intent.getIntExtra("sector_id", 0)
        val sectorNombre     = intent.getStringExtra("sector_nombre") ?: ""
        val loteId           = intent.getIntExtra("lote_id", 0)
        val loteNombre       = intent.getStringExtra("lote_nombre") ?: ""
        val modo             = intent.getStringExtra("modo") ?: SuperPoli3Activity.MODO_LINEA_PALMA
        val linea            = intent.getStringExtra("linea") ?: ""
        val palma            = intent.getStringExtra("palma") ?: ""
        val cantPalmas       = intent.getStringExtra("cant_palmas") ?: ""

        actualizarDisplays()
        setupContadores()

        binding.btnAccion.setOnClickListener {
            startActivity(Intent(this, SuperPoliValorActivity::class.java).also {
                it.putExtra("plantacion_id", plantacionId)
                it.putExtra("plantacion_nombre", plantacionNombre)
                it.putExtra("sector_id", sectorId)
                it.putExtra("sector_nombre", sectorNombre)
                it.putExtra("lote_id", loteId)
                it.putExtra("lote_nombre", loteNombre)
                it.putExtra("modo", modo)
                it.putExtra("linea", linea)
                it.putExtra("palma", palma)
                it.putExtra("cant_palmas", cantPalmas)
                it.putExtra("primera", primera)
                it.putExtra("segunda", segunda)
                it.putExtra("tercera", tercera)
                it.putExtra("paso", SuperPoliValorActivity.PASO_HOJA_SIN_MARCAR)
            })
        }
    }

    private fun setupContadores() {
        binding.btnMas1.setOnClickListener   { primera++; actualizarDisplays() }
        binding.btnMenos1.setOnClickListener { if (primera > 0) { primera--; actualizarDisplays() } }
        binding.btnMas2.setOnClickListener   { segunda++; actualizarDisplays() }
        binding.btnMenos2.setOnClickListener { if (segunda > 0) { segunda--; actualizarDisplays() } }
        binding.btnMas3.setOnClickListener   { tercera++; actualizarDisplays() }
        binding.btnMenos3.setOnClickListener { if (tercera > 0) { tercera--; actualizarDisplays() } }
    }

    private fun actualizarDisplays() {
        binding.tvValor1.text = primera.toString()
        binding.tvValor2.text = segunda.toString()
        binding.tvValor3.text = tercera.toString()
    }
}