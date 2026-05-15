package com.palmadata.app.censo_enfermedades

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.palmadata.app.databinding.ActivityCensoEnf4Binding

class CensoEnf4Activity : AppCompatActivity() {

    private lateinit var binding: ActivityCensoEnf4Binding
    private var valorActual = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCensoEnf4Binding.inflate(layoutInflater)
        setContentView(binding.root)

        val sector = intent.getStringExtra("sector") ?: ""
        val lote   = intent.getStringExtra("lote") ?: ""
        val censo  = intent.getStringExtra("censo") ?: ""

        setupTeclado()

        binding.btnAccion.setOnClickListener {
            when {
                valorActual.isEmpty() -> mostrarError("Debe ingresar un valor para LÍNEA")
                valorActual.length > 2 -> mostrarError("LÍNEA debe tener máximo 2 dígitos")
                else -> {
                    val intent = Intent(this, CensoEnf5Activity::class.java)
                    intent.putExtra("sector", sector)
                    intent.putExtra("lote", lote)
                    intent.putExtra("censo", censo)
                    intent.putExtra("linea", valorActual)
                    startActivity(intent)
                }
            }
        }
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
                if (valorActual.length < 2) {
                    valorActual += valor
                    actualizarDisplay()
                }
            }
        }

        binding.btnC.setOnClickListener { valorActual = ""; actualizarDisplay(); ocultarError() }
        binding.btnDel.setOnClickListener {
            if (valorActual.isNotEmpty()) {
                valorActual = valorActual.dropLast(1)
                actualizarDisplay()
                ocultarError()
            }
        }
    }

    private fun actualizarDisplay() { binding.tvDisplay.text = valorActual }
    private fun mostrarError(msg: String) { binding.tvError.text = msg; binding.tvError.visibility = View.VISIBLE }
    private fun ocultarError() { binding.tvError.visibility = View.GONE }
}