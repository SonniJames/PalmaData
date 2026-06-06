package com.palmadata.app.strategus

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.palmadata.app.databinding.ActivityStrategus6Binding

class Strategus6Activity : AppCompatActivity() {
    private lateinit var binding: ActivityStrategus6Binding
    private var valorActual = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStrategus6Binding.inflate(layoutInflater)
        setContentView(binding.root)
        val plantacionId     = intent.getIntExtra("plantacion_id", 0)
        val plantacionNombre = intent.getStringExtra("plantacion_nombre") ?: ""
        val sectorId         = intent.getIntExtra("sector_id", 0)
        val sectorNombre     = intent.getStringExtra("sector_nombre") ?: ""
        val loteId           = intent.getIntExtra("lote_id", 0)
        val loteNombre       = intent.getStringExtra("lote_nombre") ?: ""
        val censo            = intent.getStringExtra("censo") ?: ""
        val linea            = intent.getStringExtra("linea") ?: ""
        val palma            = intent.getStringExtra("palma") ?: ""
        setupTeclado()
        binding.btnAccion.setOnClickListener {
            startActivity(Intent(this, Strategus7Activity::class.java).also {
                it.putExtra("plantacion_id", plantacionId)
                it.putExtra("plantacion_nombre", plantacionNombre)
                it.putExtra("sector_id", sectorId)
                it.putExtra("sector_nombre", sectorNombre)
                it.putExtra("lote_id", loteId)
                it.putExtra("lote_nombre", loteNombre)
                it.putExtra("censo", censo)
                it.putExtra("linea", linea)
                it.putExtra("palma", palma)
                it.putExtra("galerias", valorActual.ifEmpty { "0" })
            })
        }
    }
    private fun setupTeclado() {
        val botones = mapOf(binding.btn0 to "0", binding.btn1 to "1", binding.btn2 to "2", binding.btn3 to "3", binding.btn4 to "4", binding.btn5 to "5", binding.btn6 to "6", binding.btn7 to "7", binding.btn8 to "8", binding.btn9 to "9")
        botones.forEach { (btn, v) -> btn.setOnClickListener { valorActual += v; actualizarDisplay() } }
        binding.btnC.setOnClickListener { valorActual = ""; actualizarDisplay() }
        binding.btnDel.setOnClickListener { if (valorActual.isNotEmpty()) { valorActual = valorActual.dropLast(1); actualizarDisplay() } }
    }
    private fun actualizarDisplay() { binding.tvDisplay.text = valorActual.ifEmpty { "0" } }
}