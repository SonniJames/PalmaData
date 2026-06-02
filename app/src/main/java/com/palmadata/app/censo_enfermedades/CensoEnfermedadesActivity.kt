package com.palmadata.app.censo_enfermedades

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class CensoEnfermedadesActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Lanza directamente la primera pantalla del flujo
        startActivity(Intent(this, CensoEnf0Activity::class.java))
        finish()
    }
}