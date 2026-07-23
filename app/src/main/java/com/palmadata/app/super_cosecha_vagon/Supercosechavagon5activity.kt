package com.palmadata.app.super_cosecha_vagon

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.palmadata.app.databinding.ActivitySuperCosechaVagon5Binding
import com.palmadata.app.utils.DatabaseHelper
import com.palmadata.app.utils.SessionManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class SuperCosechaVagon5Activity : AppCompatActivity() {
    private lateinit var binding: ActivitySuperCosechaVagon5Binding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySuperCosechaVagon5Binding.inflate(layoutInflater)
        setContentView(binding.root)

        val plantacionId       = intent.getIntExtra("plantacion_id", 0)
        val plantacionNombre   = intent.getStringExtra("plantacion_nombre") ?: ""
        val sectorId           = intent.getIntExtra("sector_id", 0)
        val sectorNombre       = intent.getStringExtra("sector_nombre") ?: ""
        val loteId             = intent.getIntExtra("lote_id", 0)
        val loteNombre         = intent.getStringExtra("lote_nombre") ?: ""
        val trabajadorId       = intent.getIntExtra("trabajador_id", 0)
        val racimosMuestra     = intent.getIntExtra("racimos_muestra", 0)
        val racimosVerde       = intent.getIntExtra("racimos_verde", 0)
        val racimosSobremaduro = intent.getIntExtra("racimos_sobremaduro", 0)
        val racimosPodridos    = intent.getIntExtra("racimos_podridos", 0)
        val pedunculoLargo     = intent.getIntExtra("pedunculo_largo", 0)
        val racimosMalformados = intent.getIntExtra("racimos_malformados", 0)
        val racimosEnfermos    = intent.getIntExtra("racimos_enfermos", 0)
        val racimosEupalamides = intent.getIntExtra("racimos_eupalamides", 0)

        binding.etObservaciones.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) { binding.tvContador.text = "${s?.length ?: 0}/250" }
        })

        binding.btnGuardar.setOnClickListener {
            guardarRegistro(
                plantacionId, plantacionNombre, sectorId, sectorNombre, loteId, loteNombre,
                trabajadorId, racimosMuestra, racimosVerde, racimosSobremaduro, racimosPodridos,
                pedunculoLargo, racimosMalformados, racimosEnfermos, racimosEupalamides
            )
        }
    }

    private fun guardarRegistro(
        plantacionId: Int, plantacionNombre: String, sectorId: Int, sectorNombre: String,
        loteId: Int, loteNombre: String, trabajadorId: Int,
        racimosMuestra: Int, racimosVerde: Int, racimosSobremaduro: Int, racimosPodridos: Int,
        pedunculoLargo: Int, racimosMalformados: Int, racimosEnfermos: Int, racimosEupalamides: Int
    ) {
        val worker = SessionManager.getCurrentWorker(this)
        if (worker == null) {
            Toast.makeText(this, "Error: no hay trabajador en sesión", Toast.LENGTH_SHORT).show()
            return
        }
        val ahora = Date()
        val registro = SuperCosechaVagonRegistro(
            idUnico            = UUID.randomUUID().toString(),
            fecha              = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(ahora),
            hora               = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(ahora),
            supervisor         = worker.code.toIntOrNull() ?: 0,
            // El trabajador de la pantalla 3 es opcional: 0 significa "en blanco",
            // y se guarda como NULL (no como 0) en la base de datos.
            trabajador         = if (trabajadorId > 0) trabajadorId else null,
            catLoteId          = loteId.toLong(),
            catPlantacionId    = plantacionId.toLong(),
            racimosMuestra     = racimosMuestra,
            racimosVerde       = racimosVerde,
            racimosSobremaduro = racimosSobremaduro,
            racimosPodridos    = racimosPodridos,
            pedunculoLargo     = pedunculoLargo,
            racimosMalformados = racimosMalformados,
            racimosEnfermos    = racimosEnfermos,
            racimosEupalamides = racimosEupalamides,
            observaciones      = binding.etObservaciones.text.toString(),
            latitud            = SessionManager.getLastLatitude(this),
            longitud           = SessionManager.getLastLongitude(this)
        )
        try {
            DatabaseHelper.getInstance(this).guardarSuperCosechaVagon(registro)
            Toast.makeText(this, "✅ Registro guardado", Toast.LENGTH_SHORT).show()
            val opcionesIntent = Intent(this, SuperCosechaVagonOpcionesActivity::class.java)
            opcionesIntent.putExtra("plantacion_id", plantacionId)
            opcionesIntent.putExtra("plantacion_nombre", plantacionNombre)
            opcionesIntent.putExtra("sector_id", sectorId)
            opcionesIntent.putExtra("sector_nombre", sectorNombre)
            opcionesIntent.putExtra("lote_id", loteId)
            opcionesIntent.putExtra("lote_nombre", loteNombre)
            startActivity(opcionesIntent)
            finish()
        } catch (e: Exception) {
            Toast.makeText(this, "❌ Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}