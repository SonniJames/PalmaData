package com.palmadata.app.censo_enfermedades

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.palmadata.app.databinding.ActivityCensoEnf2Binding
import com.palmadata.app.ui.WorkerAdapter
import com.palmadata.app.utils.DatabaseHelper

class CensoEnf2Activity : AppCompatActivity() {

    private lateinit var binding: ActivityCensoEnf2Binding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCensoEnf2Binding.inflate(layoutInflater)
        setContentView(binding.root)

        val plantacionId     = intent.getIntExtra("plantacion_id", 0)
        val plantacionNombre = intent.getStringExtra("plantacion_nombre") ?: ""
        val sectorId         = intent.getIntExtra("sector_id", 0)
        val sectorNombre     = intent.getStringExtra("sector_nombre") ?: ""

        val db    = DatabaseHelper(this)
        val lotes = db.getLotesPorSector(sectorId)

        val adapter = WorkerAdapter { nombreSeleccionado ->
            val lote = lotes.first { it.second == nombreSeleccionado }
            val nextIntent = Intent(this, CensoEnf3Activity::class.java)
            nextIntent.putExtra("plantacion_id",     plantacionId)
            nextIntent.putExtra("plantacion_nombre", plantacionNombre)
            nextIntent.putExtra("sector_id",         sectorId)
            nextIntent.putExtra("sector_nombre",     sectorNombre)
            nextIntent.putExtra("lote_id",           lote.first)
            nextIntent.putExtra("lote_nombre",       lote.second)
            startActivity(nextIntent)
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