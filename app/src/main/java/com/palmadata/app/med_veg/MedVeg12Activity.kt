package com.palmadata.app.med_veg

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.palmadata.app.databinding.ActivityMedVeg12Binding
import com.palmadata.app.utils.aDecimalCampo

/**
 * Pantalla 12 — FOLIOLO 5. Cajas decimales (solo dígitos y punto, por
 * android:digits del layout: la coma del teclado no escribe nada). Vacío = 0.
 */
class MedVeg12Activity : AppCompatActivity() {
    private lateinit var binding: ActivityMedVeg12Binding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMedVeg12Binding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btnAccion.setOnClickListener {
            startActivity(Intent(this, MedVeg13Activity::class.java).also {
                intent.extras?.let { e -> it.putExtras(e) }
                it.putExtra("ancho_5", binding.etAncho5.text.toString().aDecimalCampo() ?: 0.0)
                it.putExtra("largo_5", binding.etLargo5.text.toString().aDecimalCampo() ?: 0.0)
            })
        }
    }
}
