package com.palmadata.app.polinizacion

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.palmadata.app.databinding.ActivityPoli6Binding
import com.palmadata.app.utils.DatabaseHelper
import com.palmadata.app.utils.SessionManager
import java.text.SimpleDateFormat
import java.util.*

class Poli6Activity : AppCompatActivity() {
    private lateinit var binding: ActivityPoli6Binding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPoli6Binding.inflate(layoutInflater)
        setContentView(binding.root)
        val plantacionId = intent.getIntExtra("plantacion_id", 0)
        val loteId       = intent.getIntExtra("lote_id", 0)
        val aplicacion1  = intent.getIntExtra("aplicacion1", 0)
        val aplicacion2  = intent.getIntExtra("aplicacion2", 0)
        val aplicacion3  = intent.getIntExtra("aplicacion3", 0)
        val linea        = intent.getStringExtra("linea") ?: ""
        val palma        = intent.getStringExtra("palma") ?: ""

        binding.etObservaciones.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) { binding.tvContador.text = "${s?.length ?: 0}/255" }
        })

        binding.btnGuardar.setOnClickListener {
            guardarRegistro(plantacionId, loteId, aplicacion1, aplicacion2, aplicacion3, linea, palma)
        }
    }

    private fun guardarRegistro(
        plantacionId: Int, loteId: Int,
        aplicacion1: Int, aplicacion2: Int, aplicacion3: Int,
        linea: String, palma: String
    ) {
        val worker = SessionManager.getCurrentWorker(this)
        if (worker == null) {
            Toast.makeText(this, "Error: no hay trabajador en sesión", Toast.LENGTH_SHORT).show()
            return
        }
        val ahora    = Date()
        val fmtFecha = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val fmtHora  = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

        val registro = PolinizacionRegistro(
            id              = UUID.randomUUID().toString(),
            fecha           = fmtFecha.format(ahora),
            hora            = fmtHora.format(ahora),
            linea           = linea.toIntOrNull() ?: 0,
            palma           = palma.toIntOrNull() ?: 0,
            catLoteId       = loteId.toLong(),
            catPalmaId      = 0L,
            catPlantacionId = plantacionId,
            polinizador     = worker.code.toIntOrNull() ?: 0,
            aplicacion1     = aplicacion1,
            aplicacion2     = aplicacion2,
            aplicacion3     = aplicacion3,
            observaciones   = binding.etObservaciones.text.toString(),
            latitud         = SessionManager.getLastLatitude(this),
            longitud        = SessionManager.getLastLongitude(this),
            equipo          = SessionManager.getEquipoId(this)
        )

        try {
            DatabaseHelper(this).guardarPolinizacion(registro)
            Toast.makeText(this, "✅ Registro guardado", Toast.LENGTH_SHORT).show()
            val opcionesIntent = Intent(this, PoliOpcionesActivity::class.java)
            opcionesIntent.putExtra("plantacion_id", plantacionId)
            opcionesIntent.putExtra("lote_id",       loteId)
            startActivity(opcionesIntent)
            finish()
        } catch (e: Exception) {
            Toast.makeText(this, "❌ Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}