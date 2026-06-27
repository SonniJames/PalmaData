package com.palmadata.app.tratamientos

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.palmadata.app.databinding.ActivityTrat2Binding
import com.palmadata.app.ui.WorkerAdapter
import com.palmadata.app.utils.DatabaseHelper

class Trat2Activity : AppCompatActivity() {
    private lateinit var binding: ActivityTrat2Binding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTrat2Binding.inflate(layoutInflater)
        setContentView(binding.root)
        val plantacionId     = intent.getIntExtra("plantacion_id", 0)
        val plantacionNombre = intent.getStringExtra("plantacion_nombre") ?: ""
        val sectorId         = intent.getIntExtra("sector_id", 0)
        val sectorNombre     = intent.getStringExtra("sector_nombre") ?: ""
        val lotes = DatabaseHelper(this).getLotesPorSector(sectorId)
        val adapter = WorkerAdapter { nombre ->
            val l = lotes.first { it.second == nombre }
            val i = Intent(this, Trat3Activity::class.java)
            i.putExtra("plantacion_id", plantacionId)
            i.putExtra("plantacion_nombre", plantacionNombre)
            i.putExtra("sector_id", sectorId)
            i.putExtra("sector_nombre", sectorNombre)
            i.putExtra("lote_id", l.first)
            i.putExtra("lote_nombre", l.second)
            startActivity(i)
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