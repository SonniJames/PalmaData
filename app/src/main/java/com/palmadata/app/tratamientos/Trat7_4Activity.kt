package com.palmadata.app.tratamientos

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.palmadata.app.R
import com.palmadata.app.databinding.ActivityTrat74Binding
import com.palmadata.app.utils.DatabaseHelper
import org.json.JSONArray

/**
 * Pantalla 7.4 — UNIDADES. Una fila por producto seleccionado en PRODUCTOS;
 * el recuadro de la derecha abre la lista de unidades (unidad_aplicacion) y
 * deja la elegida. Se puede avanzar con productos sin unidad (queda NULL).
 */
class Trat7_4Activity : AppCompatActivity() {
    private lateinit var binding: ActivityTrat74Binding
    private lateinit var seleccion: JSONArray
    private lateinit var unidades: List<Pair<Int, String>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTrat74Binding.inflate(layoutInflater)
        setContentView(binding.root)

        seleccion = ProductosSeleccion.leer(intent)
        unidades  = DatabaseHelper.getInstance(this).getUnidadesAplicacion()
        if (seleccion.length() == 0) binding.tvVacio.visibility = View.VISIBLE

        for (i in 0 until seleccion.length()) agregarFila(i)

        binding.btnAccion.setOnClickListener {
            startActivity(Intent(this, Trat8Activity::class.java).also {
                intent.extras?.let { e -> it.putExtras(e) }
                it.putExtra(ProductosSeleccion.EXTRA, seleccion.toString())
            })
        }
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun agregarFila(i: Int) {
        val p = seleccion.getJSONObject(i)
        val blanco = ContextCompat.getColor(this, R.color.white)
        val texto  = ContextCompat.getColor(this, R.color.text_primary)
        val verde  = ContextCompat.getColor(this, R.color.palma_green_dark)

        val recuadro = TextView(this).apply {
            text = if (p.has("unidad_nombre")) p.getString("unidad_nombre") else "(Unidad)"
            textSize = 14f
            setTextColor(verde)
            gravity = Gravity.CENTER
            setBackgroundResource(R.color.grid_background)
            setPadding(dp(12), dp(10), dp(12), dp(10))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener {
                if (unidades.isEmpty()) return@setOnClickListener
                MaterialAlertDialogBuilder(this@Trat7_4Activity)
                    .setTitle(p.getString("producto_nombre"))
                    .setItems(unidades.map { it.second }.toTypedArray()) { d, pos ->
                        p.put("unidad_aplicacion_id", unidades[pos].first)
                        p.put("unidad_nombre", unidades[pos].second)
                        text = unidades[pos].second
                        d.dismiss()
                    }
                    .setNegativeButton("Cancelar") { d, _ -> d.dismiss() }
                    .show()
            }
        }
        val fila = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(blanco)
            setPadding(dp(16), dp(8), dp(16), dp(8))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(dp(12), dp(4), dp(12), 0)
            }
            addView(TextView(this@Trat7_4Activity).apply {
                text = p.getString("producto_nombre"); textSize = 15f; setTextColor(texto)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.4f)
            })
            addView(recuadro)
        }
        binding.contenedor.addView(fila)
    }
}
