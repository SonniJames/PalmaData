package com.palmadata.app.supercosecha

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.palmadata.app.databinding.ActivitySuperCosecha8Binding
import com.palmadata.app.utils.DatabaseHelper
import com.palmadata.app.utils.SessionManager
import java.text.SimpleDateFormat
import java.util.*

class SuperCosecha8Activity : AppCompatActivity() {
    private lateinit var binding: ActivitySuperCosecha8Binding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySuperCosecha8Binding.inflate(layoutInflater)
        setContentView(binding.root)

        val plantacionId        = intent.getIntExtra("plantacion_id", 0)
        val loteId              = intent.getIntExtra("lote_id", 0)
        val ciclo               = intent.getStringExtra("ciclo") ?: "0"
        // Listas de ids en texto: "" ninguno, "112" uno, "125,159,520" varios
        val cortadorIds         = intent.getStringExtra("cortador_ids")   ?: ""
        val recolectorIds       = intent.getStringExtra("recolector_ids") ?: ""
        val alistadorIds        = intent.getStringExtra("alistador_ids")  ?: ""
        val linea               = intent.getStringExtra("linea") ?: ""
        val palma               = intent.getStringExtra("palma") ?: ""
        val racimosRecogidos    = intent.getIntExtra("racimos_recogidos", 0)
        val racimosVerdes       = intent.getIntExtra("racimos_verdes", 0)
        val racimossobremaduros = intent.getIntExtra("racimos_sobremaduros", 0)
        val racimosPodridos     = intent.getIntExtra("racimos_podridos", 0)
        val racimossinrecoger   = intent.getIntExtra("racimossinrecoger", 0)
        val racimossincortar    = intent.getIntExtra("racimossincortar", 0)
        val racimorobado        = intent.getIntExtra("racimorobado", 0)
        val hojasmalacomo       = intent.getIntExtra("hojasmalacomo", 0)   // "H. mal acomo."
        val hojacolgando        = intent.getIntExtra("hojacolgando", 0)
        val frutoplato          = intent.getIntExtra("frutoplato", 0)
        val racimomalacomo      = intent.getIntExtra("racimomalacomo", 0)  // "R. mal acomo."
        val pendunculolargo     = intent.getIntExtra("pendunculolargo", 0)
        // Pantalla 7.1 — vacío = null (NULL en la base)
        val porcentajeBasura    = (intent.getStringExtra("porcentaje_basura") ?: "").trim().toDoubleOrNull()
        // Pantalla 7.2 — casillas: 1 marcada, 0 sin marcar
        val platosEnmalezados   = intent.getIntExtra("platos_enmalezados", 0)
        val malezasEstipe       = intent.getIntExtra("malezas_estipe", 0)
        val loteInundado        = intent.getIntExtra("lote_inundado", 0)
        val podaAtrasada        = intent.getIntExtra("poda_atrasada", 0)

        binding.etObservaciones.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) { binding.tvContador.text = "${s?.length ?: 0}/250" }
        })

        binding.btnGuardar.setOnClickListener {
            guardarRegistro(plantacionId, loteId, ciclo, cortadorIds, recolectorIds, alistadorIds, linea, palma,
                racimosRecogidos, racimosVerdes, racimossobremaduros, racimosPodridos,
                racimossinrecoger, racimossincortar, racimorobado, hojasmalacomo, hojacolgando, frutoplato,
                racimomalacomo, pendunculolargo, porcentajeBasura,
                platosEnmalezados, malezasEstipe, loteInundado, podaAtrasada)
        }
    }
    private fun guardarRegistro(
        plantacionId: Int, loteId: Int, ciclo: String,
        cortadorIds: String, recolectorIds: String, alistadorIds: String,
        linea: String, palma: String, racimosRecogidos: Int, racimosVerdes: Int,
        racimossobremaduros: Int, racimosPodridos: Int, racimossinrecoger: Int,
        racimossincortar: Int, racimorobado: Int, hojasmalacomo: Int, hojacolgando: Int, frutoplato: Int,
        racimomalacomo: Int, pendunculolargo: Int, porcentajeBasura: Double?,
        platosEnmalezados: Int, malezasEstipe: Int, loteInundado: Int, podaAtrasada: Int
    ) {
        val worker = SessionManager.getCurrentWorker(this)
        if (worker == null) { Toast.makeText(this, "Error: no hay trabajador en sesión", Toast.LENGTH_SHORT).show(); return }
        val ahora = Date()
        val registro = SuperCosechaRegistro(
            idUnico             = UUID.randomUUID().toString(),
            fecha               = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(ahora),
            hora                = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(ahora),
            supervisor          = worker.code.toIntOrNull() ?: 0,
            cortador            = cortadorIds,
            recolector          = recolectorIds,
            alistador           = alistadorIds,
            // LÍNEA y PALMA son opcionales en este módulo: vacío = NULL
            linea               = linea.toIntOrNull(),
            palma               = palma.toIntOrNull(),
            ciclo               = ciclo.toIntOrNull() ?: 0,
            catLoteId           = loteId.toLong(),
            catPlantacionId     = plantacionId,
            racimosRecogidos    = racimosRecogidos,
            racimosVerdes       = racimosVerdes,
            racimossobremaduros = racimossobremaduros,
            racimosPodridos     = racimosPodridos,
            racimossinrecoger   = racimossinrecoger,
            racimossincortar    = racimossincortar,
            racimorobado        = racimorobado,
            hojasmalacomo       = hojasmalacomo,
            hojacolgando        = hojacolgando,
            frutoplato          = frutoplato,
            racimomalacomo      = racimomalacomo,
            pendunculolargo     = pendunculolargo,
            porcentajeBasura    = porcentajeBasura,
            platosEnmalezados   = platosEnmalezados,
            malezasEstipe       = malezasEstipe,
            loteInundado        = loteInundado,
            podaAtrasada        = podaAtrasada,
            observaciones       = binding.etObservaciones.text.toString(),
            latitud             = SessionManager.getLastLatitude(this),
            longitud            = SessionManager.getLastLongitude(this),
            equipo              = SessionManager.getEquipoId(this)
        )
        try {
            DatabaseHelper.getInstance(this).guardarSuperCosecha(registro)
            Toast.makeText(this, "✅ Registro guardado", Toast.LENGTH_SHORT).show()
            val opcionesIntent = Intent(this, SuperCosechaOpcionesActivity::class.java)
            opcionesIntent.putExtra("plantacion_id",  plantacionId)
            opcionesIntent.putExtra("lote_id",        loteId)
            opcionesIntent.putExtra("ciclo",          ciclo)
            opcionesIntent.putExtra("cortador_ids",   cortadorIds)
            opcionesIntent.putExtra("recolector_ids", recolectorIds)
            opcionesIntent.putExtra("alistador_ids",  alistadorIds)
            startActivity(opcionesIntent)
            finish()
        } catch (e: Exception) {
            Toast.makeText(this, "❌ Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}