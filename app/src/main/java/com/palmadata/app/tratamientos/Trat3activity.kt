package com.palmadata.app.tratamientos

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.palmadata.app.databinding.ActivityTrat3Binding

class Trat3Activity : AppCompatActivity() {
    private lateinit var binding: ActivityTrat3Binding
    private var valorActual = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTrat3Binding.inflate(layoutInflater)
        setContentView(binding.root)
        valorActual = ""
        actualizarDisplay()

        val plantacionId     = intent.getIntExtra("plantacion_id", 0)
        val plantacionNombre = intent.getStringExtra("plantacion_nombre") ?: ""
        val sectorId         = intent.getIntExtra("sector_id", 0)
        val sectorNombre     = intent.getStringExtra("sector_nombre") ?: ""
        val loteId           = intent.getIntExtra("lote_id", 0)
        val loteNombre       = intent.getStringExtra("lote_nombre") ?: ""
        setupTeclado()
        binding.btnAccion.setOnClickListener {
            when {
                valorActual.isEmpty()  -> mostrarError("Debe ingresar un valor para LÍNEA")
                valorActual.length > 3 -> mostrarError("LÍNEA debe tener máximo 3 dígitos")
                else -> {
                    val i = Intent(this, Trat4Activity::class.java)
                    i.putExtra("plantacion_id", plantacionId)
                    i.putExtra("plantacion_nombre", plantacionNombre)
                    i.putExtra("sector_id", sectorId)
                    i.putExtra("sector_nombre", sectorNombre)
                    i.putExtra("lote_id", loteId)
                    i.putExtra("lote_nombre", loteNombre)
                    i.putExtra("linea", valorActual)
                    startActivity(i)
                }
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