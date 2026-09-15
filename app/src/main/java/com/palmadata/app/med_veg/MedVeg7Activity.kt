package com.palmadata.app.med_veg

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.palmadata.app.databinding.ActivityMedVeg7Binding
import com.palmadata.app.utils.aDecimalCampo

/**
 * Pantalla 7 — INF. PECIOLO. Cajas decimales (solo dígitos y punto, por
 * android:digits del layout: la coma del teclado no escribe nada). Vacío = 0.
 */
class MedVeg7Activity : AppCompatActivity() {
    private lateinit var binding: ActivityMedVeg7Binding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMedVeg7Binding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btnAccion.setOnClickListener {
            startActivity(Intent(this, MedVeg8Activity::class.java).also {
                intent.extras?.let { e -> it.putExtras(e) }
                it.putExtra("long_peciolo", binding.etLongPeciolo.text.toString().aDecimalCampo() ?: 0.0)
                it.putExtra("anch_peciolo", binding.etAnchPeciolo.text.toString().aDecimalCampo() ?: 0.0)
                it.putExtra("prof_peciolo", binding.etProfPeciolo.text.toString().aDecimalCampo() ?: 0.0)
                it.putExtra("long_raquis", binding.etLongRaquis.text.toString().aDecimalCampo() ?: 0.0)
            })
        }
    }
}
