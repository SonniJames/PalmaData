package com.palmadata.app.med_veg

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.palmadata.app.databinding.ActivityMedVeg19Binding
import com.palmadata.app.utils.DatabaseHelper
import com.palmadata.app.utils.SessionManager
import java.text.SimpleDateFormat
import java.util.*

/** Pantalla 19 — OBSERVACIONES + GUARDAR. Arma el registro con todos los extras acumulados. */
class MedVeg19Activity : AppCompatActivity() {

    private lateinit var binding: ActivityMedVeg19Binding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMedVeg19Binding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.etObservaciones.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) { binding.tvContador.text = "${s?.length ?: 0}/100" }
        })

        binding.btnGuardar.setOnClickListener { guardarRegistro() }
    }

    private fun guardarRegistro() {
        val worker = SessionManager.getCurrentWorker(this)
        if (worker == null) {
            Toast.makeText(this, "Error: no hay trabajador en sesión", Toast.LENGTH_SHORT).show()
            return
        }
        val ahora        = Date()
        val formatoFecha = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val formatoHora  = SimpleDateFormat("HH:mm:ss",   Locale.getDefault())

        // Extras: enteros de los teclados viajan como String ("" = no ingresado → NULL);
        // los decimales de las cajas viajan como Double (vacío ya es 0.0).
        fun s(k: String) = intent.getStringExtra(k) ?: ""
        fun d(k: String) = intent.getDoubleExtra(k, 0.0)

        val registro = MedVegRegistro(
            id              = UUID.randomUUID().toString(),
            fecha           = formatoFecha.format(ahora),
            hora            = formatoHora.format(ahora),
            catPlantacionId = intent.getIntExtra("plantacion_id", 0),
            catLoteId       = intent.getIntExtra("lote_id", 0).toLong(),
            evaluador       = worker.code.toIntOrNull() ?: 0,
            observaciones   = binding.etObservaciones.text.toString().take(100),
            linea           = s("linea").toIntOrNull() ?: 0,
            palma           = s("palma").toIntOrNull() ?: 0,
            latitud         = SessionManager.getLastLatitude(this),
            longitud        = SessionManager.getLastLongitude(this),
            nutUmaId        = intent.getIntExtra("uma_id", 0),
            numFoliolos     = s("num_foliolos").toDoubleOrNull(),
            longPeciolo     = d("long_peciolo"), anchPeciolo = d("anch_peciolo"),
            profPeciolo     = d("prof_peciolo"), longRaquis  = d("long_raquis"),
            ancho1 = d("ancho_1"), ancho2 = d("ancho_2"), ancho3 = d("ancho_3"), ancho4 = d("ancho_4"), ancho5 = d("ancho_5"), ancho6 = d("ancho_6"), ancho7 = d("ancho_7"), ancho8 = d("ancho_8"),
            largo1 = d("largo_1"), largo2 = d("largo_2"), largo3 = d("largo_3"), largo4 = d("largo_4"), largo5 = d("largo_5"), largo6 = d("largo_6"), largo7 = d("largo_7"), largo8 = d("largo_8"),
            hoja            = s("hoja").toIntOrNull(),
            numHojasVerdes  = s("hojas_verdes").toIntOrNull(),
            nivFoliar       = s("niv_foliar").toDoubleOrNull()
        )

        try {
            DatabaseHelper.getInstance(this).guardarMedVeg(registro)
            Toast.makeText(this, "✅ Registro guardado", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, MedVeg20Activity::class.java).also {
                // Solo lo que NUEVO REGISTRO necesita para volver a la pantalla 3
                it.putExtra("plantacion_id",     intent.getIntExtra("plantacion_id", 0))
                it.putExtra("plantacion_nombre", s("plantacion_nombre"))
                it.putExtra("sector_id",         intent.getIntExtra("sector_id", 0))
                it.putExtra("sector_nombre",     s("sector_nombre"))
                it.putExtra("lote_id",           intent.getIntExtra("lote_id", 0))
                it.putExtra("lote_nombre",       s("lote_nombre"))
            })
            finish()
        } catch (e: Exception) {
            Toast.makeText(this, "❌ Error al guardar: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
