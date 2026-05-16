package com.palmadata.app.censo_enfermedades

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.palmadata.app.MainActivity
import com.palmadata.app.databinding.ActivityCensoEnf8Binding
import com.palmadata.app.utils.SessionManager
import java.text.SimpleDateFormat
import java.util.*

class CensoEnf8Activity : AppCompatActivity() {

    private lateinit var binding: ActivityCensoEnf8Binding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCensoEnf8Binding.inflate(layoutInflater)
        setContentView(binding.root)

        val censo        = intent.getStringExtra("censo") ?: ""
        val linea        = intent.getStringExtra("linea") ?: ""
        val palma        = intent.getStringExtra("palma") ?: ""
        val enfermedadId = intent.getIntExtra("enfermedadId", 0)
        val eventoId     = intent.getIntExtra("eventoId", 0)

        binding.etObservaciones.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                binding.tvContador.text = "${s?.length ?: 0}/500"
            }
        })

        binding.btnGuardar.setOnClickListener {
            guardarRegistro(censo, linea, palma, enfermedadId, eventoId)
        }
    }

    private fun guardarRegistro(
        censo: String, linea: String, palma: String,
        enfermedadId: Int, eventoId: Int
    ) {
        val worker     = SessionManager.getCurrentWorker(this)
        val plantacion = SessionManager.getCurrentPlantacion(this)

        if (worker == null || plantacion == null) {
            Toast.makeText(this, "Error: sesión inválida", Toast.LENGTH_SHORT).show()
            return
        }

        val ahora        = Date()
        val formatoFecha = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val formatoHora  = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val formatoId    = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())

        val fechaStr     = formatoFecha.format(ahora)
        val horaStr      = formatoHora.format(ahora)
        val plantacionId = CensoEnfData.getPlantacionId(plantacion.name)

        val codigoPlantacion = when (plantacionId) {
            CensoEnfData.ID_PALMERAS_YARIMA -> "PDY"
            CensoEnfData.ID_VILLA_CLAUDIA   -> "VC"
            CensoEnfData.ID_CUCU            -> "CUCU"
            else -> "UNK"
        }
        val idRegistro = "${codigoPlantacion}_${formatoId.format(ahora)}_${worker.code}"

        val lat = SessionManager.getLastLatitude(this)
        val lon = SessionManager.getLastLongitude(this)

        val registro = CensoEnfRegistro(
            censo             = censo.toLongOrNull() ?: 0L,
            linea             = linea.toIntOrNull() ?: 0,
            palma             = palma.toIntOrNull() ?: 0,
            observaciones     = binding.etObservaciones.text.toString(),
            catPlantacionId   = plantacionId,
            sanEnfermedadesId = enfermedadId,
            sanEventoEnfId    = eventoId,
            evaluador         = worker.code.toIntOrNull() ?: 0,
            fecha             = fechaStr,
            hora              = horaStr,
            actualizacion     = fechaStr,
            latitud           = lat,
            longitud          = lon,
            id                = idRegistro,
            equipo = SessionManager.getEquipoId(this)
        )

        // TODO Etapa 4: guardar en SQLite local y subir al DataLake
        Toast.makeText(
            this,
            "✅ Registro guardado\nID: $idRegistro",
            Toast.LENGTH_LONG
        ).show()

        // Volver al MainActivity limpiando el stack de Activities
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        startActivity(intent)
        finish()
    }
}