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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.palmadata.app.R
import com.palmadata.app.databinding.ActivityTrat82Binding

/**
 * Pantalla 8.2 — REMISIÓN. Un registro puede llevar varias remisiones:
 * "+ Agregar remisión" abre el teclado numérico, y cada una queda en una
 * fila con "−" a la izquierda para quitarla. Viajan como texto separado por
 * comas ("2015,8546,6987"), que es como se guardan en la base. Sin ninguna
 * → NULL.
 */
class Trat8_2Activity : AppCompatActivity() {
    private lateinit var binding: ActivityTrat82Binding
    private val remisiones = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTrat82Binding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnAgregar.setOnClickListener { pedirRemision() }
        binding.btnAccion.setOnClickListener {
            startActivity(Intent(this, Trat9Activity::class.java).also {
                intent.extras?.let { e -> it.putExtras(e) }
                it.putExtra("remision", remisiones.joinToString(","))
            })
        }
        redibujar()
    }

    private fun pedirRemision() {
        val campo = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            hint = "Número de remisión"
            filters = arrayOf(android.text.InputFilter.LengthFilter(9))
        }
        val marco = LinearLayout(this).apply {
            setPadding(dp(24), dp(8), dp(24), 0)
            addView(campo, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        }
        val dialogo = MaterialAlertDialogBuilder(this)
            .setTitle("Agregar remisión")
            .setView(marco)
            .setPositiveButton("Agregar") { d, _ ->
                val v = campo.text.toString().trim().trimStart('0').ifEmpty { if (campo.text.isNotBlank()) "0" else "" }
                if (v.isNotEmpty()) {
                    if (remisiones.contains(v)) {
                        android.widget.Toast.makeText(this, "La remisión $v ya está en la lista", android.widget.Toast.LENGTH_SHORT).show()
                    } else {
                        remisiones.add(v)
                        redibujar()
                    }
                }
                d.dismiss()
            }
            .setNegativeButton("Cancelar") { d, _ -> d.dismiss() }
            .create()
        dialogo.show()
        campo.requestFocus()
        dialogo.window?.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun redibujar() {
        binding.contenedor.removeAllViews()
        binding.tvVacio.visibility = if (remisiones.isEmpty()) View.VISIBLE else View.GONE
        val blanco = ContextCompat.getColor(this, R.color.white)
        val texto  = ContextCompat.getColor(this, R.color.text_primary)
        val rojo   = 0xFFD32F2F.toInt()
        remisiones.forEachIndexed { i, rem ->
            binding.contenedor.addView(LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setBackgroundColor(blanco)
                setPadding(dp(8), dp(4), dp(16), dp(4))
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(dp(12), dp(4), dp(12), 0)
                }
                // "−" a la izquierda: quita esta remisión
                addView(TextView(this@Trat8_2Activity).apply {
                    text = "−"; textSize = 26f; setTextColor(rojo)
                    gravity = Gravity.CENTER
                    setPadding(dp(14), 0, dp(14), 0)
                    setOnClickListener { remisiones.removeAt(i); redibujar() }
                })
                addView(TextView(this@Trat8_2Activity).apply {
                    text = rem; textSize = 17f; setTextColor(texto)
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                })
            })
        }
    }
}
