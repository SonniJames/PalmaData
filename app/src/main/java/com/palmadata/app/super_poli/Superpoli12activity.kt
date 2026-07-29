package com.palmadata.app.super_poli

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.palmadata.app.databinding.ActivitySuperPoli12Binding
import com.palmadata.app.utils.DatabaseHelper

/**
 * Pantalla 12 — POLINIZADOR. Autocompletado sobre la misma lista de
 * trabajadores de la pantalla de módulos. A diferencia de otros módulos,
 * aquí el campo es OBLIGATORIO: sin trabajador no se puede continuar.
 */
class SuperPoli12Activity : AppCompatActivity() {
    private lateinit var binding: ActivitySuperPoli12Binding
    private var trabajadores = listOf<Triple<Int, String, Int>>()
    private var polinizadorId = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySuperPoli12Binding.inflate(layoutInflater)
        setContentView(binding.root)

        trabajadores = DatabaseHelper.getInstance(this).getTrabajadoresConSupervisor()
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, trabajadores.map { it.second })
        binding.acPolinizador.setAdapter(adapter)
        binding.acPolinizador.setOnItemClickListener { _, _, position, _ ->
            val nombre = binding.acPolinizador.adapter.getItem(position).toString()
            polinizadorId = trabajadores.first { it.second == nombre }.first
        }

        binding.btnAccion.setOnClickListener {
            // El id se deriva del texto visible: si el operario borró o editó
            // lo que había elegido, la selección deja de ser válida.
            val nombre = binding.acPolinizador.text.toString().trim()
            polinizadorId = trabajadores.firstOrNull { it.second == nombre }?.first ?: 0

            if (polinizadorId == 0) {
                AlertDialog.Builder(this)
                    .setTitle("Falta el polinizador")
                    .setMessage("Debe seleccionar un trabajador de la lista para continuar.")
                    .setPositiveButton("Entendido", null)
                    .show()
                return@setOnClickListener
            }

            val extras = Bundle(intent.extras ?: Bundle())
            extras.putInt("polinizador_id", polinizadorId)
            startActivity(Intent(this, SuperPoli13Activity::class.java).putExtras(extras))
        }
    }
}