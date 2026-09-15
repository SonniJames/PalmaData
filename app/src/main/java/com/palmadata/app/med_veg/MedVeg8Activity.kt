package com.palmadata.app.med_veg

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.palmadata.app.databinding.ActivityMedVeg8Binding
import com.palmadata.app.utils.aDecimalCampo

/**
 * Pantalla 8 — FOLIOLO 1. Cajas decimales (solo dígitos y punto, por
 * android:digits del layout: la coma del teclado no escribe nada). Vacío = 0.
 */
class MedVeg8Activity : AppCompatActivity() {
    private lateinit var binding: ActivityMedVeg8Binding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMedVeg8Binding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btnAccion.setOnClickListener {
            startActivity(Intent(this, MedVeg9Activity::class.java).also {
                intent.extras?.let { e -> it.putExtras(e) }
                it.putExtra("ancho_1", binding.etAncho1.text.toString().aDecimalCampo() ?: 0.0)
                it.putExtra("largo_1", binding.etLargo1.text.toString().aDecimalCampo() ?: 0.0)
            })
        }
    }
}
