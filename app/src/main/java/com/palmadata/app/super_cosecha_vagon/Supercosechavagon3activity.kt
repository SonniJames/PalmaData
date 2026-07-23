package com.palmadata.app.super_cosecha_vagon

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import com.palmadata.app.databinding.ActivitySuperCosechaVagon3Binding
import com.palmadata.app.utils.DatabaseHelper

class SuperCosechaVagon3Activity : AppCompatActivity() {
    private lateinit var binding: ActivitySuperCosechaVagon3Binding
    private var trabajadores = listOf<Triple<Int, String, Int>>()

    // 0 = campo vacío (el trabajador es opcional). Al guardar, 0 viaja como NULL.
    private var trabajadorId = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySuperCosechaVagon3Binding.inflate(layoutInflater)
        setContentView(binding.root)

        val plantacionId     = intent.getIntExtra("plantacion_id", 0)
        val plantacionNombre = intent.getStringExtra("plantacion_nombre") ?: ""
        val sectorId         = intent.getIntExtra("sector_id", 0)
        val sectorNombre     = intent.getStringExtra("sector_nombre") ?: ""
        val loteId           = intent.getIntExtra("lote_id", 0)
        val loteNombre       = intent.getStringExtra("lote_nombre") ?: ""

        // Misma lista de trabajadores que alimenta la pantalla de módulos
        trabajadores = DatabaseHelper.getInstance(this).getTrabajadoresConSupervisor()
        val nombres = trabajadores.map { it.second }
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, nombres)

        binding.acTrabajador.setAdapter(adapter)
        binding.acTrabajador.setOnItemClickListener { _, _, position, _ ->
            val nombre = binding.acTrabajador.adapter.getItem(position).toString()
            trabajadorId = trabajadores.first { it.second == nombre }.first
        }

        binding.btnAccion.setOnClickListener {
            // Si el operario borró el texto tras haber elegido, la selección
            // deja de ser válida: el campo vuelve a considerarse vacío.
            if (binding.acTrabajador.text.toString().trim().isEmpty()) trabajadorId = 0

            startActivity(Intent(this, SuperCosechaVagon4Activity::class.java).also {
                it.putExtra("plantacion_id", plantacionId)
                it.putExtra("plantacion_nombre", plantacionNombre)
                it.putExtra("sector_id", sectorId)
                it.putExtra("sector_nombre", sectorNombre)
                it.putExtra("lote_id", loteId)
                it.putExtra("lote_nombre", loteNombre)
                it.putExtra("trabajador_id", trabajadorId)
            })
        }
    }
}