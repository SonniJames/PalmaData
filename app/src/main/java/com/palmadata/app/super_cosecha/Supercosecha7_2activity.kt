package com.palmadata.app.supercosecha

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.palmadata.app.databinding.ActivitySuperCosecha72Binding

/**
 * Pantalla 7.2 — ESTADO LOTE. Cuatro casillas: marcada = 1, sin marcar = 0.
 *   Platos enmalezados → platos_enmalezados
 *   Malezas en estipe  → malezas_estipe
 *   Lote inundado      → lote_inundado
 *   Poda atrasada      → poda_atrasada
 *
 * Se puede continuar sin marcar ninguna. El botón OBSERVACIONES lleva a la
 * pantalla 8 (antes salía desde la pantalla 7). Arrastra todos los extras
 * acumulados y agrega los cuatro de esta pantalla.
 */
class SuperCosecha7_2Activity : AppCompatActivity() {
    private lateinit var binding: ActivitySuperCosecha72Binding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySuperCosecha72Binding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnAccion.setOnClickListener {
            val extras = Bundle(intent.extras ?: Bundle())
            extras.putInt("platos_enmalezados", if (binding.cbPlatosEnmalezados.isChecked) 1 else 0)
            extras.putInt("malezas_estipe",     if (binding.cbMalezasEstipe.isChecked) 1 else 0)
            extras.putInt("lote_inundado",      if (binding.cbLoteInundado.isChecked) 1 else 0)
            extras.putInt("poda_atrasada",      if (binding.cbPodaAtrasada.isChecked) 1 else 0)
            startActivity(Intent(this, SuperCosecha8Activity::class.java).putExtras(extras))
        }
    }
}
