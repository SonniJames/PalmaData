package com.palmadata.app.med_veg

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.palmadata.app.databinding.ActivityMedVeg13Binding
import com.palmadata.app.utils.aDecimalCampo

/**
 * Pantalla 13 — FOLIOLO 6. Cajas decimales (solo dígitos y punto, por
 * android:digits del layout: la coma del teclado no escribe nada). Vacío = 0.
 */
class MedVeg13Activity : AppCompatActivity() {
    private lateinit var binding: ActivityMedVeg13Binding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMedVeg13Binding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btnAccion.setOnClickListener {
            startActivity(Intent(this, MedVeg14Activity::class.java).also {
                intent.extras?.let { e -> it.putExtras(e) }
                it.putExtra("ancho_6", binding.etAncho6.text.toString().aDecimalCampo() ?: 0.0)
                it.putExtra("largo_6", binding.etLargo6.text.toString().aDecimalCampo() ?: 0.0)
            })
        }
    }
}
