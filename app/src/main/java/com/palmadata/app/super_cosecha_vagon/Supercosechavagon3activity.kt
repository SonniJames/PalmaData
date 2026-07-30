package com.palmadata.app.super_cosecha_vagon

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.palmadata.app.R
import com.palmadata.app.databinding.ActivitySuperCosechaVagon3Binding
import com.palmadata.app.utils.DatabaseHelper

/**
 * Pantalla 3 — TRABAJADOR.
 *
 * Admite VARIOS trabajadores: el operario escribe, elige del desplegable y el
 * nombre queda listado debajo con un "−" para quitarlo. Al continuar viaja
 * como una lista de ids separados por coma:
 *   ""            ningún trabajador (el campo es opcional)
 *   "112"         uno
 *   "125,159,520" varios
 *
 * Al regresar desde la pantalla de opciones, la selección del registro
 * anterior llega en el extra y se reconstruye tal cual estaba.
 */
class SuperCosechaVagon3Activity : AppCompatActivity() {

    private lateinit var binding: ActivitySuperCosechaVagon3Binding
    private var trabajadores = listOf<Triple<Int, String, Int>>()

    // Selección actual: pares (id, nombre)
    private val seleccion = mutableListOf<Pair<Int, String>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySuperCosechaVagon3Binding.inflate(layoutInflater)
        setContentView(binding.root)

        val plantacionId     = intent.getIntExtra("plantacion_id", 0)
        val plantacionNombre = intent.getStringExtra("plantacion_nombre") ?: ""
        val sectorId         = intent.getIntExtra("sector_id", 0)
        val sectorNombre     = intent.getStringExtra("sector_nombre") ?: ""
        val loteId           = intent.getIntExtra("lote_id", 0)
        val loteNombre       = intent.getStringExtra("lote_nombre") ?: ""

        // Misma lista de trabajadores que alimenta la pantalla de módulos
        trabajadores = DatabaseHelper.getInstance(this).getTrabajadoresConSupervisor()
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, trabajadores.map { it.second })

        binding.acTrabajador.setAdapter(adapter)
        binding.acTrabajador.setOnItemClickListener { _, _, position, _ ->
            val nombre = binding.acTrabajador.adapter.getItem(position).toString()
            val t = trabajadores.firstOrNull { it.second == nombre }
            if (t != null && seleccion.none { it.first == t.first }) {
                seleccion.add(Pair(t.first, t.second))
                pintarSeleccion()
            }
            // Se limpia para poder escribir el siguiente nombre de una vez
            binding.acTrabajador.setText("")
        }

        // Precargar la selección del registro anterior (al volver desde opciones)
        precargar(intent.getStringExtra("trabajador_ids") ?: "")

        binding.btnAccion.setOnClickListener {
            startActivity(Intent(this, SuperCosechaVagon4Activity::class.java).also {
                it.putExtra("plantacion_id", plantacionId)
                it.putExtra("plantacion_nombre", plantacionNombre)
                it.putExtra("sector_id", sectorId)
                it.putExtra("sector_nombre", sectorNombre)
                it.putExtra("lote_id", loteId)
                it.putExtra("lote_nombre", loteNombre)
                // Solo cuenta lo que quedó en la lista; el texto suelto del campo
                // se ignora, así no hay ids fantasma de nombres a medio escribir.
                it.putExtra("trabajador_ids", seleccion.joinToString(",") { s -> s.first.toString() })
            })
        }
    }

    /** Reconstruye la lista desde "125,159,520" buscando los nombres. */
    private fun precargar(idsCsv: String) {
        if (idsCsv.isBlank()) return
        idsCsv.split(",").mapNotNull { it.trim().toIntOrNull() }.forEach { id ->
            val t = trabajadores.firstOrNull { it.first == id }
            if (t != null && seleccion.none { s -> s.first == id }) {
                seleccion.add(Pair(t.first, t.second))
            }
        }
        pintarSeleccion()
    }

    /** Dibuja una fila por trabajador seleccionado, con el "−" para quitarlo. */
    private fun pintarSeleccion() {
        val contenedor = binding.contTrabajador
        contenedor.removeAllViews()
        seleccion.toList().forEach { par ->
            val fila = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setBackgroundColor(resources.getColor(R.color.white, theme))
                setPadding(dp(12), dp(8), dp(12), dp(8))
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = dp(2) }
            }

            val btnQuitar = TextView(this).apply {
                text = "−"
                textSize = 22f
                setTextColor(resources.getColor(R.color.worker_not_set, theme))
                gravity = Gravity.CENTER
                setPadding(dp(8), 0, dp(16), 0)
                isClickable = true
                setOnClickListener {
                    seleccion.remove(par)
                    pintarSeleccion()
                }
            }

            val tvNombre = TextView(this).apply {
                text = par.second
                textSize = 15f
                setTextColor(resources.getColor(R.color.text_primary, theme))
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            }

            fila.addView(btnQuitar)
            fila.addView(tvNombre)
            contenedor.addView(fila)
        }
    }

    private fun dp(valor: Int): Int = (valor * resources.displayMetrics.density).toInt()
}