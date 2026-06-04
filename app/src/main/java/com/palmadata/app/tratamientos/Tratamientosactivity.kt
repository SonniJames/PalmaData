package com.palmadata.app.tratamientos

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class TratamientosActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivity(Intent(this, Trat0Activity::class.java))
        finish()
    }
}