package com.palmadata.app.med_veg

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.palmadata.app.databinding.ActivityMedVeg15Binding
import com.palmadata.app.utils.aDecimalCampo

/**
 * Pantalla 15 — FOLIOLO 8. Cajas decimales (solo dígitos y punto, por
 * android:digits del layout: la coma del teclado no escribe nada). Vacío = 0.
 */
class MedVeg15Activity : AppCompatActivity() {
    private lateinit var binding: ActivityMedVeg15Binding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMedVeg15Binding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btnAccion.setOnClickListener {
            startActivity(Intent(this, MedVeg16Activity::class.java).also {
                intent.extras?.let { e -> it.putExtras(e) }
                it.putExtra("ancho_8", binding.etAncho8.text.toString().aDecimalCampo() ?: 0.0)
                it.putExtra("largo_8", binding.etLargo8.text.toString().aDecimalCampo() ?: 0.0)
            })
        }
    }
}
