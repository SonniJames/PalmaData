package com.palmadata.app.censo_enfermedades

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.palmadata.app.databinding.ActivityCensoEnf2Binding
import com.palmadata.app.ui.WorkerAdapter

class CensoEnf2Activity : AppCompatActivity() {

    private lateinit var binding: ActivityCensoEnf2Binding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCensoEnf2Binding.inflate(layoutInflater)
        setContentView(binding.root)

        val sector = intent.getStringExtra("sector") ?: return
        val lotes = CensoEnfData.lotesPorSector[sector] ?: emptyList()

        val adapter = WorkerAdapter { loteSeleccionado ->
            val nextIntent = Intent(this, CensoEnf3Activity::class.java)
            nextIntent.putExtra("sector", sector)
            nextIntent.putExtra("lote", loteSeleccionado)
            startActivity(nextIntent)
        }

        binding.rvLotes.layoutManager = LinearLayoutManager(this)
        binding.rvLotes.adapter = adapter
        adapter.submitList(lotes)
    }
}