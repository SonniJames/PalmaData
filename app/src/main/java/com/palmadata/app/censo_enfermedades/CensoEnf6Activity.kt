package com.palmadata.app.censo_enfermedades

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.palmadata.app.databinding.ActivityCensoEnf6Binding
import com.palmadata.app.ui.WorkerAdapter

class CensoEnf6Activity : AppCompatActivity() {

    private lateinit var binding: ActivityCensoEnf6Binding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCensoEnf6Binding.inflate(layoutInflater)
        setContentView(binding.root)

        val sector = intent.getStringExtra("sector") ?: ""
        val lote   = intent.getStringExtra("lote") ?: ""
        val censo  = intent.getStringExtra("censo") ?: ""
        val linea  = intent.getStringExtra("linea") ?: ""
        val palma  = intent.getStringExtra("palma") ?: ""

        val adapter = WorkerAdapter { nombreEnf ->
            val enfermedad = CensoEnfData.enfermedades.first { it.nombre == nombreEnf }
            val nextIntent = Intent(this, CensoEnf7Activity::class.java)
            nextIntent.putExtra("sector", sector)
            nextIntent.putExtra("lote", lote)
            nextIntent.putExtra("censo", censo)
            nextIntent.putExtra("linea", linea)
            nextIntent.putExtra("palma", palma)
            nextIntent.putExtra("enfermedadId", enfermedad.id)
            nextIntent.putExtra("enfermedadNombre", enfermedad.nombre)
            startActivity(nextIntent)
        }

        binding.rvEnfermedades.layoutManager = LinearLayoutManager(this)
        binding.rvEnfermedades.adapter = adapter
        adapter.submitList(CensoEnfData.enfermedades.map { it.nombre })
    }
}