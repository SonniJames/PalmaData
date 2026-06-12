package com.palmadata.app.plagas

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.palmadata.app.databinding.ActivityPlagas6Binding
import com.palmadata.app.ui.WorkerAdapter
import com.palmadata.app.utils.DatabaseHelper

class Plagas6Activity : AppCompatActivity() {
    private lateinit var binding: ActivityPlagas6Binding
    private lateinit var adapter: WorkerAdapter
    private var insectos = listOf<Pair<Int, String>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlagas6Binding.inflate(layoutInflater)
        setContentView(binding.root)

        val plantacionId = intent.getIntExtra("plantacion_id", 0)
        val plantacionNombre = intent.getStringExtra("plantacion_nombre") ?: ""
        val sectorId = intent.getIntExtra("sector_id", 0)
        val sectorNombre = intent.getStringExtra("sector_nombre") ?: ""
        val loteId = intent.getIntExtra("lote_id", 0)
        val loteNombre = intent.getStringExtra("lote_nombre") ?: ""
        val lectura = intent.getStringExtra("lectura") ?: ""
        val linea = intent.getStringExtra("linea") ?: ""
        val palma = intent.getStringExtra("palma") ?: ""

        insectos = DatabaseHelper(this).getInsectos()
        adapter = WorkerAdapter { nombre ->
            val insecto = insectos.first { it.second == nombre }
            startActivity(Intent(this, Plagas7Activity::class.java).also {
                it.putExtra("plantacion_id", plantacionId); it.putExtra("plantacion_nombre", plantacionNombre)
                it.putExtra("sector_id", sectorId); it.putExtra("sector_nombre", sectorNombre)
                it.putExtra("lote_id", loteId); it.putExtra("lote_nombre", loteNombre)
                it.putExtra("lectura", lectura); it.putExtra("linea", linea); it.putExtra("palma", palma)
                it.putExtra("insecto_id", insecto.first); it.putExtra("insecto_nombre", insecto.second)
            })
        }
        binding.rvInsectos.layoutManager = LinearLayoutManager(this)
        binding.rvInsectos.adapter = adapter
        adapter.submitList(insectos.map { it.second })

        if (insectos.isEmpty()) { binding.tvNoInsectos.visibility = View.VISIBLE }

        binding.etBuscador.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                adapter.filter(s.toString())
                binding.tvNoInsectos.visibility = if (adapter.isEmpty()) View.VISIBLE else View.GONE
            }
        })
    }
}