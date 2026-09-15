package com.palmadata.app.med_veg

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.palmadata.app.databinding.ActivityMedVeg17Binding

/** Pantalla 17 — HOJA. Teclado numérico; opcional (vacío = NULL). */
class MedVeg17Activity : AppCompatActivity() {

    private lateinit var binding: ActivityMedVeg17Binding
    private var valorActual = ""

    companion object { private const val MAX_LEN = 3 }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMedVeg17Binding.inflate(layoutInflater)
        setContentView(binding.root)
        actualizarDisplay()
        setupTeclado()
        binding.btnAccion.setOnClickListener {
            // Opcional: vacío viaja como "" y se guarda NULL en la base
            siguiente()
        }
    }

    private fun siguiente() {
        startActivity(Intent(this, MedVeg18Activity::class.java).also {
            intent.extras?.let { e -> it.putExtras(e) }
            it.putExtra("hoja", valorActual)
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
        binding.btnC.setOnClickListener { valorActual = ""; actualizarDisplay(); ocultarError() }
        binding.btnDel.setOnClickListener {
            if (valorActual.isNotEmpty()) { valorActual = valorActual.dropLast(1); actualizarDisplay(); ocultarError() }
        }
    }

    private fun actualizarDisplay() { binding.tvDisplay.text = valorActual }
    private fun mostrarError(msg: String) { binding.tvError.text = msg; binding.tvError.visibility = View.VISIBLE }
    private fun ocultarError() { binding.tvError.visibility = View.GONE }
}
