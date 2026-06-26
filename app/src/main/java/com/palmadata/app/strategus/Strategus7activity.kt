package com.palmadata.app.strategus

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.palmadata.app.databinding.ActivityStrategus7Binding
import com.palmadata.app.utils.DatabaseHelper
import com.palmadata.app.utils.SessionManager
import java.text.SimpleDateFormat
import java.util.*

class Strategus7Activity : AppCompatActivity() {
    private lateinit var binding: ActivityStrategus7Binding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStrategus7Binding.inflate(layoutInflater)
        setContentView(binding.root)
        val plantacionId = intent.getIntExtra("plantacion_id", 0)
        val loteId       = intent.getIntExtra("lote_id", 0)
        val censo        = intent.getStringExtra("censo") ?: ""
        val linea        = intent.getStringExtra("linea") ?: ""
        val palma        = intent.getStringExtra("palma") ?: ""
        val galerias     = intent.getStringExtra("galerias") ?: "0"
        binding.etObservaciones.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) { binding.tvContador.text = "${s?.length ?: 0}/255" }
        })
        binding.btnGuardar.setOnClickListener {
            guardarRegistro(plantacionId, loteId, censo, linea, palma, galerias)
        }
    }

    private fun guardarRegistro(
        plantacionId: Int, loteId: Int, censo: String,
        linea: String, palma: String, galerias: String
    ) {
        val worker = SessionManager.getCurrentWorker(this)
        if (worker == null) {
            Toast.makeText(this, "Error: no hay trabajador en sesión", Toast.LENGTH_SHORT).show()
            return
        }
        val ahora    = Date()
        val fmtFecha = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val fmtHora  = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val registro = StrategusRegistro(
            id              = UUID.randomUUID().toString(),
            fecha           = fmtFecha.format(ahora),
            hora            = fmtHora.format(ahora),
            catLoteId       = loteId.toLong(),
            linea           = linea.toIntOrNull() ?: 0,
            palma           = palma.toIntOrNull() ?: 0,
            catPalmaId      = 0L,
            galerias        = galerias.toIntOrNull() ?: 0,
            censo           = censo.toLongOrNull() ?: 0L,
            evaluador       = worker.code.toIntOrNull() ?: 0,
            catPlantacionId = plantacionId,
            observaciones   = binding.etObservaciones.text.toString(),
            latitud         = SessionManager.getLastLatitude(this),
            longitud        = SessionManager.getLastLongitude(this),
            equipo          = SessionManager.getEquipoId(this)
        )
        try {
            DatabaseHelper(this).guardarStrategus(registro)
            Toast.makeText(this, "✅ Registro guardado", Toast.LENGTH_SHORT).show()
            val opcionesIntent = Intent(this, StrategusOpcionesActivity::class.java)
            opcionesIntent.putExtra("plantacion_id", plantacionId)
            opcionesIntent.putExtra("lote_id",       loteId)
            opcionesIntent.putExtra("censo",         censo)
            startActivity(opcionesIntent)
            finish()
        } catch (e: Exception) {
            Toast.makeText(this, "❌ Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}