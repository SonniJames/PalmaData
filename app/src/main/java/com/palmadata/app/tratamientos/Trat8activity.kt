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
import com.palmadata.app.databinding.ActivityTrat8Binding
import com.palmadata.app.utils.aDecimalCampo
import org.json.JSONArray

/**
 * Pantalla 8 — CANTIDADES. Una fila por producto seleccionado (con su unidad
 * debajo del nombre, si la tiene); el recuadro abre el teclado numérico del
 * sistema, solo dígitos y punto. Vacío = cantidad NULL para ese producto.
 *
 * Reemplaza al antiguo teclado de CANTIDAD (un solo valor por registro).
 */
class Trat8Activity : AppCompatActivity() {
    private lateinit var binding: ActivityTrat8Binding
    private lateinit var seleccion: JSONArray
    private val campos = mutableListOf<EditText>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTrat8Binding.inflate(layoutInflater)
        setContentView(binding.root)

        seleccion = ProductosSeleccion.leer(intent)
        if (seleccion.length() == 0) binding.tvVacio.visibility = View.VISIBLE
        for (i in 0 until seleccion.length()) agregarFila(i)

        binding.btnAccion.setOnClickListener {
            for (i in 0 until seleccion.length()) {
                val v = campos[i].text.toString().aDecimalCampo()
                if (v == null) seleccion.getJSONObject(i).remove("cantidad")
                else seleccion.getJSONObject(i).put("cantidad", v)
            }
            startActivity(Intent(this, Trat8_2Activity::class.java).also {
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

        val nombres = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.4f)
            addView(TextView(this@Trat8Activity).apply { text = p.getString("producto_nombre"); textSize = 15f; setTextColor(texto) })
            if (p.has("unidad_nombre")) addView(TextView(this@Trat8Activity).apply { text = p.getString("unidad_nombre"); textSize = 12f; setTextColor(gris) })
        }
        val campo = EditText(this).apply {
            hint = "0.0"
            textSize = 16f
            // Solo dígitos y punto: la coma del teclado no escribe nada
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            keyListener = android.text.method.DigitsKeyListener.getInstance("0123456789.")
            gravity = Gravity.CENTER
            setBackgroundResource(R.color.grid_background)
            setPadding(dp(12), dp(10), dp(12), dp(10))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            if (p.has("cantidad")) setText(p.getDouble("cantidad").toString())
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
