package com.palmadata.app.strategus

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.palmadata.app.databinding.ActivityStrategus1Binding
import com.palmadata.app.ui.WorkerAdapter
import com.palmadata.app.utils.DatabaseHelper

class Strategus1Activity : AppCompatActivity() {
    private lateinit var binding: ActivityStrategus1Binding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStrategus1Binding.inflate(layoutInflater)
        setContentView(binding.root)
        val plantacionId     = intent.getIntExtra("plantacion_id", 0)
        val plantacionNombre = intent.getStringExtra("plantacion_nombre") ?: ""
        val sectores = DatabaseHelper(this).getSectoresPorPlantacion(plantacionId)
        val adapter = WorkerAdapter { nombre ->
            val s = sectores.first { it.second == nombre }
            startActivity(Intent(this, Strategus2Activity::class.java).also {
                it.putExtra("plantacion_id", plantacionId)
                it.putExtra("plantacion_nombre", plantacionNombre)
                it.putExtra("sector_id", s.first)
                it.putExtra("sector_nombre", s.second)
            })
        }
        binding.rvSectores.layoutManager = LinearLayoutManager(this)
        binding.rvSectores.adapter = adapter
        adapter.submitList(sectores.map { it.second })
    }
}