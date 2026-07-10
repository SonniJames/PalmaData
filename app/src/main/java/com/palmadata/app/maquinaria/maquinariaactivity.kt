package com.palmadata.app.maquinaria

import android.app.AlertDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import com.palmadata.app.R
import com.palmadata.app.databinding.ActivityMaquinariaBinding
import com.palmadata.app.databinding.DialogFinalizarLaborBinding
import com.palmadata.app.utils.DatabaseHelper
import com.palmadata.app.utils.SessionManager
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class MaquinariaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMaquinariaBinding
    private lateinit var db: DatabaseHelper

    // Valores seleccionados
    private var plantacionId = 0; private var plantacionNombre = ""
    private var maquinaId = 0; private var maquinaNombre = ""
    private var implementoId = 0; private var implementoNombre = ""
    private var laborId = 0; private var laborNombre = ""
    private var unidadId = 0; private var unidadNombre = ""
    private var trabajadorId = 0
    private var lotesSeleccionados = mutableListOf<Pair<Int, String>>() // id, nombre

    private var laborIniciada = false
    private var fechaInicial = ""; private var horaInicial = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMaquinariaBinding.inflate(layoutInflater)
        setContentView(binding.root)
        db = DatabaseHelper.getInstance(this)

        setupCamposSeleccion()
        setupTrabajadorAutoComplete()
        setupCamposNumericos()
        setupLotes()
        setupBotones()

        // Si Android mató el proceso con una labor iniciada, se restaura tal cual
        restaurarSesionEnCurso()

        verificarCamposObligatorios()
    }

    override fun onPause() {
        super.onPause()
        // Mantener actualizada la sesión (combustible, cantidad, observaciones
        // se escriben durante la labor) por si el sistema mata el proceso
        if (laborIniciada) guardarSesionEnCurso()
    }

    // ── Campos de selección con diálogo ──────────────────────────────────────

    private fun setupCamposSeleccion() {
        binding.tvPlantacion.setOnClickListener { mostrarDialogoLista("Plantación", db.getPlantaciones()) { id, nombre -> plantacionId = id; plantacionNombre = nombre; binding.tvPlantacion.text = nombre; verificarCamposObligatorios() } }
        binding.tvMaquina.setOnClickListener { mostrarDialogoLista("Máquina", db.getMaquinaria()) { id, nombre -> maquinaId = id; maquinaNombre = nombre; binding.tvMaquina.text = nombre; verificarCamposObligatorios() } }
        binding.tvImplemento.setOnClickListener { mostrarDialogoLista("Implemento", db.getImplementos()) { id, nombre -> implementoId = id; implementoNombre = nombre; binding.tvImplemento.text = nombre; verificarCamposObligatorios() } }
        binding.tvLabor.setOnClickListener { mostrarDialogoLista("Labor", db.getLaboresMaquinaria()) { id, nombre -> laborId = id; laborNombre = nombre; binding.tvLabor.text = nombre; verificarCamposObligatorios() } }
        binding.tvUnidad.setOnClickListener { mostrarDialogoLista("Unidad", db.getUnidadesMaquinaria()) { id, nombre -> unidadId = id; unidadNombre = nombre; binding.tvUnidad.text = nombre } }
    }

    private fun mostrarDialogoLista(titulo: String, opciones: List<Pair<Int, String>>, onSeleccion: (Int, String) -> Unit) {
        if (opciones.isEmpty()) { Toast.makeText(this, "Sin datos. Sincronice primero.", Toast.LENGTH_SHORT).show(); return }
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_lista_buscador, null)
        val etBuscar = view.findViewById<EditText>(R.id.etBuscar)
        val lv = view.findViewById<ListView>(R.id.lvOpciones)
        var filtradas = opciones.map { it.second }
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, filtradas.toMutableList())
        lv.adapter = adapter
        val dialog = AlertDialog.Builder(this).setTitle(titulo).setView(view).setNegativeButton("Cancelar", null).create()
        etBuscar.addTextChangedListener { s ->
            filtradas = opciones.map { it.second }.filter { it.contains(s.toString(), ignoreCase = true) }
            adapter.clear(); adapter.addAll(filtradas); adapter.notifyDataSetChanged()
        }
        lv.setOnItemClickListener { _, _, position, _ ->
            val nombre = filtradas[position]
            val id = opciones.first { it.second == nombre }.first
            onSeleccion(id, nombre); dialog.dismiss()
        }
        dialog.show()
    }

    // ── Trabajador autocompletado ─────────────────────────────────────────────

    private fun setupTrabajadorAutoComplete() {
        val trabajadores = db.getTrabajadoresConSupervisor()
        val nombres = trabajadores.map { it.second }
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, nombres)
        binding.acTrabajador.setAdapter(adapter)
        binding.acTrabajador.setOnItemClickListener { _, _, position, _ ->
            val nombre = binding.acTrabajador.adapter.getItem(position).toString()
            trabajadorId = trabajadores.first { it.second == nombre }.first
            verificarCamposObligatorios()
        }
        binding.acTrabajador.addTextChangedListener { verificarCamposObligatorios() }
    }

    // ── Campos numéricos ─────────────────────────────────────────────────────

    private fun setupCamposNumericos() {
        listOf(binding.etHorometroInicial, binding.etKilometroInicial,
            binding.etCombustible, binding.etCantidad).forEach { et ->
            et.addTextChangedListener { verificarCamposObligatorios() }
        }
    }

    // ── Lotes con selección múltiple ─────────────────────────────────────────

    private fun setupLotes() {
        binding.tvLotes.setOnClickListener { mostrarDialogoLotes() }
    }

    private fun mostrarDialogoLotes() {
        val todosLotes = db.getTodosLotes()
        if (todosLotes.isEmpty()) { Toast.makeText(this, "Sin datos. Sincronice primero.", Toast.LENGTH_SHORT).show(); return }

        // IDs seleccionados actualmente
        val idsSeleccionados = lotesSeleccionados.map { it.first }.toMutableSet()

        val view = LayoutInflater.from(this).inflate(R.layout.dialog_lista_buscador, null)
        val etBuscar = view.findViewById<EditText>(R.id.etBuscar)
        val lv = view.findViewById<ListView>(R.id.lvOpciones)

        var lotesFiltrados = todosLotes.toMutableList()

        val adapter = object : ArrayAdapter<String>(this, android.R.layout.simple_list_item_multiple_choice, lotesFiltrados.map { it.second }.toMutableList()) {}
        lv.adapter = adapter
        lv.choiceMode = ListView.CHOICE_MODE_MULTIPLE

        // Marcar los ya seleccionados
        fun actualizarChecks() {
            lotesFiltrados.forEachIndexed { i, lote ->
                lv.setItemChecked(i, idsSeleccionados.contains(lote.first))
            }
        }
        actualizarChecks()

        lv.setOnItemClickListener { _, _, position, _ ->
            val lote = lotesFiltrados[position]
            if (idsSeleccionados.contains(lote.first)) {
                idsSeleccionados.remove(lote.first)
            } else {
                idsSeleccionados.add(lote.first)
            }
        }

        etBuscar.addTextChangedListener { s ->
            lotesFiltrados = todosLotes.filter { it.second.contains(s.toString(), ignoreCase = true) }.toMutableList()
            adapter.clear()
            adapter.addAll(lotesFiltrados.map { it.second })
            adapter.notifyDataSetChanged()
            actualizarChecks()
        }

        AlertDialog.Builder(this)
            .setTitle("Asignar lotes")
            .setView(view)
            .setPositiveButton("Aceptar") { _, _ ->
                lotesSeleccionados.clear()
                todosLotes.forEach { lote ->
                    if (idsSeleccionados.contains(lote.first)) lotesSeleccionados.add(lote)
                }
                binding.tvLotes.text = if (lotesSeleccionados.isEmpty()) "Seleccionar lotes"
                else lotesSeleccionados.joinToString(", ") { it.second }
                verificarCamposObligatorios()
            }
            .setNegativeButton("Cancelar", null)
            .create()
            .show()
    }

    // ── Validación obligatorios ───────────────────────────────────────────────

    private fun verificarCamposObligatorios() {
        val ok = plantacionId > 0 && maquinaId > 0 && implementoId > 0 &&
                laborId > 0 && trabajadorId > 0 && lotesSeleccionados.isNotEmpty() &&
                binding.etHorometroInicial.text.isNotEmpty() && binding.etKilometroInicial.text.isNotEmpty()
        binding.btnIniciarFinalizar.isEnabled = if (laborIniciada) true else ok
    }

    // ── Botones ──────────────────────────────────────────────────────────────

    private fun setupBotones() {
        binding.btnIniciarFinalizar.isEnabled = false
        binding.btnIniciarFinalizar.setOnClickListener {
            if (!laborIniciada) iniciarLabor() else mostrarDialogoFinalizar()
        }
    }

    private fun iniciarLabor() {
        val ahora = Date()
        fechaInicial = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(ahora)
        horaInicial  = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(ahora)
        laborIniciada = true
        SessionManager.setTrabajadorMaquinariaActivo(this, trabajadorId)
        guardarSesionEnCurso()
        bloquearCamposLabor()
    }

    /** Bloquea los campos iniciales y pone el botón en modo FINALIZAR */
    private fun bloquearCamposLabor() {
        binding.tvPlantacion.isEnabled = false
        binding.acTrabajador.isEnabled = false
        binding.etHorometroInicial.isEnabled = false
        binding.etKilometroInicial.isEnabled = false
        binding.tvLotes.isEnabled = false

        binding.btnIniciarFinalizar.text = "FINALIZAR LABOR"
        binding.btnIniciarFinalizar.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#D32F2F"))
        binding.btnIniciarFinalizar.isEnabled = true
    }

    // ── Persistencia de la labor en curso ─────────────────────────────────────
    // Una labor de maquinaria dura horas; si Android mata el proceso, sin esto
    // la labor iniciada se pierde y el trabajador quedaría "pegado" en los tracks.

    private val PREFS_SESION = "maquinaria_sesion_en_curso"

    private fun guardarSesionEnCurso() {
        val json = JSONObject().apply {
            put("plantacionId", plantacionId);   put("plantacionNombre", plantacionNombre)
            put("maquinaId", maquinaId);         put("maquinaNombre", maquinaNombre)
            put("implementoId", implementoId);   put("implementoNombre", implementoNombre)
            put("laborId", laborId);             put("laborNombre", laborNombre)
            put("unidadId", unidadId);           put("unidadNombre", unidadNombre)
            put("trabajadorId", trabajadorId)
            put("trabajadorNombre", binding.acTrabajador.text.toString())
            put("fechaInicial", fechaInicial);   put("horaInicial", horaInicial)
            put("horometroInicial", binding.etHorometroInicial.text.toString())
            put("kilometroInicial", binding.etKilometroInicial.text.toString())
            put("combustible", binding.etCombustible.text.toString())
            put("cantidad", binding.etCantidad.text.toString())
            put("observaciones", binding.etObservaciones.text.toString())
            put("lotes", JSONArray().apply {
                lotesSeleccionados.forEach { (id, nombre) ->
                    put(JSONObject().put("id", id).put("nombre", nombre))
                }
            })
        }
        getSharedPreferences(PREFS_SESION, MODE_PRIVATE).edit()
            .putString("sesion", json.toString()).apply()
    }

    private fun restaurarSesionEnCurso() {
        val guardada = getSharedPreferences(PREFS_SESION, MODE_PRIVATE)
            .getString("sesion", null) ?: return
        try {
            val j = JSONObject(guardada)
            plantacionId = j.optInt("plantacionId"); plantacionNombre = j.optString("plantacionNombre")
            maquinaId    = j.optInt("maquinaId");    maquinaNombre    = j.optString("maquinaNombre")
            implementoId = j.optInt("implementoId"); implementoNombre = j.optString("implementoNombre")
            laborId      = j.optInt("laborId");      laborNombre      = j.optString("laborNombre")
            unidadId     = j.optInt("unidadId");     unidadNombre     = j.optString("unidadNombre")
            trabajadorId = j.optInt("trabajadorId")
            fechaInicial = j.optString("fechaInicial")
            horaInicial  = j.optString("horaInicial")

            lotesSeleccionados.clear()
            val arr = j.optJSONArray("lotes") ?: JSONArray()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                lotesSeleccionados.add(Pair(o.getInt("id"), o.getString("nombre")))
            }

            // Restaurar UI
            if (plantacionNombre.isNotEmpty()) binding.tvPlantacion.text = plantacionNombre
            if (maquinaNombre.isNotEmpty())    binding.tvMaquina.text    = maquinaNombre
            if (implementoNombre.isNotEmpty()) binding.tvImplemento.text = implementoNombre
            if (laborNombre.isNotEmpty())      binding.tvLabor.text      = laborNombre
            if (unidadNombre.isNotEmpty())     binding.tvUnidad.text     = unidadNombre
            binding.acTrabajador.setText(j.optString("trabajadorNombre"), false)
            binding.etHorometroInicial.setText(j.optString("horometroInicial"))
            binding.etKilometroInicial.setText(j.optString("kilometroInicial"))
            binding.etCombustible.setText(j.optString("combustible"))
            binding.etCantidad.setText(j.optString("cantidad"))
            binding.etObservaciones.setText(j.optString("observaciones"))
            if (lotesSeleccionados.isNotEmpty()) {
                binding.tvLotes.text = lotesSeleccionados.joinToString(", ") { it.second }
            }

            laborIniciada = true
            SessionManager.setTrabajadorMaquinariaActivo(this, trabajadorId)
            bloquearCamposLabor()
            Toast.makeText(this, "Labor en curso restaurada. Finalícela cuando termine.", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            // Sesión corrupta: se descarta y se limpia la atribución de tracks
            limpiarSesionEnCurso()
            SessionManager.clearTrabajadorMaquinariaActivo(this)
        }
    }

    private fun limpiarSesionEnCurso() {
        getSharedPreferences(PREFS_SESION, MODE_PRIVATE).edit().clear().apply()
    }

    private fun mostrarDialogoFinalizar() {
        val combustible = binding.etCombustible.text.toString().toDoubleOrNull()
        if (combustible == null || combustible <= 0.0) {
            Toast.makeText(this, "Ingrese valor en Combustible", Toast.LENGTH_SHORT).show()
            return
        }
        val horometroInicial = binding.etHorometroInicial.text.toString().toDoubleOrNull() ?: 0.0
        val kilometroInicial = binding.etKilometroInicial.text.toString().toDoubleOrNull() ?: 0.0

        val dialogBinding = DialogFinalizarLaborBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(this)
            .setTitle("Finalizar labor")
            .setView(dialogBinding.root)
            .create()

        dialogBinding.btnGuardarFinal.setOnClickListener {
            val horometroFinal = dialogBinding.etHorometroFinal.text.toString().toDoubleOrNull()
            val kilometroFinal = dialogBinding.etKilometroFinal.text.toString().toDoubleOrNull()

            when {
                horometroFinal == null -> { Toast.makeText(this, "Ingrese Horómetro Final", Toast.LENGTH_SHORT).show(); return@setOnClickListener }
                horometroFinal < horometroInicial -> { Toast.makeText(this, "Horómetro final debe ser ≥ al inicial", Toast.LENGTH_SHORT).show(); return@setOnClickListener }
                kilometroFinal == null -> { Toast.makeText(this, "Ingrese Kilometraje Final", Toast.LENGTH_SHORT).show(); return@setOnClickListener }
                kilometroFinal < kilometroInicial -> { Toast.makeText(this, "Kilometraje final debe ser ≥ al inicial", Toast.LENGTH_SHORT).show(); return@setOnClickListener }
            }

            guardarRegistro(horometroFinal!!, kilometroFinal!!)
            dialog.dismiss()
        }
        dialog.show()
    }

    // ── Guardar ──────────────────────────────────────────────────────────────

    private fun guardarRegistro(horometroFinal: Double, kilometroFinal: Double) {
        val ahora = Date()
        val registro = MaquinariaRegistro(
            idUnico          = UUID.randomUUID().toString(),
            maquina          = maquinaId,
            plantacion       = plantacionId,
            implemento       = implementoId,
            labor            = laborId,
            trabajador       = trabajadorId,
            kiloinicial      = binding.etKilometroInicial.text.toString().toDoubleOrNull() ?: 0.0,
            kilofinal        = kilometroFinal,
            combustible      = binding.etCombustible.text.toString().toDoubleOrNull() ?: 0.0,
            horometroinicial = binding.etHorometroInicial.text.toString().toDoubleOrNull() ?: 0.0,
            horometrofinal   = horometroFinal,
            lote             = lotesSeleccionados.joinToString(",") { it.first.toString() },
            observaciones    = binding.etObservaciones.text.toString(),
            unidadcantidad   = unidadId,
            cantidad         = binding.etCantidad.text.toString().toDoubleOrNull() ?: 0.0,
            fechainicial     = fechaInicial,
            horainicial      = horaInicial,
            fechafinal       = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(ahora),
            horafinal        = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(ahora),
            equipo           = SessionManager.getEquipoId(this)
        )
        try {
            db.guardarMaquinaria(registro)
            SessionManager.clearTrabajadorMaquinariaActivo(this)
            limpiarSesionEnCurso()
            Toast.makeText(this, "✅ Registro guardado", Toast.LENGTH_SHORT).show()
            finish()
        } catch (e: Exception) {
            Toast.makeText(this, "❌ Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}