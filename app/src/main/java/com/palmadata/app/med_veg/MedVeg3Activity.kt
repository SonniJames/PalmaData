package com.palmadata.app.med_veg

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.palmadata.app.databinding.ActivityMedVeg3Binding
import com.palmadata.app.ui.WorkerAdapter
import com.palmadata.app.utils.DatabaseHelper

/**
 * Pantalla 3 — UMA. Lista de plantacion.nut_uma (código visible, nut_uma_id
 * es lo que viaja) con buscador. Igual que el resto de la app: tocar una uma
 * avanza de inmediato a la pantalla 4. El botón LÍNEA - PALMA avanza SIN uma
 * (nut_uma_id queda en 0, el DEFAULT de la tabla).
 *
 * Es también la pantalla a la que vuelve NUEVO REGISTRO (con CLEAR_TOP |
 * SINGLE_TOP): onNewIntent limpia el buscador para mostrar la lista completa,
 * con plantación, sector y lote ya cargados en los extras.
 */
class MedVeg3Activity : AppCompatActivity() {
    private lateinit var binding: ActivityMedVeg3Binding
    private lateinit var adapter: WorkerAdapter
    private var umas = listOf<Pair<Int, String>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMedVeg3Binding.inflate(layoutInflater)
        setContentView(binding.root)

        umas = DatabaseHelper.getInstance(this).getNutUmas()

        adapter = WorkerAdapter { codigo -> avanzar(umas.first { it.second == codigo }) }
        binding.rvUmas.layoutManager = LinearLayoutManager(this)
        binding.rvUmas.adapter = adapter
        adapter.submitList(umas.map { it.second })
        if (umas.isEmpty()) binding.tvNoUmas.visibility = View.VISIBLE

        binding.etBuscador.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                adapter.filter(s.toString())
                binding.tvNoUmas.visibility = if (adapter.isEmpty()) View.VISIBLE else View.GONE
            }
        })

        // Sin tocar ninguna uma: avanza sin selección
        binding.btnAccion.setOnClickListener { avanzar(null) }
    }

    private fun avanzar(uma: Pair<Int, String>?) {
        startActivity(Intent(this, MedVeg4Activity::class.java).also {
            intent.extras?.let { e -> it.putExtras(e) }
            uma?.let { u ->
                it.putExtra("uma_id", u.first)
                it.putExtra("uma_codigo", u.second)
            }
        })
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        binding.etBuscador.setText("")   // el TextWatcher restaura la lista completa
    }
}
