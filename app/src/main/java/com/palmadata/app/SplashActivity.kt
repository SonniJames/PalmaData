package com.palmadata.app

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.palmadata.app.utils.ServerConfig

/**
 * SplashActivity: punto de entrada de la app.
 * Decide si mostrar la configuración inicial o ir directo al MainActivity.
 */
class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val destino = if (ServerConfig.estaConfigurado(this)) {
            // Ya configurado → ir al MainActivity
            Intent(this, MainActivity::class.java)
        } else {
            // Primer inicio → ir a configuración
            Intent(this, SetupActivity::class.java)
        }

        startActivity(destino)
        finish()
    }
}