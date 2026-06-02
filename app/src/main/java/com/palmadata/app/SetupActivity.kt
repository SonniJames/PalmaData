package com.palmadata.app

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.palmadata.app.databinding.ActivitySetupBinding
import com.palmadata.app.utils.ServerConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

class SetupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySetupBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySetupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnConectar.setOnClickListener {
            val ip     = binding.etIp.text.toString().trim()
            val puerto = binding.etPuerto.text.toString().trim()

            // Validaciones básicas
            if (ip.isEmpty()) {
                mostrarError("Ingrese la dirección IP")
                return@setOnClickListener
            }
            if (puerto.isEmpty()) {
                mostrarError("Ingrese el puerto")
                return@setOnClickListener
            }

            ocultarTeclado()
            probarConexion(ip, puerto)
        }
    }

    private fun probarConexion(ip: String, puerto: String) {
        // Mostrar progreso
        binding.progressBar.visibility = View.VISIBLE
        binding.btnConectar.isEnabled  = false
        binding.tvError.visibility     = View.GONE
        binding.tvExito.visibility     = View.GONE

        val url = "http://$ip:$puerto/ping"

        lifecycleScope.launch {
            val resultado = withContext(Dispatchers.IO) {
                intentarConexion(url)
            }

            binding.progressBar.visibility = View.GONE
            binding.btnConectar.isEnabled  = true

            if (resultado.exito) {
                // Guardar configuración
                ServerConfig.guardar(this@SetupActivity, ip, puerto)

                binding.tvExito.text       = "✅ Conexión exitosa. Entrando..."
                binding.tvExito.visibility = View.VISIBLE

                // Esperar 1 segundo y pasar al MainActivity
                binding.root.postDelayed({
                    startActivity(Intent(this@SetupActivity, MainActivity::class.java))
                    finish()
                }, 1000)

            } else {
                mostrarError("❌ No se pudo conectar a $ip:$puerto\n${resultado.mensaje}")
            }
        }
    }

    private fun intentarConexion(urlString: String): ResultadoConexion {
        return try {
            val url        = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 5000   // 5 segundos
            connection.readTimeout    = 5000
            connection.requestMethod  = "GET"
            connection.connect()

            val code = connection.responseCode
            connection.disconnect()

            if (code == 200) {
                ResultadoConexion(exito = true)
            } else {
                ResultadoConexion(exito = false, mensaje = "El servidor respondió con código $code")
            }
        } catch (e: Exception) {
            ResultadoConexion(exito = false, mensaje = e.message ?: "Error desconocido")
        }
    }

    private fun mostrarError(mensaje: String) {
        binding.tvError.text       = mensaje
        binding.tvError.visibility = View.VISIBLE
        binding.tvExito.visibility = View.GONE
    }

    private fun ocultarTeclado() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.root.windowToken, 0)
    }

    data class ResultadoConexion(
        val exito: Boolean,
        val mensaje: String = ""
    )
}