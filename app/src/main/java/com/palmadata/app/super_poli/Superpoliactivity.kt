package com.palmadata.app.super_poli

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class SuperPoliActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivity(Intent(this, SuperPoli0Activity::class.java))
        finish()
    }
}