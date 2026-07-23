package com.palmadata.app.super_cosecha_vagon

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import com.palmadata.app.databinding.ActivitySuperCosechaVagon4Binding

class SuperCosechaVagon4Activity : AppCompatActivity() {
    private lateinit var binding: ActivitySuperCosechaVagon4Binding

    // Las 7 filas con contadores +/- (R. muestra se lee del EditText)
    private var racimosVerde = 0
    private var racimosSobremaduro = 0
    private var racimosPodridos = 0
    private var pedunculoLargo = 0
    private var racimosMalformados = 0
    private var racimosEnfermos = 0
    private var racimosEupalamides = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySuperCosechaVagon4Binding.inflate(layoutInflater)
        setContentView(binding.root)

        val plantacionId     = intent.getIntExtra("plantacion_id", 0)
        val plantacionNombre = intent.getStringExtra("plantacion_nombre") ?: ""
        val sectorId         = intent.getIntExtra("sector_id", 0)
        val sectorNombre     = intent.getStringExtra("sector_nombre") ?: ""
        val loteId           = intent.getIntExtra("lote_id", 0)
        val loteNombre       = intent.getStringExtra("lote_nombre") ?: ""
        val trabajadorId     = intent.getIntExtra("trabajador_id", 0)

        actualizarDisplays()
        setupContadores()
        setupCampoMuestra()

        binding.btnAccion.setOnClickListener {
            // R. muestra vacío = 0
            val racimosMuestra = binding.etRacimosMuestra.text.toString().trim().toIntOrNull() ?: 0

            startActivity(Intent(this, SuperCosechaVagon5Activity::class.java).also {
                it.putExtra("plantacion_id", plantacionId)
                it.putExtra("plantacion_nombre", plantacionNombre)
                it.putExtra("sector_id", sectorId)
                it.putExtra("sector_nombre", sectorNombre)
                it.putExtra("lote_id", loteId)
                it.putExtra("lote_nombre", loteNombre)
                it.putExtra("trabajador_id", trabajadorId)
                it.putExtra("racimos_muestra", racimosMuestra)
                it.putExtra("racimos_verde", racimosVerde)
                it.putExtra("racimos_sobremaduro", racimosSobremaduro)
                it.putExtra("racimos_podridos", racimosPodridos)
                it.putExtra("pedunculo_largo", pedunculoLargo)
                it.putExtra("racimos_malformados", racimosMalformados)
                it.putExtra("racimos_enfermos", racimosEnfermos)
                it.putExtra("racimos_eupalamides", racimosEupalamides)
            })
        }
    }

    /**
     * Al pulsar la tecla de confirmación del teclado numérico, se baja el
     * teclado y se quita el foco del campo: así el operario puede seguir
     * usando los botones +/- de las demás filas sin estorbos.
     */
    private fun setupCampoMuestra() {
        binding.etRacimosMuestra.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(v.windowToken, 0)
                v.clearFocus()
                true
            } else false
        }
    }

    private fun setupContadores() {
        binding.btnMas1.setOnClickListener   { racimosVerde++;        actualizarDisplays() }
        binding.btnMenos1.setOnClickListener { if (racimosVerde > 0)        { racimosVerde--;        actualizarDisplays() } }
        binding.btnMas2.setOnClickListener   { racimosSobremaduro++;  actualizarDisplays() }
        binding.btnMenos2.setOnClickListener { if (racimosSobremaduro > 0)  { racimosSobremaduro--;  actualizarDisplays() } }
        binding.btnMas3.setOnClickListener   { racimosPodridos++;     actualizarDisplays() }
        binding.btnMenos3.setOnClickListener { if (racimosPodridos > 0)     { racimosPodridos--;     actualizarDisplays() } }
        binding.btnMas4.setOnClickListener   { pedunculoLargo++;      actualizarDisplays() }
        binding.btnMenos4.setOnClickListener { if (pedunculoLargo > 0)      { pedunculoLargo--;      actualizarDisplays() } }
        binding.btnMas5.setOnClickListener   { racimosMalformados++;  actualizarDisplays() }
        binding.btnMenos5.setOnClickListener { if (racimosMalformados > 0)  { racimosMalformados--;  actualizarDisplays() } }
        binding.btnMas6.setOnClickListener   { racimosEnfermos++;     actualizarDisplays() }
        binding.btnMenos6.setOnClickListener { if (racimosEnfermos > 0)     { racimosEnfermos--;     actualizarDisplays() } }
        binding.btnMas7.setOnClickListener   { racimosEupalamides++;  actualizarDisplays() }
        binding.btnMenos7.setOnClickListener { if (racimosEupalamides > 0)  { racimosEupalamides--;  actualizarDisplays() } }
    }

    private fun actualizarDisplays() {
        binding.tvValor1.text = racimosVerde.toString()
        binding.tvValor2.text = racimosSobremaduro.toString()
        binding.tvValor3.text = racimosPodridos.toString()
        binding.tvValor4.text = pedunculoLargo.toString()
        binding.tvValor5.text = racimosMalformados.toString()
        binding.tvValor6.text = racimosEnfermos.toString()
        binding.tvValor7.text = racimosEupalamides.toString()
    }
}