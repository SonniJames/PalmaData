package com.palmadata.app.polinizacion

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.palmadata.app.databinding.ActivityPoli3Binding

class Poli3Activity : AppCompatActivity() {
    private lateinit var binding: ActivityPoli3Binding
    private var aplicacion1 = 0
    private var aplicacion2 = 0
    private var aplicacion3 = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPoli3Binding.inflate(layoutInflater)
        setContentView(binding.root)

        val plantacionId     = intent.getIntExtra("plantacion_id", 0)
        val plantacionNombre = intent.getStringExtra("plantacion_nombre") ?: ""
        val sectorId         = intent.getIntExtra("sector_id", 0)
        val sectorNombre     = intent.getStringExtra("sector_nombre") ?: ""
        val loteId           = intent.getIntExtra("lote_id", 0)
        val loteNombre       = intent.getStringExtra("lote_nombre") ?: ""

        actualizarDisplays()

        // Primera
        binding.btnMas1.setOnClickListener { aplicacion1++; actualizarDisplays() }
        binding.btnMenos1.setOnClickListener { if (aplicacion1 > 0) { aplicacion1--; actualizarDisplays() } }

        // Segunda
        binding.btnMas2.setOnClickListener { aplicacion2++; actualizarDisplays() }
        binding.btnMenos2.setOnClickListener { if (aplicacion2 > 0) { aplicacion2--; actualizarDisplays() } }

        // Tercera
        binding.btnMas3.setOnClickListener { aplicacion3++; actualizarDisplays() }
        binding.btnMenos3.setOnClickListener { if (aplicacion3 > 0) { aplicacion3--; actualizarDisplays() } }

        binding.btnAccion.setOnClickListener {
            startActivity(Intent(this, Poli4Activity::class.java).also {
                it.putExtra("plantacion_id", plantacionId)
                it.putExtra("plantacion_nombre", plantacionNombre)
                it.putExtra("sector_id", sectorId)
                it.putExtra("sector_nombre", sectorNombre)
                it.putExtra("lote_id", loteId)
                it.putExtra("lote_nombre", loteNombre)
                it.putExtra("aplicacion1", aplicacion1)
                it.putExtra("aplicacion2", aplicacion2)
                it.putExtra("aplicacion3", aplicacion3)
            })
        }
    }

    private fun actualizarDisplays() {
        binding.tvValor1.text = aplicacion1.toString()
        binding.tvValor2.text = aplicacion2.toString()
        binding.tvValor3.text = aplicacion3.toString()
    }
}