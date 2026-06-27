package com.palmadata.app.trampas

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.palmadata.app.databinding.ActivityTrampas2Binding
import com.palmadata.app.ui.WorkerAdapter
import com.palmadata.app.utils.DatabaseHelper

class Trampas2Activity : AppCompatActivity() {
    private lateinit var binding: ActivityTrampas2Binding
    private lateinit var adapter: WorkerAdapter
    private var trampas = listOf<Pair<Int, String>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTrampas2Binding.inflate(layoutInflater)
        setContentView(binding.root)


        val plantacionId     = intent.getIntExtra("plantacion_id", 0)
        val plantacionNombre = intent.getStringExtra("plantacion_nombre") ?: ""
        val lectura          = intent.getStringExtra("lectura") ?: ""

        trampas = DatabaseHelper(this).getTrampas()

        adapter = WorkerAdapter { codigo ->
            val trampa = trampas.first { it.second == codigo }
            startActivity(Intent(this, Trampas3Activity::class.java).also {
                it.putExtra("plantacion_id", plantacionId)
                it.putExtra("plantacion_nombre", plantacionNombre)
                it.putExtra("lectura", lectura)
                it.putExtra("trampa_id", trampa.first)
                it.putExtra("trampa_codigo", trampa.second)
            })
        }

        binding.rvTrampas.layoutManager = LinearLayoutManager(this)
        binding.rvTrampas.adapter = adapter
        adapter.submitList(trampas.map { it.second })

        if (trampas.isEmpty()) {
            binding.tvNoTrampas.visibility = View.VISIBLE
            binding.tvNoTrampas.text = "Sin trampas. Sincronice primero."
        }

        binding.etBuscador.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                adapter.filter(s.toString())
                binding.tvNoTrampas.visibility = if (adapter.isEmpty()) View.VISIBLE else View.GONE
            }
        })
    }
}