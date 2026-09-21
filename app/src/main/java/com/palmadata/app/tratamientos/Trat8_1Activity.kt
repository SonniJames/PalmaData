package com.palmadata.app.tratamientos

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.palmadata.app.databinding.ActivityTrat81Binding
import com.palmadata.app.ui.WorkerAdapter
import com.palmadata.app.utils.DatabaseHelper

/**
 * Pantalla 8.1 — UNIDAD DE MEDIDA. Lista con buscador, igual que el resto del módulo: tocar una opción
 * avanza de inmediato con esa selección. El botón de abajo avanza SIN
 * selección (el campo es opcional: viaja sin extra y se guarda NULL).
 * Todos los extras acumulados se copian con putExtras.
 */
class Trat8_1Activity : AppCompatActivity() {
    private lateinit var binding: ActivityTrat81Binding
    private lateinit var adapter: WorkerAdapter
    private var opciones = listOf<Pair<Int, String>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTrat81Binding.inflate(layoutInflater)
        setContentView(binding.root)
        val db = DatabaseHelper.getInstance(this)
        opciones = db.getUnidadesAplicacion()

        adapter = WorkerAdapter { nombre -> avanzar(opciones.first { it.second == nombre }) }
        binding.rvUnidades.layoutManager = LinearLayoutManager(this)
        binding.rvUnidades.adapter = adapter
        adapter.submitList(opciones.map { it.second })
        if (opciones.isEmpty()) binding.tvNoUnidades.visibility = View.VISIBLE

        binding.etBuscador.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                adapter.filter(s.toString())
                binding.tvNoUnidades.visibility = if (adapter.isEmpty()) View.VISIBLE else View.GONE
            }
        })

        // Sin tocar ninguna opción: avanza sin selección (queda NULL)
        binding.btnAccion.setOnClickListener { avanzar(null) }
    }

    private fun avanzar(opcion: Pair<Int, String>?) {
        startActivity(Intent(this, Trat8_2Activity::class.java).also {
            intent.extras?.let { e -> it.putExtras(e) }
            opcion?.let { o ->
                it.putExtra("unidad_aplicacion_id", o.first)
                it.putExtra("unidad_aplicacion_nombre", o.second)
            }
        })
    }
}
