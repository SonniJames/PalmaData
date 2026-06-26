package com.palmadata.app.tratamientos

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.palmadata.app.databinding.ActivityTrat9Binding
import com.palmadata.app.utils.DatabaseHelper
import com.palmadata.app.utils.SessionManager
import java.text.SimpleDateFormat
import java.util.*

class Trat9Activity : AppCompatActivity() {
    private lateinit var binding: ActivityTrat9Binding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTrat9Binding.inflate(layoutInflater)
        setContentView(binding.root)
        val plantacionId  = intent.getIntExtra("plantacion_id", 0)
        val loteId        = intent.getIntExtra("lote_id", 0)
        val linea         = intent.getStringExtra("linea") ?: ""
        val palma         = intent.getStringExtra("palma") ?: ""
        val tratamientoId = intent.getIntExtra("tratamiento_id", 0)
        val enfermedadId  = intent.getIntExtra("enfermedad_id", 0)
        val eventoId      = intent.getIntExtra("evento_id", 0)
        val cantidad      = intent.getStringExtra("cantidad") ?: ""
        binding.etObservaciones.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) { binding.tvContador.text = "${s?.length ?: 0}/500" }
        })
        binding.btnGuardar.setOnClickListener {
            guardarRegistro(plantacionId, loteId, linea, palma, tratamientoId, enfermedadId, eventoId, cantidad)
        }
    }

    private fun guardarRegistro(
        plantacionId: Int, loteId: Int, linea: String, palma: String,
        tratamientoId: Int, enfermedadId: Int, eventoId: Int, cantidad: String
    ) {
        val worker = SessionManager.getCurrentWorker(this)
        if (worker == null) {
            Toast.makeText(this, "Error: no hay trabajador en sesión", Toast.LENGTH_SHORT).show()
            return
        }
        val ahora = Date()
        val fmtFecha = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val fmtHora  = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val registro = TratamientoRegistro(
            id                = UUID.randomUUID().toString(),
            sanEventoTratId   = tratamientoId,
            auxTrabajadorId   = worker.code.toIntOrNull() ?: 0,
            fecha             = fmtFecha.format(ahora),
            hora              = fmtHora.format(ahora),
            catLoteId         = loteId.toLong(),
            catPalmaId        = 0.0,
            catPlantacionId   = plantacionId,
            linea             = linea.toIntOrNull() ?: 0,
            palma             = palma.toIntOrNull() ?: 0,
            sanEnfermedadesId = enfermedadId,
            sanEventoEnfId    = eventoId,
            observaciones     = binding.etObservaciones.text.toString(),
            latitud           = SessionManager.getLastLatitude(this),
            longitud          = SessionManager.getLastLongitude(this),
            cantidad          = cantidad.toDoubleOrNull() ?: 0.0,
            equipo            = SessionManager.getEquipoId(this)
        )
        try {
            DatabaseHelper(this).guardarTratamiento(registro)
            Toast.makeText(this, "✅ Registro guardado", Toast.LENGTH_SHORT).show()
            val opcionesIntent = Intent(this, TratOpcionesActivity::class.java)
            opcionesIntent.putExtra("plantacion_id", plantacionId)
            opcionesIntent.putExtra("lote_id",       loteId)
            startActivity(opcionesIntent)
            finish()
        } catch (e: Exception) {
            Toast.makeText(this, "❌ Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}