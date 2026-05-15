package com.palmadata.app.censo_enfermedades

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.palmadata.app.databinding.ActivityCensoEnf7Binding
import com.palmadata.app.ui.WorkerAdapter

class CensoEnf7Activity : AppCompatActivity() {

    private lateinit var binding: ActivityCensoEnf7Binding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCensoEnf7Binding.inflate(layoutInflater)
        setContentView(binding.root)

        val sector           = intent.getStringExtra("sector") ?: ""
        val lote             = intent.getStringExtra("lote") ?: ""
        val censo            = intent.getStringExtra("censo") ?: ""
        val linea            = intent.getStringExtra("linea") ?: ""
        val palma            = intent.getStringExtra("palma") ?: ""
        val enfermedadId     = intent.getIntExtra("enfermedadId", 0)
        val enfermedadNombre = intent.getStringExtra("enfermedadNombre") ?: ""

        val eventos = CensoEnfData.eventosPorEnfermedad[enfermedadId] ?: emptyList()

        val adapter = WorkerAdapter { nombreEvento ->
            val evento = eventos.first { it.nombre == nombreEvento }
            val nextIntent = Intent(this, CensoEnf8Activity::class.java)
            nextIntent.putExtra("sector", sector)
            nextIntent.putExtra("lote", lote)
            nextIntent.putExtra("censo", censo)
            nextIntent.putExtra("linea", linea)
            nextIntent.putExtra("palma", palma)
            nextIntent.putExtra("enfermedadId", enfermedadId)
            nextIntent.putExtra("enfermedadNombre", enfermedadNombre)
            nextIntent.putExtra("eventoId", evento.id)
            nextIntent.putExtra("eventoNombre", evento.nombre)
            startActivity(nextIntent)
        }

        binding.rvEventos.layoutManager = LinearLayoutManager(this)
        binding.rvEventos.adapter = adapter
        adapter.submitList(eventos.map { it.nombre })
    }
}