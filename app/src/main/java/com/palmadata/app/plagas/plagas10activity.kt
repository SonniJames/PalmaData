package com.palmadata.app.plagas

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.palmadata.app.databinding.ActivityPlagas10Binding

class Plagas10Activity : AppCompatActivity() {
    private lateinit var binding: ActivityPlagas10Binding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlagas10Binding.inflate(layoutInflater)
        setContentView(binding.root)
        val plantacionId    = intent.getIntExtra("plantacion_id", 0)
        val loteId          = intent.getIntExtra("lote_id", 0)
        val lectura         = intent.getStringExtra("lectura") ?: ""
        val linea           = intent.getStringExtra("linea") ?: ""
        val palma           = intent.getStringExtra("palma") ?: ""
        val insectoId       = intent.getIntExtra("insecto_id", 0)
        val estadoInsectoId = intent.getIntExtra("estado_insecto_id", 0)
        val cantidad        = intent.getStringExtra("cantidad") ?: "0"
        val nivFoliar       = intent.getStringExtra("niv_foliar") ?: "0"

        binding.btnAccion.setOnClickListener {
            val defol5  = binding.etDefol5.text.toString().toDoubleOrNull() ?: 0.0
            val defol13 = binding.etDefol13.text.toString().toDoubleOrNull() ?: 0.0
            val defol21 = binding.etDefol21.text.toString().toDoubleOrNull() ?: 0.0
            val defol29 = binding.etDefol29.text.toString().toDoubleOrNull() ?: 0.0
            val defol37 = binding.etDefol37.text.toString().toDoubleOrNull() ?: 0.0

            startActivity(Intent(this, Plagas11Activity::class.java).also {
                it.putExtra("plantacion_id", plantacionId); it.putExtra("lote_id", loteId)
                it.putExtra("lectura", lectura); it.putExtra("linea", linea); it.putExtra("palma", palma)
                it.putExtra("insecto_id", insectoId); it.putExtra("estado_insecto_id", estadoInsectoId)
                it.putExtra("cantidad", cantidad); it.putExtra("niv_foliar", nivFoliar)
                it.putExtra("defol5", defol5); it.putExtra("defol13", defol13)
                it.putExtra("defol21", defol21); it.putExtra("defol29", defol29)
                it.putExtra("defol37", defol37)
            })
        }
    }
}