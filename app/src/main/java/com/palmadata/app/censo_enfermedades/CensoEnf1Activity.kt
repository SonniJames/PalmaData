package com.palmadata.app.censo_enfermedades

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.palmadata.app.databinding.ActivityCensoEnf1Binding
import com.palmadata.app.ui.WorkerAdapter

class CensoEnf1Activity : AppCompatActivity() {

    private lateinit var binding: ActivityCensoEnf1Binding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCensoEnf1Binding.inflate(layoutInflater)
        setContentView(binding.root)

        val adapter = WorkerAdapter { sectorSeleccionado ->
            val nextIntent = Intent(this, CensoEnf2Activity::class.java)
            nextIntent.putExtra("sector", sectorSeleccionado)
            startActivity(nextIntent)
        }

        binding.rvSectores.layoutManager = LinearLayoutManager(this)
        binding.rvSectores.adapter = adapter
        adapter.submitList(CensoEnfData.sectores)
    }
}