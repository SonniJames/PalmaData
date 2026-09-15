package com.palmadata.app.med_veg

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.palmadata.app.databinding.ActivityMedVeg9Binding
import com.palmadata.app.utils.aDecimalCampo

/**
 * Pantalla 9 — FOLIOLO 2. Cajas decimales (solo dígitos y punto, por
 * android:digits del layout: la coma del teclado no escribe nada). Vacío = 0.
 */
class MedVeg9Activity : AppCompatActivity() {
    private lateinit var binding: ActivityMedVeg9Binding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMedVeg9Binding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btnAccion.setOnClickListener {
            startActivity(Intent(this, MedVeg10Activity::class.java).also {
                intent.extras?.let { e -> it.putExtras(e) }
                it.putExtra("ancho_2", binding.etAncho2.text.toString().aDecimalCampo() ?: 0.0)
                it.putExtra("largo_2", binding.etLargo2.text.toString().aDecimalCampo() ?: 0.0)
            })
        }
    }
}
