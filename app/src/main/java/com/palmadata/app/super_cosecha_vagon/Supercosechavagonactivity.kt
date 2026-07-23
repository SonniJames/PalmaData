package com.palmadata.app.super_cosecha_vagon

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class SuperCosechaVagonActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivity(Intent(this, SuperCosechaVagon0Activity::class.java))
        finish()
    }
}