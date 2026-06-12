package com.palmadata.app.supercosecha

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import com.palmadata.app.databinding.ActivitySuperCosecha4Binding
import com.palmadata.app.utils.DatabaseHelper

class SuperCosecha4Activity : AppCompatActivity() {
    private lateinit var binding: ActivitySuperCosecha4Binding
    private var trabajadores = listOf<Triple<Int, String, Int>>()
    private var cortadorId = 0
    private var recolectorId = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySuperCosecha4Binding.inflate(layoutInflater)
        setContentView(binding.root)

        val plantacionId     = intent.getIntExtra("plantacion_id", 0)
        val plantacionNombre = intent.getStringExtra("plantacion_nombre") ?: ""
        val sectorId         = intent.getIntExtra("sector_id", 0)
        val sectorNombre     = intent.getStringExtra("sector_nombre") ?: ""
        val loteId           = intent.getIntExtra("lote_id", 0)
        val loteNombre       = intent.getStringExtra("lote_nombre") ?: ""
        val ciclo            = intent.getStringExtra("ciclo") ?: "0"

        trabajadores = DatabaseHelper(this).getTrabajadoresConSupervisor()
        val nombres = trabajadores.map { it.second }

        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, nombres)

        binding.acCortador.setAdapter(adapter)
        binding.acCortador.setOnItemClickListener { _, _, position, _ ->
            val nombre = binding.acCortador.adapter.getItem(position).toString()
            cortadorId = trabajadores.first { it.second == nombre }.first
        }

        binding.acRecolector.setAdapter(adapter)
        binding.acRecolector.setOnItemClickListener { _, _, position, _ ->
            val nombre = binding.acRecolector.adapter.getItem(position).toString()
            recolectorId = trabajadores.first { it.second == nombre }.first
        }

        binding.btnAccion.setOnClickListener {
            startActivity(Intent(this, SuperCosecha5Activity::class.java).also {
                it.putExtra("plantacion_id", plantacionId)
                it.putExtra("plantacion_nombre", plantacionNombre)
                it.putExtra("sector_id", sectorId)
                it.putExtra("sector_nombre", sectorNombre)
                it.putExtra("lote_id", loteId)
                it.putExtra("lote_nombre", loteNombre)
                it.putExtra("ciclo", ciclo)
                it.putExtra("cortador_id", cortadorId)
                it.putExtra("recolector_id", recolectorId)
            })
        }
    }
}