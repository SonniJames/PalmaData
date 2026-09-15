package com.palmadata.app.med_veg

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.palmadata.app.databinding.ActivityMedVeg10Binding
import com.palmadata.app.utils.aDecimalCampo

/**
 * Pantalla 10 — FOLIOLO 3. Cajas decimales (solo dígitos y punto, por
 * android:digits del layout: la coma del teclado no escribe nada). Vacío = 0.
 */
class MedVeg10Activity : AppCompatActivity() {
    private lateinit var binding: ActivityMedVeg10Binding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMedVeg10Binding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btnAccion.setOnClickListener {
            startActivity(Intent(this, MedVeg11Activity::class.java).also {
                intent.extras?.let { e -> it.putExtras(e) }
                it.putExtra("ancho_3", binding.etAncho3.text.toString().aDecimalCampo() ?: 0.0)
                it.putExtra("largo_3", binding.etLargo3.text.toString().aDecimalCampo() ?: 0.0)
            })
        }
    }
}
