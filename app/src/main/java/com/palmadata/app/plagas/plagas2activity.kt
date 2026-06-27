package com.palmadata.app.plagas

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.palmadata.app.databinding.ActivityPlagas2Binding
import com.palmadata.app.ui.WorkerAdapter
import com.palmadata.app.utils.DatabaseHelper

class Plagas2Activity : AppCompatActivity() {
    private lateinit var binding: ActivityPlagas2Binding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlagas2Binding.inflate(layoutInflater)
        setContentView(binding.root)
        val plantacionId     = intent.getIntExtra("plantacion_id", 0)
        val plantacionNombre = intent.getStringExtra("plantacion_nombre") ?: ""
        val sectorId         = intent.getIntExtra("sector_id", 0)
        val sectorNombre     = intent.getStringExtra("sector_nombre") ?: ""
        val lotes = DatabaseHelper(this).getLotesPorSector(sectorId)
        val adapter = WorkerAdapter { nombre ->
            val l = lotes.first { it.second == nombre }
            startActivity(Intent(this, Plagas3Activity::class.java).also {
                it.putExtra("plantacion_id", plantacionId)
                it.putExtra("plantacion_nombre", plantacionNombre)
                it.putExtra("sector_id", sectorId)
                it.putExtra("sector_nombre", sectorNombre)
                it.putExtra("lote_id", l.first)
                it.putExtra("lote_nombre", l.second)
            })
        }
        binding.rvLotes.layoutManager = LinearLayoutManager(this)
        binding.rvLotes.adapter = adapter
        adapter.submitList(lotes.map { it.second })

        binding.etBuscadorLotes.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                adapter.filter(s.toString())
                binding.tvNoLotes.visibility = if (adapter.isEmpty()) View.VISIBLE else View.GONE
            }
        })
    }
}