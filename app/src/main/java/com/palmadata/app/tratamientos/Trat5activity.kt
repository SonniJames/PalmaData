package com.palmadata.app.tratamientos

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.palmadata.app.databinding.ActivityTrat5Binding
import com.palmadata.app.ui.WorkerAdapter
import com.palmadata.app.utils.DatabaseHelper

class Trat5Activity : AppCompatActivity() {
    private lateinit var binding: ActivityTrat5Binding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTrat5Binding.inflate(layoutInflater)
        setContentView(binding.root)
        val plantacionId     = intent.getIntExtra("plantacion_id", 0)
        val plantacionNombre = intent.getStringExtra("plantacion_nombre") ?: ""
        val sectorId         = intent.getIntExtra("sector_id", 0)
        val sectorNombre     = intent.getStringExtra("sector_nombre") ?: ""
        val loteId           = intent.getIntExtra("lote_id", 0)
        val loteNombre       = intent.getStringExtra("lote_nombre") ?: ""
        val linea            = intent.getStringExtra("linea") ?: ""
        val palma            = intent.getStringExtra("palma") ?: ""
        val tratamientos = DatabaseHelper(this).getTratamientosEventos()
        val adapter = WorkerAdapter { codigo ->
            val t = tratamientos.first { it.second == codigo }
            val i = Intent(this, Trat6Activity::class.java)
            i.putExtra("plantacion_id", plantacionId)
            i.putExtra("plantacion_nombre", plantacionNombre)
            i.putExtra("sector_id", sectorId)
            i.putExtra("sector_nombre", sectorNombre)
            i.putExtra("lote_id", loteId)
            i.putExtra("lote_nombre", loteNombre)
            i.putExtra("linea", linea)
            i.putExtra("palma", palma)
            i.putExtra("tratamiento_id", t.first)
            i.putExtra("tratamiento_codigo", t.second)
            startActivity(i)
        }
        binding.rvTratamientos.layoutManager = LinearLayoutManager(this)
        binding.rvTratamientos.adapter = adapter
        adapter.submitList(tratamientos.map { it.second })
    }
}