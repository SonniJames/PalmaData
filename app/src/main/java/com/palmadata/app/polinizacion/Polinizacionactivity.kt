package com.palmadata.app.polinizacion

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class PolinizacionActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivity(Intent(this, Poli0Activity::class.java))
        finish()
    }
}