package com.palmadata.app.trampas

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.palmadata.app.databinding.ActivityTrampas4Binding

class Trampas4Activity : AppCompatActivity() {
    private lateinit var binding: ActivityTrampas4Binding
    private var valorActual = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTrampas4Binding.inflate(layoutInflater)
        setContentView(binding.root)
        val plantacionId     = intent.getIntExtra("plantacion_id", 0)
        val plantacionNombre = intent.getStringExtra("plantacion_nombre") ?: ""
        val lectura          = intent.getStringExtra("lectura") ?: ""
        val trampaId         = intent.getIntExtra("trampa_id", 0)
        val trampaCodigo     = intent.getStringExtra("trampa_codigo") ?: ""
        val machos           = intent.getStringExtra("machos") ?: "0"
        setupTeclado()
        binding.btnAccion.setOnClickListener {
            startActivity(Intent(this, Trampas5Activity::class.java).also {
                it.putExtra("plantacion_id", plantacionId)
                it.putExtra("plantacion_nombre", plantacionNombre)
                it.putExtra("lectura", lectura)
                it.putExtra("trampa_id", trampaId)
                it.putExtra("trampa_codigo", trampaCodigo)
                it.putExtra("machos", machos)
                it.putExtra("hembras", valorActual.ifEmpty { "0" })
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