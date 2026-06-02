package com.palmadata.app.censo_enfermedades

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.palmadata.app.databinding.ActivityCensoEnf6Binding
import com.palmadata.app.ui.WorkerAdapter
import com.palmadata.app.utils.DatabaseHelper

class CensoEnf6Activity : AppCompatActivity() {

    private lateinit var binding: ActivityCensoEnf6Binding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCensoEnf6Binding.inflate(layoutInflater)
        setContentView(binding.root)

        val plantacionId     = intent.getIntExtra("plantacion_id", 0)
        val plantacionNombre = intent.getStringExtra("plantacion_nombre") ?: ""
        val sectorId         = intent.getIntExtra("sector_id", 0)
        val sectorNombre     = intent.getStringExtra("sector_nombre") ?: ""
        val loteId           = intent.getIntExtra("lote_id", 0)
        val loteNombre       = intent.getStringExtra("lote_nombre") ?: ""
        val censo            = intent.getStringExtra("censo") ?: ""
        val linea            = intent.getStringExtra("linea") ?: ""
        val palma            = intent.getStringExtra("palma") ?: ""

        val db           = DatabaseHelper(this)
        val enfermedades = db.getEnfermedades()

        val adapter = WorkerAdapter { nombreEnf ->
            val enfermedad = enfermedades.first { it.second == nombreEnf }
            val nextIntent = Intent(this, CensoEnf7Activity::class.java)
            nextIntent.putExtra("plantacion_id",     plantacionId)
            nextIntent.putExtra("plantacion_nombre", plantacionNombre)
            nextIntent.putExtra("sector_id",         sectorId)
            nextIntent.putExtra("sector_nombre",     sectorNombre)
            nextIntent.putExtra("lote_id",           loteId)
            nextIntent.putExtra("lote_nombre",       loteNombre)
            nextIntent.putExtra("censo",             censo)
            nextIntent.putExtra("linea",             linea)
            nextIntent.putExtra("palma",             palma)
            nextIntent.putExtra("enfermedadId",      enfermedad.first)
            nextIntent.putExtra("enfermedadNombre",  enfermedad.second)
            startActivity(nextIntent)
        }

        binding.rvEnfermedades.layoutManager = LinearLayoutManager(this)
        binding.rvEnfermedades.adapter = adapter
        adapter.submitList(enfermedades.map { it.second })
    }
}