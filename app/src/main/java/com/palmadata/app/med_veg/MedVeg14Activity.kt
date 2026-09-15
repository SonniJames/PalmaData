package com.palmadata.app.med_veg

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.palmadata.app.databinding.ActivityMedVeg14Binding
import com.palmadata.app.utils.aDecimalCampo

/**
 * Pantalla 14 — FOLIOLO 7. Cajas decimales (solo dígitos y punto, por
 * android:digits del layout: la coma del teclado no escribe nada). Vacío = 0.
 */
class MedVeg14Activity : AppCompatActivity() {
    private lateinit var binding: ActivityMedVeg14Binding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMedVeg14Binding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btnAccion.setOnClickListener {
            startActivity(Intent(this, MedVeg15Activity::class.java).also {
                intent.extras?.let { e -> it.putExtras(e) }
                it.putExtra("ancho_7", binding.etAncho7.text.toString().aDecimalCampo() ?: 0.0)
                it.putExtra("largo_7", binding.etLargo7.text.toString().aDecimalCampo() ?: 0.0)
            })
        }
    }
}
