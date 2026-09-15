package com.palmadata.app.med_veg

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

/** Entrada del módulo MEDIDAS VEGETATIVAS (formulario 40): lanza la pantalla 0. */
class MedVegActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivity(Intent(this, MedVeg0Activity::class.java))
        finish()
    }
}
