package com.palmadata.app.tratamientos

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.palmadata.app.databinding.ActivityTrat74Binding
import com.palmadata.app.ui.WorkerAdapter
import com.palmadata.app.utils.DatabaseHelper

/**
 * Pantalla 7.4 — PRODUCTO. Lista con buscador, igual que el resto del módulo: tocar una opción
 * avanza de inmediato con esa selección. El botón de abajo avanza SIN
 * selección (el campo es opcional: viaja sin extra y se guarda NULL).
 * Todos los extras acumulados se copian con putExtras.
 */
class Trat7_4Activity : AppCompatActivity() {
    private lateinit var binding: ActivityTrat74Binding
    private lateinit var adapter: WorkerAdapter
    private var opciones = listOf<Pair<Int, String>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTrat74Binding.inflate(layoutInflater)
        setContentView(binding.root)
        val db = DatabaseHelper.getInstance(this)
        // Filtrado por la categoría elegida en la pantalla anterior; si no se
        // eligió ninguna (es opcional), se muestran todos los productos activos.
        val categoriaId = intent.getIntExtra("categoria_producto_id", 0)
        opciones = if (categoriaId > 0) db.getProductosPorCategoria(categoriaId) else db.getProductos()

        adapter = WorkerAdapter { nombre -> avanzar(opciones.first { it.second == nombre }) }
        binding.rvProductos.layoutManager = LinearLayoutManager(this)
        binding.rvProductos.adapter = adapter
        adapter.submitList(opciones.map { it.second })
        if (opciones.isEmpty()) binding.tvNoProductos.visibility = View.VISIBLE

        binding.etBuscador.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                adapter.filter(s.toString())
                binding.tvNoProductos.visibility = if (adapter.isEmpty()) View.VISIBLE else View.GONE
            }
        })

        // Sin tocar ninguna opción: avanza sin selección (queda NULL)
        binding.btnAccion.setOnClickListener { avanzar(null) }
    }

    private fun avanzar(opcion: Pair<Int, String>?) {
        startActivity(Intent(this, Trat8Activity::class.java).also {
            intent.extras?.let { e -> it.putExtras(e) }
            opcion?.let { o ->
                it.putExtra("producto_id", o.first)
                it.putExtra("producto_nombre", o.second)
            }
        })
    }
}
