package com.palmadata.app.plagas

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class PlagasActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivity(Intent(this, Plagas0Activity::class.java))
        finish()
    }
}