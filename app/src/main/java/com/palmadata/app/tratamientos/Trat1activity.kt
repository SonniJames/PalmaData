package com.palmadata.app.tratamientos

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.palmadata.app.databinding.ActivityTrat1Binding
import com.palmadata.app.ui.WorkerAdapter
import com.palmadata.app.utils.DatabaseHelper

class Trat1Activity : AppCompatActivity() {
    private lateinit var binding: ActivityTrat1Binding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTrat1Binding.inflate(layoutInflater)
        setContentView(binding.root)
        val plantacionId     = intent.getIntExtra("plantacion_id", 0)
        val plantacionNombre = intent.getStringExtra("plantacion_nombre") ?: ""
        val sectores = DatabaseHelper(this).getSectoresPorPlantacion(plantacionId)
        val adapter = WorkerAdapter { nombre ->
            val s = sectores.first { it.second == nombre }
            val i = Intent(this, Trat2Activity::class.java)
            i.putExtra("plantacion_id", plantacionId)
            i.putExtra("plantacion_nombre", plantacionNombre)
            i.putExtra("sector_id", s.first)
            i.putExtra("sector_nombre", s.second)
            startActivity(i)
        }
        binding.rvSectores.layoutManager = LinearLayoutManager(this)
        binding.rvSectores.adapter = adapter
        adapter.submitList(sectores.map { it.second })
    }
}