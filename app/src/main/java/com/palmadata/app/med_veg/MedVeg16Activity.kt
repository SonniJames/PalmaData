package com.palmadata.app.med_veg

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.palmadata.app.databinding.ActivityMedVeg16Binding

/** Pantalla 16 — NIVEL FOLIAR. Teclado numérico con punto decimal; opcional (vacío = NULL). */
class MedVeg16Activity : AppCompatActivity() {

    private lateinit var binding: ActivityMedVeg16Binding
    private var valorActual = ""

    companion object { private const val MAX_LEN = 6 }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMedVeg16Binding.inflate(layoutInflater)
        setContentView(binding.root)
        actualizarDisplay()
        setupTeclado()
        binding.btnAccion.setOnClickListener {
            // Opcional: vacío viaja como "" y se guarda NULL en la base
            siguiente()
        }
    }

    private fun siguiente() {
        startActivity(Intent(this, MedVeg17Activity::class.java).also {
            intent.extras?.let { e -> it.putExtras(e) }
            it.putExtra("niv_foliar", valorActual)
        })
    }

    private fun setupTeclado() {
        val botones = mapOf(
            binding.btn0 to "0", binding.btn1 to "1", binding.btn2 to "2",
            binding.btn3 to "3", binding.btn4 to "4", binding.btn5 to "5",
            binding.btn6 to "6", binding.btn7 to "7", binding.btn8 to "8",
            binding.btn9 to "9"
        )
        botones.forEach { (btn, valor) ->
            btn.setOnClickListener {
                if (valorActual.length < MAX_LEN) { valorActual += valor; actualizarDisplay(); ocultarError() }
            }
        }
        // Un solo punto decimal, y nunca como primer carácter
        binding.btnPunto.setOnClickListener {
            if (valorActual.isNotEmpty() && !valorActual.contains(".") && valorActual.length < MAX_LEN) { valorActual += "."; actualizarDisplay() }
        }
        binding.btnC.setOnClickListener { valorActual = ""; actualizarDisplay(); ocultarError() }
        binding.btnDel.setOnClickListener {
            if (valorActual.isNotEmpty()) { valorActual = valorActual.dropLast(1); actualizarDisplay(); ocultarError() }
        }
    }

    private fun actualizarDisplay() { binding.tvDisplay.text = valorActual }
    private fun mostrarError(msg: String) { binding.tvError.text = msg; binding.tvError.visibility = View.VISIBLE }
    private fun ocultarError() { binding.tvError.visibility = View.GONE }
}
