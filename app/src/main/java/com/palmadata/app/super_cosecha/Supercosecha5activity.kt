package com.palmadata.app.supercosecha

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.palmadata.app.databinding.ActivitySuperCosecha5Binding

class SuperCosecha5Activity : AppCompatActivity() {
    private lateinit var binding: ActivitySuperCosecha5Binding
    private var valorActual = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySuperCosecha5Binding.inflate(layoutInflater)
        setContentView(binding.root)
        val plantacionId  = intent.getIntExtra("plantacion_id", 0)
        val loteId        = intent.getIntExtra("lote_id", 0)
        val ciclo         = intent.getStringExtra("ciclo") ?: "0"
        val cortadorId    = intent.getIntExtra("cortador_id", 0)
        val recolectorId  = intent.getIntExtra("recolector_id", 0)
        setupTeclado()
        binding.btnAccion.setOnClickListener {
            when {
                valorActual.isEmpty()  -> mostrarError("Debe ingresar un valor para LÍNEA")
                valorActual.length > 3 -> mostrarError("LÍNEA debe tener máximo 3 dígitos")
                else -> startActivity(Intent(this, SuperCosecha6Activity::class.java).also {
                    it.putExtra("plantacion_id", plantacionId); it.putExtra("lote_id", loteId)
                    it.putExtra("ciclo", ciclo); it.putExtra("cortador_id", cortadorId)
                    it.putExtra("recolector_id", recolectorId); it.putExtra("linea", valorActual)
                })
            }
        }
    }
    private fun setupTeclado() {
        val botones = mapOf(binding.btn0 to "0", binding.btn1 to "1", binding.btn2 to "2", binding.btn3 to "3", binding.btn4 to "4", binding.btn5 to "5", binding.btn6 to "6", binding.btn7 to "7", binding.btn8 to "8", binding.btn9 to "9")
        botones.forEach { (btn, v) -> btn.setOnClickListener { if (valorActual.length < 3) { valorActual += v; actualizarDisplay() } } }
        binding.btnC.setOnClickListener { valorActual = ""; actualizarDisplay(); ocultarError() }
        binding.btnDel.setOnClickListener { if (valorActual.isNotEmpty()) { valorActual = valorActual.dropLast(1); actualizarDisplay(); ocultarError() } }
    }
    private fun actualizarDisplay() { binding.tvDisplay.text = valorActual }
    private fun mostrarError(msg: String) { binding.tvError.text = msg; binding.tvError.visibility = View.VISIBLE }
    private fun ocultarError() { binding.tvError.visibility = View.GONE }
}