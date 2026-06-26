package com.palmadata.app.trampas

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.palmadata.app.databinding.ActivityTrampas6Binding
import com.palmadata.app.utils.DatabaseHelper
import com.palmadata.app.utils.SessionManager
import java.text.SimpleDateFormat
import java.util.*

class Trampas6Activity : AppCompatActivity() {
    private lateinit var binding: ActivityTrampas6Binding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTrampas6Binding.inflate(layoutInflater)
        setContentView(binding.root)
        val plantacionId  = intent.getIntExtra("plantacion_id", 0)
        val lectura       = intent.getStringExtra("lectura") ?: ""
        val trampaId      = intent.getIntExtra("trampa_id", 0)
        val machos        = intent.getStringExtra("machos") ?: "0"
        val hembras       = intent.getStringExtra("hembras") ?: "0"
        val sanTipoTrampa = intent.getIntExtra("san_tipo_trampa", 0)
        val feromona      = intent.getStringExtra("feromona") ?: "no"
        val atrayente     = intent.getIntExtra("atrayente", 0)

        binding.etObservaciones.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) { binding.tvContador.text = "${s?.length ?: 0}/255" }
        })

        binding.btnGuardar.setOnClickListener {
            guardarRegistro(plantacionId, lectura, trampaId, machos, hembras, sanTipoTrampa, feromona, atrayente)
        }
    }

    private fun guardarRegistro(
        plantacionId: Int, lectura: String, trampaId: Int,
        machos: String, hembras: String, sanTipoTrampa: Int,
        feromona: String, atrayente: Int
    ) {
        val worker = SessionManager.getCurrentWorker(this)
        if (worker == null) {
            Toast.makeText(this, "Error: no hay trabajador en sesión", Toast.LENGTH_SHORT).show()
            return
        }
        val ahora    = Date()
        val fmtFecha = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val fmtHora  = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

        val registro = TrampasRegistro(
            id              = UUID.randomUUID().toString(),
            fecha           = fmtFecha.format(ahora),
            hora            = fmtHora.format(ahora),
            lectura         = lectura.toLongOrNull() ?: 0L,
            censador        = worker.code.toIntOrNull() ?: 0,
            machos          = machos.toIntOrNull() ?: 0,
            hembras         = hembras.toIntOrNull() ?: 0,
            sanTrampaId     = trampaId,
            sanTipoTrampa   = sanTipoTrampa,
            catPlantacionId = plantacionId,
            atrayente       = atrayente,
            feromona        = feromona,
            observaciones   = binding.etObservaciones.text.toString(),
            equipo          = SessionManager.getEquipoId(this)
        )

        try {
            DatabaseHelper(this).guardarTrampa(registro)
            Toast.makeText(this, "✅ Registro guardado", Toast.LENGTH_SHORT).show()
            val opcionesIntent = Intent(this, TrampasOpcionesActivity::class.java)
            opcionesIntent.putExtra("plantacion_id", plantacionId)
            opcionesIntent.putExtra("lectura",       lectura)
            startActivity(opcionesIntent)
            finish()
        } catch (e: Exception) {
            Toast.makeText(this, "❌ Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}