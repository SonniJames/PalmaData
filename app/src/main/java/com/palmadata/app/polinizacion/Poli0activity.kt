package com.palmadata.app.polinizacion

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.palmadata.app.databinding.ActivityPoli0Binding
import com.palmadata.app.ui.WorkerAdapter
import com.palmadata.app.utils.DatabaseHelper

class Poli0Activity : AppCompatActivity() {
    private lateinit var binding: ActivityPoli0Binding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPoli0Binding.inflate(layoutInflater)
        setContentView(binding.root)
        val plantaciones = DatabaseHelper(this).getPlantaciones()
        val adapter = WorkerAdapter { nombre ->
            val p = plantaciones.first { it.second == nombre }
            startActivity(Intent(this, Poli1Activity::class.java).also {
                it.putExtra("plantacion_id", p.first)
                it.putExtra("plantacion_nombre", p.second)
            })
        }
        binding.rvPlantaciones.layoutManager = LinearLayoutManager(this)
        binding.rvPlantaciones.adapter = adapter
        adapter.submitList(plantaciones.map { it.second })
    }
}