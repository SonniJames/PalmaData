package com.palmadata.app.plagas

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.palmadata.app.MainActivity
import com.palmadata.app.databinding.ActivityPlagas11Binding
import com.palmadata.app.utils.DatabaseHelper
import com.palmadata.app.utils.SessionManager
import java.text.SimpleDateFormat
import java.util.*

class Plagas11Activity : AppCompatActivity() {
    private lateinit var binding: ActivityPlagas11Binding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlagas11Binding.inflate(layoutInflater)
        setContentView(binding.root)
        val plantacionId    = intent.getIntExtra("plantacion_id", 0)
        val loteId          = intent.getIntExtra("lote_id", 0)
        val lectura         = intent.getStringExtra("lectura") ?: ""
        val linea           = intent.getStringExtra("linea") ?: ""
        val palma           = intent.getStringExtra("palma") ?: ""
        val insectoId       = intent.getIntExtra("insecto_id", 0)
        val estadoInsectoId = intent.getIntExtra("estado_insecto_id", 0)
        val cantidad        = intent.getStringExtra("cantidad") ?: "0"
        val nivFoliar       = intent.getStringExtra("niv_foliar") ?: "0"
        val defol5          = intent.getDoubleExtra("defol5", 0.0)
        val defol13         = intent.getDoubleExtra("defol13", 0.0)
        val defol21         = intent.getDoubleExtra("defol21", 0.0)
        val defol29         = intent.getDoubleExtra("defol29", 0.0)
        val defol37         = intent.getDoubleExtra("defol37", 0.0)

        binding.etObservaciones.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) { binding.tvContador.text = "${s?.length ?: 0}/100" }
        })

        binding.btnGuardar.setOnClickListener {
            guardarRegistro(plantacionId, loteId, lectura, linea, palma, insectoId, estadoInsectoId, cantidad, nivFoliar, defol5, defol13, defol21, defol29, defol37)
        }
    }

    private fun guardarRegistro(
        plantacionId: Int, loteId: Int, lectura: String, linea: String, palma: String,
        insectoId: Int, estadoInsectoId: Int, cantidad: String, nivFoliar: String,
        defol5: Double, defol13: Double, defol21: Double, defol29: Double, defol37: Double
    ) {
        val worker = SessionManager.getCurrentWorker(this)
        if (worker == null) { Toast.makeText(this, "Error: no hay trabajador en sesión", Toast.LENGTH_SHORT).show(); return }
        val ahora = Date()
        val registro = PlagasRegistro(
            id              = UUID.randomUUID().toString(),
            fecha           = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(ahora),
            hora            = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(ahora),
            lectura         = lectura.toIntOrNull() ?: 0,
            linea           = linea.toIntOrNull() ?: 0,
            palma           = palma.toIntOrNull() ?: 0,
            catLoteId       = loteId.toLong(),
            catPalmaId      = 0L,
            catPlantacionId = plantacionId,
            evaluador       = worker.code.toIntOrNull() ?: 0,
            insectoId       = insectoId,
            estadoInsectoId = estadoInsectoId,
            cantidad        = cantidad.toIntOrNull() ?: 0,
            nivFoliar       = nivFoliar.toIntOrNull() ?: 0,
            defol5          = defol5, defol13 = defol13, defol21 = defol21,
            defol29         = defol29, defol37 = defol37,
            observaciones   = binding.etObservaciones.text.toString(),
            latitud         = SessionManager.getLastLatitude(this),
            longitud        = SessionManager.getLastLongitude(this),
            equipo          = SessionManager.getEquipoId(this)
        )
        try {
            DatabaseHelper(this).guardarPlagas(registro)
            Toast.makeText(this, "✅ Registro guardado", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "❌ Error: ${e.message}", Toast.LENGTH_LONG).show(); return
        }
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        startActivity(intent); finish()
    }
}