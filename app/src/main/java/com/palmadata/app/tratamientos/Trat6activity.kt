package com.palmadata.app.tratamientos

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.palmadata.app.databinding.ActivityTrat6Binding
import com.palmadata.app.ui.WorkerAdapter
import com.palmadata.app.utils.DatabaseHelper

class Trat6Activity : AppCompatActivity() {
    private lateinit var binding: ActivityTrat6Binding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTrat6Binding.inflate(layoutInflater)
        setContentView(binding.root)
        val plantacionId     = intent.getIntExtra("plantacion_id", 0)
        val plantacionNombre = intent.getStringExtra("plantacion_nombre") ?: ""
        val sectorId         = intent.getIntExtra("sector_id", 0)
        val sectorNombre     = intent.getStringExtra("sector_nombre") ?: ""
        val loteId           = intent.getIntExtra("lote_id", 0)
        val loteNombre       = intent.getStringExtra("lote_nombre") ?: ""
        val linea            = intent.getStringExtra("linea") ?: ""
        val palma            = intent.getStringExtra("palma") ?: ""
        val tratamientoId    = intent.getIntExtra("tratamiento_id", 0)
        val tratamientoCodigo = intent.getStringExtra("tratamiento_codigo") ?: ""
        val enfermedades = DatabaseHelper(this).getEnfermedades()
        val adapter = WorkerAdapter { nombre ->
            val e = enfermedades.first { it.second == nombre }
            val i = Intent(this, Trat7Activity::class.java)
            i.putExtra("plantacion_id", plantacionId)
            i.putExtra("plantacion_nombre", plantacionNombre)
            i.putExtra("sector_id", sectorId)
            i.putExtra("sector_nombre", sectorNombre)
            i.putExtra("lote_id", loteId)
            i.putExtra("lote_nombre", loteNombre)
            i.putExtra("linea", linea)
            i.putExtra("palma", palma)
            i.putExtra("tratamiento_id", tratamientoId)
            i.putExtra("tratamiento_codigo", tratamientoCodigo)
            i.putExtra("enfermedad_id", e.first)
            i.putExtra("enfermedad_nombre", e.second)
            startActivity(i)
        }
        binding.rvEnfermedades.layoutManager = LinearLayoutManager(this)
        binding.rvEnfermedades.adapter = adapter
        adapter.submitList(enfermedades.map { it.second })
    }
}