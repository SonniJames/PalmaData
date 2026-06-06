package com.palmadata.app.strategus

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class StrategusActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivity(Intent(this, Strategus0Activity::class.java))
        finish()
    }
}