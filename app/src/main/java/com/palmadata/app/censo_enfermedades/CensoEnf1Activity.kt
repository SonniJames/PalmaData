package com.palmadata.app.censo_enfermedades

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.palmadata.app.databinding.ActivityCensoEnf1Binding
import com.palmadata.app.ui.WorkerAdapter
import com.palmadata.app.utils.DatabaseHelper

class CensoEnf1Activity : AppCompatActivity() {

    private lateinit var binding: ActivityCensoEnf1Binding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCensoEnf1Binding.inflate(layoutInflater)
        setContentView(binding.root)

        val plantacionId     = intent.getIntExtra("plantacion_id", 0)
        val plantacionNombre = intent.getStringExtra("plantacion_nombre") ?: ""

        val db      = DatabaseHelper(this)
        val sectores = db.getSectoresPorPlantacion(plantacionId)

        val adapter = WorkerAdapter { nombreSeleccionado ->
            val sector = sectores.first { it.second == nombreSeleccionado }
            val nextIntent = Intent(this, CensoEnf2Activity::class.java)
            nextIntent.putExtra("plantacion_id",     plantacionId)
            nextIntent.putExtra("plantacion_nombre", plantacionNombre)
            nextIntent.putExtra("sector_id",         sector.first)
            nextIntent.putExtra("sector_nombre",     sector.second)
            startActivity(nextIntent)
        }

        binding.rvSectores.layoutManager = LinearLayoutManager(this)
        binding.rvSectores.adapter = adapter
        adapter.submitList(sectores.map { it.second })
    }
}