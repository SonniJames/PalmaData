package com.palmadata.app.tratamientos

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.palmadata.app.R
import com.palmadata.app.databinding.ActivityTrat73Binding
import com.palmadata.app.utils.DatabaseHelper
import org.json.JSONArray
import org.json.JSONObject

/**
 * Pantalla 7.3 — PRODUCTOS. Categorías plegadas ("+ Nombre"); al tocar una se
 * despliegan sus productos, cada uno con casilla. Se pueden marcar varios de
 * varias categorías. El botón UNIDADES avanza con los marcados (en el orden
 * en que aparecen en pantalla) o sin ninguno.
 *
 * La lista se arma en código (sin adaptador): son unas decenas de filas y así
 * el estado de cada casilla vive en la propia vista.
 */
class Trat7_3Activity : AppCompatActivity() {
    private lateinit var binding: ActivityTrat73Binding
    /** producto_id → (nombre, casilla), en orden de aparición */
    private val casillas = LinkedHashMap<Int, Pair<String, CheckBox>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTrat73Binding.inflate(layoutInflater)
        setContentView(binding.root)

        val db = DatabaseHelper.getInstance(this)
        val grupos = db.getCategoriasProducto().map { it to db.getProductosPorCategoria(it.first) }
            .filter { it.second.isNotEmpty() }
            .toMutableList()
        // Productos sin categoría (categoria_producto_id nulo en el servidor)
        val sinCategoria = db.getProductosPorCategoria(0)
        if (sinCategoria.isNotEmpty()) grupos.add(Pair(0, "SIN CATEGORÍA") to sinCategoria)

        if (grupos.isEmpty()) binding.tvVacio.visibility = View.VISIBLE
        grupos.forEach { (categoria, productos) -> agregarGrupo(categoria.second, productos) }

        binding.btnAccion.setOnClickListener {
            val sel = JSONArray()
            casillas.forEach { (id, par) ->
                if (par.second.isChecked) sel.put(JSONObject().put("producto_id", id).put("producto_nombre", par.first))
            }
            startActivity(Intent(this, Trat7_4Activity::class.java).also {
                intent.extras?.let { e -> it.putExtras(e) }
                it.putExtra(ProductosSeleccion.EXTRA, sel.toString())
            })
        }
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun agregarGrupo(nombre: String, productos: List<Pair<Int, String>>) {
        val verde  = ContextCompat.getColor(this, R.color.palma_green_dark)
        val blanco = ContextCompat.getColor(this, R.color.white)
        val texto  = ContextCompat.getColor(this, R.color.text_primary)

        val hijos = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
        }
        val cabecera = TextView(this).apply {
            text = "+  $nombre"
            textSize = 16f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(blanco)
            setBackgroundColor(verde)
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(dp(12), dp(6), dp(12), 0)
            }
            setOnClickListener {
                val abrir = hijos.visibility != View.VISIBLE
                hijos.visibility = if (abrir) View.VISIBLE else View.GONE
                text = (if (abrir) "−  " else "+  ") + nombre
            }
        }
        productos.forEach { (id, nombreProducto) ->
            val casilla = CheckBox(this)
            val fila = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setBackgroundColor(blanco)
                setPadding(dp(28), dp(6), dp(12), dp(6))
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(dp(12), dp(2), dp(12), 0)
                }
                addView(TextView(this@Trat7_3Activity).apply {
                    text = nombreProducto; textSize = 15f; setTextColor(texto)
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                })
                addView(casilla)
                // Tocar el nombre también marca/desmarca
                setOnClickListener { casilla.isChecked = !casilla.isChecked }
            }
            hijos.addView(fila)
            casillas[id] = Pair(nombreProducto, casilla)
        }
        binding.contenedor.addView(cabecera)
        binding.contenedor.addView(hijos)
    }
}
