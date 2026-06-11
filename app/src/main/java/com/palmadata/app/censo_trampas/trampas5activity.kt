package com.palmadata.app.trampas

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.palmadata.app.databinding.ActivityTrampas5Binding

class Trampas5Activity : AppCompatActivity() {
    private lateinit var binding: ActivityTrampas5Binding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTrampas5Binding.inflate(layoutInflater)
        setContentView(binding.root)
        val plantacionId     = intent.getIntExtra("plantacion_id", 0)
        val plantacionNombre = intent.getStringExtra("plantacion_nombre") ?: ""
        val lectura          = intent.getStringExtra("lectura") ?: ""
        val trampaId         = intent.getIntExtra("trampa_id", 0)
        val trampaCodigo     = intent.getStringExtra("trampa_codigo") ?: ""
        val machos           = intent.getStringExtra("machos") ?: "0"
        val hembras          = intent.getStringExtra("hembras") ?: "0"

        binding.btnAccion.setOnClickListener {
            val sanTipoTrampa = if (binding.cbFeromona.isChecked) 1 else 0
            val feromona      = if (binding.cbCambioFeromona.isChecked) "si" else "no"
            val atrayente     = if (binding.cbCambioAtrayente.isChecked) 1 else 0

            startActivity(Intent(this, Trampas6Activity::class.java).also {
                it.putExtra("plantacion_id", plantacionId)
                it.putExtra("plantacion_nombre", plantacionNombre)
                it.putExtra("lectura", lectura)
                it.putExtra("trampa_id", trampaId)
                it.putExtra("trampa_codigo", trampaCodigo)
                it.putExtra("machos", machos)
                it.putExtra("hembras", hembras)
                it.putExtra("san_tipo_trampa", sanTipoTrampa)
                it.putExtra("feromona", feromona)
                it.putExtra("atrayente", atrayente)
            })
        }
    }
}