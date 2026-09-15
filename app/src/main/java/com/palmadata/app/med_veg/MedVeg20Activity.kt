package com.palmadata.app.med_veg

import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.palmadata.app.MainActivity
import com.palmadata.app.databinding.ActivityMedVeg20Binding

/**
 * Pantalla 20 — REGISTRO GUARDADO. NUEVO REGISTRO vuelve a la pantalla 3 (UMA)
 * reutilizando la instancia que sigue en la pila (CLEAR_TOP | SINGLE_TOP), con
 * plantación, sector y lote ya cargados; atrás desde ahí recorre 2 → 1 → 0.
 * MÓDULOS y el botón atrás vuelven a MainActivity, que pone formulario = 0.
 */
class MedVeg20Activity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityMedVeg20Binding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnNuevoRegistro.setOnClickListener {
            startActivity(Intent(this, MedVeg3Activity::class.java).also {
                intent.extras?.let { e -> it.putExtras(e) }
                it.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            })
            finish()
        }

        binding.btnModulos.setOnClickListener { irAModulos() }

        // Atrás hace lo mismo que MÓDULOS. Se registra en el dispatcher en vez
        // de sobreescribir onBackPressed(): ese método ya no se invoca con los
        // gestos de navegación de Android 13+, así que el comportamiento se
        // perdería en los equipos nuevos sin dar ningún aviso.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { irAModulos() }
        })
    }

    private fun irAModulos() {
        val i = Intent(this, MainActivity::class.java)
        i.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        startActivity(i)
        finish()
    }
}