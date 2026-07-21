package com.palmadata.app.plagas

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.palmadata.app.MainActivity
import com.palmadata.app.databinding.ActivityPlagasOpcionesBinding

class PlagasOpcionesActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityPlagasOpcionesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val plantacionId = intent.getIntExtra("plantacion_id", 0)
        val loteId       = intent.getIntExtra("lote_id", 0)
        val lectura      = intent.getStringExtra("lectura") ?: ""
        val linea        = intent.getStringExtra("linea") ?: ""   // ← nuevo
        val palma        = intent.getStringExtra("palma") ?: ""   // ← nuevo

        binding.btnLineaPalma.setOnClickListener {
            val intent = Intent(this, Plagas4Activity::class.java)
            intent.putExtra("plantacion_id", plantacionId)
            intent.putExtra("lote_id",       loteId)
            intent.putExtra("lectura",       lectura)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }

        // ← nuevo botón
        binding.btnOtroInsecto.setOnClickListener {
            val intent = Intent(this, Plagas6Activity::class.java)
            intent.putExtra("plantacion_id", plantacionId)
            intent.putExtra("plantacion_nombre", "")
            intent.putExtra("sector_id", 0)
            intent.putExtra("sector_nombre", "")
            intent.putExtra("lote_id", loteId)
            intent.putExtra("lote_nombre", "")
            intent.putExtra("lectura", lectura)
            intent.putExtra("linea", linea)
            intent.putExtra("palma", palma)
            startActivity(intent)
            finish()
        }

            binding.btnModulos.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        super.onBackPressed()
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        startActivity(intent)
        finish()
    }
}