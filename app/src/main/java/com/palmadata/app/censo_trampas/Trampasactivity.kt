package com.palmadata.app.trampas

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class TrampasActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivity(Intent(this, Trampas0Activity::class.java))
        finish()
    }
}