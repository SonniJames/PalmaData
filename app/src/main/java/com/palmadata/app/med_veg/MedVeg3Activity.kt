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
 * es lo que viaja) con buscador. Tocar una uma la deja seleccionada; el botón
 * LÍNEA - PALMA exige una selección y pasa a la pantalla 4.
 *
 * Es también la pantalla a la que vuelve NUEVO REGISTRO (con CLEAR_TOP |
 * SINGLE_TOP): onNewIntent limpia la selección y el buscador para empezar en
 * blanco, con plantación, sector y lote ya cargados en los extras.
 */
class MedVeg3Activity : AppCompatActivity() {
    private lateinit var binding: ActivityMedVeg3Binding
    private lateinit var adapter: WorkerAdapter
    private var umas = listOf<Pair<Int, String>>()
    private var umaSeleccionada: Pair<Int, String>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMedVeg3Binding.inflate(layoutInflater)
        setContentView(binding.root)

        umas = DatabaseHelper.getInstance(this).getNutUmas()

        adapter = WorkerAdapter { codigo ->
            umaSeleccionada = umas.first { it.second == codigo }
            binding.tvSeleccion.text = "UMA: $codigo"
            binding.tvError.visibility = View.GONE
        }
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

        binding.btnAccion.setOnClickListener {
            val uma = umaSeleccionada
            if (uma == null) {
                binding.tvError.text = "Debe seleccionar una UMA"
                binding.tvError.visibility = View.VISIBLE
                return@setOnClickListener
            }
            startActivity(Intent(this, MedVeg4Activity::class.java).also {
                intent.extras?.let { e -> it.putExtras(e) }
                it.putExtra("uma_id", uma.first)
                it.putExtra("uma_codigo", uma.second)
            })
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        umaSeleccionada = null
        binding.tvSeleccion.text = "Seleccione una uma"
        binding.tvError.visibility = View.GONE
        binding.etBuscador.setText("")
    }
}
