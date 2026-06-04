package com.palmadata.app.tratamientos

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.palmadata.app.databinding.ActivityTrat8Binding

class Trat8Activity : AppCompatActivity() {
    private lateinit var binding: ActivityTrat8Binding
    private var valorActual = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTrat8Binding.inflate(layoutInflater)
        setContentView(binding.root)
        val plantacionId      = intent.getIntExtra("plantacion_id", 0)
        val plantacionNombre  = intent.getStringExtra("plantacion_nombre") ?: ""
        val sectorId          = intent.getIntExtra("sector_id", 0)
        val sectorNombre      = intent.getStringExtra("sector_nombre") ?: ""
        val loteId            = intent.getIntExtra("lote_id", 0)
        val loteNombre        = intent.getStringExtra("lote_nombre") ?: ""
        val linea             = intent.getStringExtra("linea") ?: ""
        val palma             = intent.getStringExtra("palma") ?: ""
        val tratamientoId     = intent.getIntExtra("tratamiento_id", 0)
        val tratamientoCodigo = intent.getStringExtra("tratamiento_codigo") ?: ""
        val enfermedadId      = intent.getIntExtra("enfermedad_id", 0)
        val enfermedadNombre  = intent.getStringExtra("enfermedad_nombre") ?: ""
        val eventoId          = intent.getIntExtra("evento_id", 0)
        val eventoCodigo      = intent.getStringExtra("evento_codigo") ?: ""
        setupTeclado()
        binding.btnAccion.setOnClickListener {
            val i = Intent(this, Trat9Activity::class.java)
            i.putExtra("plantacion_id", plantacionId)
            i.putExtra("plantacion_nombre", plantacionNombre)
            i.putExtra("sector_id", sectorId)
            i.putExtra("sector_nombre", sectorNombre)
            i.putExtra("lote_id", loteId)
            i.putExtra("lote_nombre", loteNombre)
            i.putExtra("linea", linea)
            i.putExtra("palma", palma)
            i.putExtra("tratamiento_id", tratamientoId)
            i.putExtra("tratamiento_codigo", tratamientoCodigo)
            i.putExtra("enfermedad_id", enfermedadId)
            i.putExtra("enfermedad_nombre", enfermedadNombre)
            i.putExtra("evento_id", eventoId)
            i.putExtra("evento_codigo", eventoCodigo)
            i.putExtra("cantidad", valorActual)
            startActivity(i)
        }
    }
    private fun setupTeclado() {
        val botones = mapOf(binding.btn0 to "0", binding.btn1 to "1", binding.btn2 to "2", binding.btn3 to "3", binding.btn4 to "4", binding.btn5 to "5", binding.btn6 to "6", binding.btn7 to "7", binding.btn8 to "8", binding.btn9 to "9")
        botones.forEach { (btn, v) -> btn.setOnClickListener { valorActual += v; actualizarDisplay() } }
        binding.btnC.setOnClickListener { valorActual = ""; actualizarDisplay() }
        binding.btnDel.setOnClickListener { if (valorActual.isNotEmpty()) { valorActual = valorActual.dropLast(1); actualizarDisplay() } }
    }
    private fun actualizarDisplay() { binding.tvDisplay.text = valorActual }
}