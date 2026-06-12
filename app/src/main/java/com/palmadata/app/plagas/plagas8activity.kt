package com.palmadata.app.plagas

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.palmadata.app.databinding.ActivityPlagas8Binding

class Plagas8Activity : AppCompatActivity() {
    private lateinit var binding: ActivityPlagas8Binding
    private var valorActual = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlagas8Binding.inflate(layoutInflater)
        setContentView(binding.root)
        val plantacionId = intent.getIntExtra("plantacion_id", 0)
        val loteId = intent.getIntExtra("lote_id", 0)
        val lectura = intent.getStringExtra("lectura") ?: ""
        val linea = intent.getStringExtra("linea") ?: ""
        val palma = intent.getStringExtra("palma") ?: ""
        val insectoId = intent.getIntExtra("insecto_id", 0)
        val estadoInsectoId = intent.getIntExtra("estado_insecto_id", 0)
        setupTeclado()
        binding.btnAccion.setOnClickListener {
            startActivity(Intent(this, Plagas9Activity::class.java).also {
                it.putExtra("plantacion_id", plantacionId); it.putExtra("lote_id", loteId)
                it.putExtra("lectura", lectura); it.putExtra("linea", linea); it.putExtra("palma", palma)
                it.putExtra("insecto_id", insectoId); it.putExtra("estado_insecto_id", estadoInsectoId)
                it.putExtra("cantidad", valorActual.ifEmpty { "0" })
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