package com.palmadata.app.super_tiempos

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.palmadata.app.databinding.ActivitySuperTiemposBinding
import com.palmadata.app.ui.WorkerAdapter
import com.palmadata.app.utils.DatabaseHelper
import com.palmadata.app.utils.SessionManager

/**
 * Paso 1 del módulo: elegir el CORTADOR que se va a observar.
 *
 * Ojo con la ambigüedad de "trabajador" en este módulo: el de la pantalla de
 * módulos es la SUPERVISORA (quien lleva el equipo); el que se elige aquí es el
 * cortador observado. Ambos se guardan en columnas distintas del registro.
 *
 * Se reutiliza WorkerAdapter, el mismo buscador incremental de los otros
 * módulos de supervisión, para que la supervisora no tenga que aprender otra
 * interacción.
 */
class SuperTiemposActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySuperTiemposBinding
    private lateinit var trabajadores: List<Pair<Int, String>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySuperTiemposBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // La supervisora debe estar identificada desde la pantalla de módulos.
        // Sin ella el registro no se puede atribuir a nadie.
        val supervisora = SessionManager.getCurrentWorker(this)
        if (supervisora == null) {
            Toast.makeText(this, "Seleccione primero su nombre en la pantalla de módulos",
                Toast.LENGTH_LONG).show()
            finish()
            return
        }
        binding.tvSupervisora.text = "Supervisora: ${supervisora.name}"

        trabajadores = DatabaseHelper.getInstance(this).getTrabajadores()

        val adapter = WorkerAdapter { nombre ->
            val cortador = trabajadores.first { it.second == nombre }

            // Un cortador no puede observarse a sí mismo: casi siempre es un
            // toque equivocado sobre el propio nombre de ella.
            if (cortador.first.toString() == supervisora.code) {
                Toast.makeText(this,
                    "La supervisora no puede ser el cortador observado",
                    Toast.LENGTH_LONG).show()
                return@WorkerAdapter
            }

            startActivity(
                Intent(this, SuperTiempos1Activity::class.java).apply {
                    putExtra(EXTRA_CORTADOR_ID, cortador.first)
                    putExtra(EXTRA_CORTADOR_NOMBRE, cortador.second)
                }
            )
            finish()   // no se vuelve aquí con atrás: la sesión ya arrancó
        }

        binding.rvCortadores.layoutManager = LinearLayoutManager(this)
        binding.rvCortadores.adapter = adapter
        adapter.submitList(trabajadores.map { it.second })

        binding.etBuscar.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                adapter.filter(s?.toString().orEmpty())
            }
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
        })
    }

    companion object {
        const val EXTRA_CORTADOR_ID     = "cortador_id"
        const val EXTRA_CORTADOR_NOMBRE = "cortador_nombre"
    }
}