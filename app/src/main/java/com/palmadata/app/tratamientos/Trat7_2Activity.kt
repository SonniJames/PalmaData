package com.palmadata.app.tratamientos

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.palmadata.app.databinding.ActivityTrat72Binding

/** Pantalla 7.2 — ÁREA INTERVENIDA. Teclado con punto decimal, opcional: vacío viaja como "" y se guarda NULL. */
class Trat7_2Activity : AppCompatActivity() {
    private lateinit var binding: ActivityTrat72Binding
    private var valorActual = ""
    companion object { private const val MAX_LEN = 10 }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTrat72Binding.inflate(layoutInflater)
        setContentView(binding.root)
        setupTeclado()
        binding.btnAccion.setOnClickListener {
            startActivity(Intent(this, Trat7_3Activity::class.java).also {
                intent.extras?.let { e -> it.putExtras(e) }
                it.putExtra("area_intervenida", valorActual)
            })
        }
    }

    private fun setupTeclado() {
        val botones = mapOf(binding.btn0 to "0", binding.btn1 to "1", binding.btn2 to "2", binding.btn3 to "3", binding.btn4 to "4", binding.btn5 to "5", binding.btn6 to "6", binding.btn7 to "7", binding.btn8 to "8", binding.btn9 to "9")
        botones.forEach { (btn, v) -> btn.setOnClickListener { if (valorActual.length < MAX_LEN) { valorActual += v; actualizarDisplay() } } }
        binding.btnPunto.setOnClickListener {
            // Solo un punto por número; si está vacío, arranca con "0."
            if (!valorActual.contains(".") && valorActual.length < MAX_LEN) {
                valorActual = if (valorActual.isEmpty()) "0." else valorActual + "."
                actualizarDisplay()
            }
        }
        binding.btnC.setOnClickListener { valorActual = ""; actualizarDisplay() }
        binding.btnDel.setOnClickListener { if (valorActual.isNotEmpty()) { valorActual = valorActual.dropLast(1); actualizarDisplay() } }
    }
    private fun actualizarDisplay() { binding.tvDisplay.text = valorActual }
}
