package com.palmadata.app.super_poli

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.palmadata.app.MainActivity
import com.palmadata.app.databinding.ActivitySuperPoliOpcionesBinding

class SuperPoliOpcionesActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivitySuperPoliOpcionesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val plantacionId     = intent.getIntExtra("plantacion_id", 0)
        val plantacionNombre = intent.getStringExtra("plantacion_nombre") ?: ""
        val sectorId         = intent.getIntExtra("sector_id", 0)
        val sectorNombre     = intent.getStringExtra("sector_nombre") ?: ""
        val loteId           = intent.getIntExtra("lote_id", 0)
        val loteNombre       = intent.getStringExtra("lote_nombre") ?: ""

        // NUEVO REGISTRO → pantalla 3 (selector de flujo), conservando
        // plantación, sector y lote. Los flags limpian la pila del registro
        // anterior para que el botón atrás siga la navegación natural.
        binding.btnNuevoRegistro.setOnClickListener {
            val intent = Intent(this, SuperPoli3Activity::class.java)
            intent.putExtra("plantacion_id", plantacionId)
            intent.putExtra("plantacion_nombre", plantacionNombre)
            intent.putExtra("sector_id", sectorId)
            intent.putExtra("sector_nombre", sectorNombre)
            intent.putExtra("lote_id", loteId)
            intent.putExtra("lote_nombre", loteNombre)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
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