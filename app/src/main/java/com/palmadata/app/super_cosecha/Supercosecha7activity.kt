package com.palmadata.app.supercosecha

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.palmadata.app.databinding.ActivitySuperCosecha7Binding

class SuperCosecha7Activity : AppCompatActivity() {
    private lateinit var binding: ActivitySuperCosecha7Binding
    private var racimosRecogidos = 0
    private var racimosVerdes = 0
    private var racimossobremaduros = 0
    private var racimosPodridos = 0
    private var racimossinrecoger = 0
    private var racimossincortar = 0
    private var racimorobado = 0
    private var hojasmalacomo = 0
    private var hojacolgando = 0
    private var frutoplato = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySuperCosecha7Binding.inflate(layoutInflater)
        setContentView(binding.root)

        val plantacionId = intent.getIntExtra("plantacion_id", 0)
        val loteId       = intent.getIntExtra("lote_id", 0)
        val ciclo        = intent.getStringExtra("ciclo") ?: "0"
        val cortadorId   = intent.getIntExtra("cortador_id", 0)
        val recolectorId = intent.getIntExtra("recolector_id", 0)
        val linea        = intent.getStringExtra("linea") ?: ""
        val palma        = intent.getStringExtra("palma") ?: ""

        actualizarDisplays()
        setupContadores()

        binding.btnAccion.setOnClickListener {
            startActivity(Intent(this, SuperCosecha8Activity::class.java).also {
                it.putExtra("plantacion_id", plantacionId); it.putExtra("lote_id", loteId)
                it.putExtra("ciclo", ciclo); it.putExtra("cortador_id", cortadorId)
                it.putExtra("recolector_id", recolectorId); it.putExtra("linea", linea)
                it.putExtra("palma", palma)
                it.putExtra("racimos_recogidos", racimosRecogidos)
                it.putExtra("racimos_verdes", racimosVerdes)
                it.putExtra("racimos_sobremaduros", racimossobremaduros)
                it.putExtra("racimos_podridos", racimosPodridos)
                it.putExtra("racimossinrecoger", racimossinrecoger)
                it.putExtra("racimossincortar", racimossincortar)
                it.putExtra("racimorobado", racimorobado)
                it.putExtra("hojasmalacomo", hojasmalacomo)
                it.putExtra("hojacolgando", hojacolgando)
                it.putExtra("frutoplato", frutoplato)
            })
        }
    }

    private fun setupContadores() {
        binding.btnMas1.setOnClickListener  { racimosRecogidos++;    actualizarDisplays() }
        binding.btnMenos1.setOnClickListener { if (racimosRecogidos > 0) { racimosRecogidos--; actualizarDisplays() } }
        binding.btnMas2.setOnClickListener  { racimosVerdes++;       actualizarDisplays() }
        binding.btnMenos2.setOnClickListener { if (racimosVerdes > 0) { racimosVerdes--; actualizarDisplays() } }
        binding.btnMas3.setOnClickListener  { racimossobremaduros++; actualizarDisplays() }
        binding.btnMenos3.setOnClickListener { if (racimossobremaduros > 0) { racimossobremaduros--; actualizarDisplays() } }
        binding.btnMas4.setOnClickListener  { racimosPodridos++;     actualizarDisplays() }
        binding.btnMenos4.setOnClickListener { if (racimosPodridos > 0) { racimosPodridos--; actualizarDisplays() } }
        binding.btnMas5.setOnClickListener  { racimossinrecoger++;   actualizarDisplays() }
        binding.btnMenos5.setOnClickListener { if (racimossinrecoger > 0) { racimossinrecoger--; actualizarDisplays() } }
        binding.btnMas6.setOnClickListener  { racimossincortar++;    actualizarDisplays() }
        binding.btnMenos6.setOnClickListener { if (racimossincortar > 0) { racimossincortar--; actualizarDisplays() } }
        binding.btnMas7.setOnClickListener  { racimorobado++;        actualizarDisplays() }
        binding.btnMenos7.setOnClickListener { if (racimorobado > 0) { racimorobado--; actualizarDisplays() } }
        binding.btnMas8.setOnClickListener  { hojasmalacomo++;       actualizarDisplays() }
        binding.btnMenos8.setOnClickListener { if (hojasmalacomo > 0) { hojasmalacomo--; actualizarDisplays() } }
        binding.btnMas9.setOnClickListener  { hojacolgando++;        actualizarDisplays() }
        binding.btnMenos9.setOnClickListener { if (hojacolgando > 0) { hojacolgando--; actualizarDisplays() } }
        binding.btnMas10.setOnClickListener { frutoplato++;          actualizarDisplays() }
        binding.btnMenos10.setOnClickListener { if (frutoplato > 0) { frutoplato--; actualizarDisplays() } }
    }

    private fun actualizarDisplays() {
        binding.tvValor1.text  = racimosRecogidos.toString()
        binding.tvValor2.text  = racimosVerdes.toString()
        binding.tvValor3.text  = racimossobremaduros.toString()
        binding.tvValor4.text  = racimosPodridos.toString()
        binding.tvValor5.text  = racimossinrecoger.toString()
        binding.tvValor6.text  = racimossincortar.toString()
        binding.tvValor7.text  = racimorobado.toString()
        binding.tvValor8.text  = hojasmalacomo.toString()
        binding.tvValor9.text  = hojacolgando.toString()
        binding.tvValor10.text = frutoplato.toString()
    }
}