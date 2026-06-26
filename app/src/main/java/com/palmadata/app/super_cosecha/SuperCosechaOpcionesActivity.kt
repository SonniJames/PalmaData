package com.palmadata.app.supercosecha

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.palmadata.app.MainActivity
import com.palmadata.app.databinding.ActivitySuperCosechaOpcionesBinding

class SuperCosechaOpcionesActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivitySuperCosechaOpcionesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val plantacionId = intent.getIntExtra("plantacion_id", 0)
        val loteId       = intent.getIntExtra("lote_id", 0)
        val ciclo        = intent.getStringExtra("ciclo") ?: "0"
        val cortadorId   = intent.getIntExtra("cortador_id", 0)
        val recolectorId = intent.getIntExtra("recolector_id", 0)

        // LÍNEA-PALMA → SuperCosecha5Activity (conserva plantacion, lote, ciclo, cortador, recolector)
        binding.btnLineaPalma.setOnClickListener {
            val intent = Intent(this, SuperCosecha5Activity::class.java)
            intent.putExtra("plantacion_id",  plantacionId)
            intent.putExtra("lote_id",        loteId)
            intent.putExtra("ciclo",          ciclo)
            intent.putExtra("cortador_id",    cortadorId)
            intent.putExtra("recolector_id",  recolectorId)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        // CORTADOR/RECOLECTOR → SuperCosecha4Activity (conserva plantacion, lote, ciclo)
        binding.btnCortadorRecolector.setOnClickListener {
            val intent = Intent(this, SuperCosecha4Activity::class.java)
            intent.putExtra("plantacion_id", plantacionId)
            intent.putExtra("lote_id",       loteId)
            intent.putExtra("ciclo",         ciclo)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
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