package com.palmadata.app.tratamientos

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.palmadata.app.databinding.ActivityTrat0Binding
import com.palmadata.app.ui.WorkerAdapter
import com.palmadata.app.utils.DatabaseHelper

class Trat0Activity : AppCompatActivity() {
    private lateinit var binding: ActivityTrat0Binding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTrat0Binding.inflate(layoutInflater)
        setContentView(binding.root)
        val plantaciones = DatabaseHelper(this).getPlantaciones()
        val adapter = WorkerAdapter { nombre ->
            val p = plantaciones.first { it.second == nombre }
            val i = Intent(this, Trat1Activity::class.java)
            i.putExtra("plantacion_id", p.first)
            i.putExtra("plantacion_nombre", p.second)
            startActivity(i)
        }
        binding.rvPlantaciones.layoutManager = LinearLayoutManager(this)
        binding.rvPlantaciones.adapter = adapter
        adapter.submitList(plantaciones.map { it.second })
    }
}