package com.palmadata.app.super_poli

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.palmadata.app.databinding.ActivitySuperPoli13Binding
import com.palmadata.app.utils.DatabaseHelper
import com.palmadata.app.utils.SessionManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Pantalla 13 — OBSERVACIONES y guardado del registro.
 * Aquí converge todo: los extras acumulados de los dos flujos se resuelven
 * en una sola fila de pro_ordenes_super_poli_detalle.
 */
class SuperPoli13Activity : AppCompatActivity() {
    private lateinit var binding: ActivitySuperPoli13Binding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySuperPoli13Binding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.etObservaciones.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) { binding.tvContador.text = "${s?.length ?: 0}/250" }
        })

        binding.btnGuardar.setOnClickListener { guardarRegistro() }
    }

    private fun guardarRegistro() {
        val worker = SessionManager.getCurrentWorker(this)
        if (worker == null) {
            Toast.makeText(this, "Error: no hay trabajador en sesión", Toast.LENGTH_SHORT).show()
            return
        }

        val plantacionId     = intent.getIntExtra("plantacion_id", 0)
        val plantacionNombre = intent.getStringExtra("plantacion_nombre") ?: ""
        val sectorId         = intent.getIntExtra("sector_id", 0)
        val sectorNombre     = intent.getStringExtra("sector_nombre") ?: ""
        val loteId           = intent.getIntExtra("lote_id", 0)
        val loteNombre       = intent.getStringExtra("lote_nombre") ?: ""

        // Los campos numéricos llegan como texto del teclado: vacío = 0
        fun num(clave: String) = (intent.getStringExtra(clave) ?: "").trim().toIntOrNull() ?: 0

        val ahora = Date()
        val registro = SuperPoliRegistro(
            idUnico                 = UUID.randomUUID().toString(),
            fechaSuper              = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(ahora),
            supervisorSinOrden      = worker.code.toIntOrNull() ?: 0,
            polinizador             = intent.getIntExtra("polinizador_id", 0),
            catLoteId               = loteId.toLong(),
            linea                   = num("linea"),
            // Solo uno de los dos trae valor según el flujo; el otro queda en 0
            palma                   = num("palma"),
            cantPalmas              = num("cant_palmas"),
            florPoliniAplicacion1   = intent.getIntExtra("primera", 0),
            florPoliniAplicacion2   = intent.getIntExtra("segunda", 0),
            florPoliniAplicacion3   = intent.getIntExtra("tercera", 0),
            hojaSinMarcar           = num("hoja_sin_marcar"),
            espataSinAbrir          = num("espata_sin_abrir"),
            espataAbierta           = num("espata_abierta"),
            espataParcial           = num("espata_parcial"),
            malaCoberturaAplicacion = num("mala_cobertura_aplicacion"),
            observaciones           = binding.etObservaciones.text.toString(),
            latitud                 = SessionManager.getLastLatitude(this),
            longitud                = SessionManager.getLastLongitude(this)
        )

        try {
            DatabaseHelper.getInstance(this).guardarSuperPoli(registro)
            Toast.makeText(this, "✅ Registro guardado", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, SuperPoliOpcionesActivity::class.java).also {
                it.putExtra("plantacion_id", plantacionId)
                it.putExtra("plantacion_nombre", plantacionNombre)
                it.putExtra("sector_id", sectorId)
                it.putExtra("sector_nombre", sectorNombre)
                it.putExtra("lote_id", loteId)
                it.putExtra("lote_nombre", loteNombre)
            })
            finish()
        } catch (e: Exception) {
            Toast.makeText(this, "❌ Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}