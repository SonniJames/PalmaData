package com.palmadata.app.censo_enfermedades

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.palmadata.app.databinding.ActivityCensoEnf0Binding
import com.palmadata.app.ui.WorkerAdapter
import com.palmadata.app.utils.DatabaseHelper

class CensoEnf0Activity : AppCompatActivity() {

    private lateinit var binding: ActivityCensoEnf0Binding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCensoEnf0Binding.inflate(layoutInflater)
        setContentView(binding.root)

        val db           = DatabaseHelper(this)
        val plantaciones = db.getPlantaciones()

        val adapter = WorkerAdapter { nombreSeleccionado ->
            val plantacion = plantaciones.first { it.second == nombreSeleccionado }
            val nextIntent = Intent(this, CensoEnf1Activity::class.java)
            nextIntent.putExtra("plantacion_id",     plantacion.first)
            nextIntent.putExtra("plantacion_nombre", plantacion.second)
            startActivity(nextIntent)
        }

        binding.rvPlantaciones.layoutManager = LinearLayoutManager(this)
        binding.rvPlantaciones.adapter = adapter
        adapter.submitList(plantaciones.map { it.second })
    }
}