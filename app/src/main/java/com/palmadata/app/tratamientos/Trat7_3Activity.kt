package com.palmadata.app.tratamientos

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.palmadata.app.databinding.ActivityTrat73Binding
import com.palmadata.app.ui.WorkerAdapter
import com.palmadata.app.utils.DatabaseHelper

/**
 * Pantalla 7.3 — CATEGORÍA DE PRODUCTO. Lista con buscador; tocar una opción la deja seleccionada y el
 * botón pasa a la siguiente pantalla CON o SIN selección (el campo es
 * opcional: sin selección viaja sin extra y se guarda NULL).
 * Todos los extras acumulados se copian con putExtras.
 */
class Trat7_3Activity : AppCompatActivity() {
    private lateinit var binding: ActivityTrat73Binding
    private lateinit var adapter: WorkerAdapter
    private var opciones = listOf<Pair<Int, String>>()
    private var seleccion: Pair<Int, String>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTrat73Binding.inflate(layoutInflater)
        setContentView(binding.root)
        val db = DatabaseHelper.getInstance(this)
        opciones = db.getCategoriasProducto()

        adapter = WorkerAdapter { nombre ->
            seleccion = opciones.first { it.second == nombre }
            binding.tvSeleccion.text = "Categoría: $nombre"
        }
        binding.rvCategorias.layoutManager = LinearLayoutManager(this)
        binding.rvCategorias.adapter = adapter
        adapter.submitList(opciones.map { it.second })
        if (opciones.isEmpty()) binding.tvNoCategorias.visibility = View.VISIBLE

        binding.etBuscador.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                adapter.filter(s.toString())
                binding.tvNoCategorias.visibility = if (adapter.isEmpty()) View.VISIBLE else View.GONE
            }
        })

        binding.btnAccion.setOnClickListener {
            startActivity(Intent(this, Trat7_4Activity::class.java).also {
                intent.extras?.let { e -> it.putExtras(e) }
                seleccion?.let { s ->
                    it.putExtra("categoria_producto_id", s.first)
                    it.putExtra("categoria_producto_nombre", s.second)
                }
            })
        }
    }
}
