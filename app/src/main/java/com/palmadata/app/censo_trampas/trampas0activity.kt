package com.palmadata.app.trampas

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.palmadata.app.databinding.ActivityTrampas0Binding
import com.palmadata.app.ui.WorkerAdapter
import com.palmadata.app.utils.DatabaseHelper

class Trampas0Activity : AppCompatActivity() {
    private lateinit var binding: ActivityTrampas0Binding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTrampas0Binding.inflate(layoutInflater)
        setContentView(binding.root)
        val plantaciones = DatabaseHelper(this).getPlantaciones()
        val adapter = WorkerAdapter { nombre ->
            val p = plantaciones.first { it.second == nombre }
            startActivity(Intent(this, Trampas1Activity::class.java).also {
                it.putExtra("plantacion_id", p.first)
                it.putExtra("plantacion_nombre", p.second)
            })
        }
        binding.rvPlantaciones.layoutManager = LinearLayoutManager(this)
        binding.rvPlantaciones.adapter = adapter
        adapter.submitList(plantaciones.map { it.second })
    }
}