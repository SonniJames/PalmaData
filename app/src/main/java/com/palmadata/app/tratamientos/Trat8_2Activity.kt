package com.palmadata.app.tratamientos

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.palmadata.app.R
import com.palmadata.app.databinding.ActivityTrat82Binding
import org.json.JSONArray

/**
 * Pantalla 8.2 — REMISIÓN. Una fila por producto seleccionado, con su cantidad
 * y unidad bajo el nombre; el recuadro abre el teclado numérico y recibe la
 * remisión de ESE producto. Así queda registrado con qué remisión llegó cada
 * producto, en vez de una sola para todo el tratamiento.
 *
 * Cada remisión viaja dentro de su producto en el JSON (ver ProductosSeleccion):
 * ya no existe una columna `remision` suelta. Vacío = NULL para ese producto,
 * y se puede avanzar sin llenar ninguna.
 */
class Trat8_2Activity : AppCompatActivity() {
    private lateinit var binding: ActivityTrat82Binding
    private lateinit var seleccion: JSONArray
    private val campos = mutableListOf<EditText>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTrat82Binding.inflate(layoutInflater)
        setContentView(binding.root)

        seleccion = ProductosSeleccion.leer(intent)
        if (seleccion.length() == 0) binding.tvVacio.visibility = View.VISIBLE
        for (i in 0 until seleccion.length()) agregarFila(i)

        binding.btnAccion.setOnClickListener {
            for (i in 0 until seleccion.length()) {
                val p = seleccion.getJSONObject(i)
                // Se guarda como número, igual que la columna de antes. Vacío o
                // no numérico → se quita la clave y queda NULL para ese producto.
                val n = campos[i].text.toString().trim().toLongOrNull()
                if (n == null) p.remove("remision") else p.put("remision", n)
            }
            startActivity(Intent(this, Trat9Activity::class.java).also {
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
        val gris   = ContextCompat.getColor(this, R.color.text_secondary)

        // Bajo el nombre, la cantidad y la unidad ya capturadas: sirven para
        // saber a qué producto se le está poniendo cada remisión.
        val detalle = buildString {
            if (p.has("cantidad")) append(p.getDouble("cantidad").toString())
            if (p.has("unidad_nombre")) {
                if (isNotEmpty()) append(" ")
                append(p.getString("unidad_nombre"))
            }
        }
        val nombres = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.4f)
            addView(TextView(this@Trat8_2Activity).apply {
                text = p.getString("producto_nombre"); textSize = 15f; setTextColor(texto)
            })
            if (detalle.isNotEmpty()) addView(TextView(this@Trat8_2Activity).apply {
                text = detalle; textSize = 12f; setTextColor(gris)
            })
        }
        val campo = EditText(this).apply {
            hint = "Remisión"
            textSize = 16f
            inputType = InputType.TYPE_CLASS_NUMBER
            filters = arrayOf(android.text.InputFilter.LengthFilter(9))
            gravity = Gravity.CENTER
            setBackgroundResource(R.color.grid_background)
            setPadding(dp(12), dp(10), dp(12), dp(10))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            if (p.has("remision")) setText(p.getLong("remision").toString())
        }
        campos.add(campo)
        binding.contenedor.addView(LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(blanco)
            setPadding(dp(16), dp(8), dp(16), dp(8))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(dp(12), dp(4), dp(12), 0)
            }
            addView(nombres)
            addView(campo)
        })
    }
}
