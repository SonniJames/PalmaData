package com.palmadata.app.mapa

import android.app.Activity
import android.app.AlertDialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import com.palmadata.app.data.model.CatalogoCenso
import com.palmadata.app.data.model.EventoCenso
import com.palmadata.app.data.model.MesCenso
import com.palmadata.app.utils.CensoMapaRepo
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Lo que la capa de censo recuerda mientras la pantalla de Mapas está
 * abierta: el mes elegido, sus datos y los eventos marcados. Al volver a
 * abrir la ventana aparece todo como se dejó.
 */
class EstadoCenso {
    var catalogo: CatalogoCenso? = null
    var anio: Int? = null
    var mes: Int? = null
    var datosMes: MesCenso? = null
    val seleccion: MutableSet<Int> = linkedSetOf()
}

/**
 * Ventana de la capa de censo de enfermedades. Es el mismo filtro de Eventos
 * de Mapas · Censo de la página web:
 *
 *   1. Año y Mes son OBLIGATORIOS: mientras no se elijan los dos, la lista de
 *      eventos queda apagada. Así nunca se carga todo el histórico.
 *   2. Al elegirlos se baja ese mes del servidor (o se usa el guardado si no
 *      hay conexión) y se enciende la lista.
 *   3. La lista: todos los eventos, con su color, su enfermedad y cuántas
 *      palmas tienen en el mes; primero los que tienen palmas. Se marcan uno
 *      o varios, con buscador y «Marcar todos» / «Quitar todos».
 *   4. Aceptar pinta las palmas de los eventos marcados.
 *
 * La ventana se arma en código (sin XML) para dejar todo en un solo archivo.
 */
class CensoDialogo(
    private val activity: Activity,
    private val estado: EstadoCenso,
    private val capaActiva: Boolean,
    private val onAceptar: () -> Unit,
    private val onQuitar: () -> Unit
) {
    companion object {
        val MESES = arrayOf("Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio", "Julio",
            "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre")

        // Los mismos tonos de la app y de la página web
        private val VERDE = Color.rgb(46, 125, 50)            // palma_green_medium
        private val TITULO = Color.rgb(50, 110, 55)           // dialog_title_text
        private val TEXTO = Color.rgb(33, 33, 33)
        private val TEXTO_SUAVE = Color.rgb(117, 117, 117)
        private val LINEA = Color.rgb(224, 224, 224)
        private val AVISO = Color.rgb(138, 97, 0)
        private val ERROR = Color.rgb(198, 40, 40)
    }

    private val d = activity.resources.displayMetrics.density
    private fun dp(v: Int) = (v * d).toInt()

    /** Copia de trabajo: solo pasa al estado al dar Aceptar. */
    private val marcados = LinkedHashSet<Int>(estado.seleccion)
    private var datos: MesCenso? = estado.datosMes
    private var anioSel: Int? = estado.anio
    private var mesSel: Int? = estado.mes
    private var filtroTexto = ""
    private var cargando = false
    /** Qué repetir si el usuario toca el mensaje de error. */
    private var reintento: (() -> Unit)? = null

    private lateinit var dialogo: AlertDialog
    private lateinit var spAnio: Spinner
    private lateinit var spMes: Spinner
    private lateinit var tvEstado: TextView
    private lateinit var bloqueEventos: LinearLayout
    private lateinit var etBuscar: EditText
    private lateinit var lista: LinearLayout
    private lateinit var tvTodos: TextView
    private lateinit var tvNinguno: TextView
    private var anios: List<Int> = emptyList()

    fun mostrar() {
        val raiz = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(16), dp(20), dp(4))
        }

        raiz.addView(TextView(activity).apply {
            text = "Censo de enfermedades"
            textSize = 19f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(TITULO)
        })
        raiz.addView(TextView(activity).apply {
            text = "Elija el año y el mes; después, los eventos que quiere ver en el mapa."
            textSize = 13f
            setTextColor(TEXTO_SUAVE)
            setPadding(0, dp(4), 0, dp(12))
        })

        // ── Año y mes ──
        val filaFecha = LinearLayout(activity).apply { orientation = LinearLayout.HORIZONTAL }
        spAnio = Spinner(activity)
        spMes = Spinner(activity)
        filaFecha.addView(campo("AÑO", spAnio), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        filaFecha.addView(View(activity), LinearLayout.LayoutParams(dp(12), 1))
        filaFecha.addView(campo("MES", spMes), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.4f))
        raiz.addView(filaFecha)

        tvEstado = TextView(activity).apply {
            textSize = 13f
            setTextColor(TEXTO_SUAVE)
            setPadding(0, dp(8), 0, dp(10))
            // Tras un error, tocar el mensaje vuelve a intentar
            setOnClickListener { reintento?.invoke() }
        }
        raiz.addView(tvEstado)

        // ── Eventos ──
        bloqueEventos = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
        bloqueEventos.addView(etiqueta("EVENTOS"))
        etBuscar = EditText(activity).apply {
            hint = "Buscar…"
            textSize = 14f
            setSingleLine()
            background = caja(Color.WHITE, LINEA)
            setPadding(dp(10), dp(8), dp(10), dp(8))
        }
        bloqueEventos.addView(etBuscar, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(4) })

        val acciones = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(2), dp(8), dp(2), dp(6))
        }
        tvTodos = enlace("Marcar todos")
        tvNinguno = enlace("Quitar todos")
        acciones.addView(tvTodos, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        acciones.addView(tvNinguno)
        bloqueEventos.addView(acciones)
        bloqueEventos.addView(View(activity).apply { setBackgroundColor(LINEA) },
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1)))

        lista = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
        val scroll = ScrollView(activity).apply {
            addView(lista)
            isVerticalScrollBarEnabled = true
            // La ventana entera también se desplaza (pantallas pequeñas o con
            // el teclado abierto): al tocar la lista, el desplazamiento es de
            // la lista y no de la ventana.
            setOnTouchListener { v, _ ->
                v.parent?.requestDisallowInterceptTouchEvent(true)
                false
            }
        }
        // Alto fijo para la lista, como en la web: el resto de la ventana
        // (año, mes, botones) queda a la vista aunque haya muchos eventos.
        val alto = (activity.resources.displayMetrics.heightPixels * 0.36).toInt()
        bloqueEventos.addView(scroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, alto))
        raiz.addView(bloqueEventos)

        etBuscar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) {
                filtroTexto = s?.toString()?.trim() ?: ""
                pintarLista()
            }
        })
        tvTodos.setOnClickListener {
            visibles().forEach { marcados.add(it.id) }
            pintarLista()
        }
        tvNinguno.setOnClickListener {
            marcados.clear()
            pintarLista()
        }

        val builder = AlertDialog.Builder(activity)
            .setView(ScrollView(activity).apply { addView(raiz) })
            .setPositiveButton("Aceptar", null)       // se conecta después para validar
            .setNegativeButton("Cancelar", null)
        if (capaActiva) builder.setNeutralButton("Quitar del mapa") { _, _ -> onQuitar() }
        dialogo = builder.create()
        // Que no salte el teclado al abrir: el buscador se usa solo si hace falta
        dialogo.window?.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN or
                    WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        dialogo.setOnShowListener {
            dialogo.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener { aceptar() }
            actualizarHabilitado()
        }
        dialogo.show()

        prepararMeses()
        cargarCatalogo()
    }

    // ── Catálogo de eventos y años ────────────────────────────────────────────

    private fun cargarCatalogo() {
        val enMemoria = estado.catalogo
        if (enMemoria != null && !enMemoria.sinConexion) {
            prepararAnios(enMemoria)
            continuarTrasCatalogo()
            return
        }
        cargando = true
        reintento = null
        apagarEventos("Cargando la lista de eventos…")
        Thread {
            val resultado = try { Result.success(CensoMapaRepo.eventos(activity)) }
            catch (e: Exception) { Result.failure(e) }
            activity.runOnUiThread {
                if (!vivo()) return@runOnUiThread
                cargando = false
                resultado.onSuccess { cat ->
                    estado.catalogo = cat
                    prepararAnios(cat)
                    continuarTrasCatalogo()
                }.onFailure { e ->
                    // Sin catálogo no hay lista que mostrar
                    mostrarError(e.message ?: "No se pudo leer la lista de eventos.") { cargarCatalogo() }
                }
            }
        }.start()
    }

    private fun continuarTrasCatalogo() {
        val a = anioSel; val m = mesSel
        val previo = datos
        if (a != null && m != null && previo != null && previo.anio == a && previo.mes == m) {
            mostrarMes(previo)
        } else if (a != null && m != null) {
            cargarMes(a, m)
        } else {
            apagarEventos("Elija el año y el mes para ver los eventos.")
        }
    }

    /** Años con censo, del más reciente al más viejo. */
    private fun prepararAnios(cat: CatalogoCenso) {
        if (anios.isNotEmpty()) return            // ya preparado (reintento)
        val actual = Calendar.getInstance().get(Calendar.YEAR)
        val hasta = cat.hasta?.take(4)?.toIntOrNull() ?: actual
        val desde = cat.desde?.take(4)?.toIntOrNull() ?: (hasta - 3)
        anios = (maxOf(hasta, desde) downTo minOf(hasta, desde)).toList()
        val items = listOf("Año…") + anios.map { it.toString() }
        spAnio.adapter = ArrayAdapter(activity, android.R.layout.simple_spinner_dropdown_item, items)
        val pos = anioSel?.let { anios.indexOf(it) } ?: -1
        if (pos < 0) anioSel = null
        spAnio.setSelection(if (pos >= 0) pos + 1 else 0, false)
        // Los oyentes van al final, con el catálogo ya listo: antes, un cambio
        // de mes no tendría lista de eventos que mostrar.
        spAnio.onItemSelectedListener = oyente
        spMes.onItemSelectedListener = oyente
        spAnio.isEnabled = true
        spMes.isEnabled = true
    }

    private fun prepararMeses() {
        val items = listOf("Mes…") + MESES.toList()
        spMes.adapter = ArrayAdapter(activity, android.R.layout.simple_spinner_dropdown_item, items)
        spMes.setSelection(mesSel ?: 0, false)
        // Apagados hasta tener el catálogo
        spAnio.isEnabled = false
        spMes.isEnabled = false
    }

    private val oyente = object : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            val a = spAnio.selectedItemPosition.let { if (it > 0) anios.getOrNull(it - 1) else null }
            val m = spMes.selectedItemPosition.let { if (it > 0) it else null }
            // Android también avisa al dibujar la ventana por primera vez:
            // si no cambió nada, no se hace nada.
            if (a == anioSel && m == mesSel) return
            anioSel = a; mesSel = m
            if (a == null || m == null) {
                datos = null
                cargando = false
                apagarEventos("Elija el año y el mes para ver los eventos.")
            } else {
                cargarMes(a, m)
            }
        }
        override fun onNothingSelected(parent: AdapterView<*>?) {}
    }

    // ── El mes ────────────────────────────────────────────────────────────────

    private fun cargarMes(anio: Int, mes: Int) {
        datos = null
        cargando = true
        reintento = null
        apagarEventos("Descargando ${MESES[mes - 1].lowercase()} de $anio…")
        Thread {
            val resultado = try { Result.success(CensoMapaRepo.mes(activity, anio, mes)) }
            catch (e: Exception) { Result.failure(e) }
            activity.runOnUiThread {
                if (!vivo()) return@runOnUiThread
                // Si mientras bajaba se cambió de mes, esta respuesta ya no sirve
                if (anioSel != anio || mesSel != mes) return@runOnUiThread
                cargando = false
                resultado.onSuccess { mostrarMes(it) }
                    .onFailure { mostrarError(it.message ?: "No se pudo leer el mes.") { cargarMes(anio, mes) } }
            }
        }.start()
    }

    private fun mostrarMes(m: MesCenso) {
        datos = m
        reintento = null
        val sinUbicacion = m.puntos.count { it.lat == null || it.lon == null }
        val texto = StringBuilder()
        texto.append("${MESES[m.mes - 1]} ${m.anio}: ${miles(m.puntos.size)} palmas censadas")
        if (sinUbicacion > 0) texto.append(" (${sinUbicacion} sin ubicación)")
        texto.append(". Cada palma con su último registro del mes.")
        if (m.sinConexion) {
            texto.append("\nSin conexión con el servidor: datos guardados el ${fecha(m.guardadoMs)}.")
            tvEstado.setTextColor(AVISO)
        } else {
            tvEstado.setTextColor(TEXTO_SUAVE)
        }
        tvEstado.text = texto
        encenderEventos(true)
        pintarLista()
    }

    private fun mostrarError(mensaje: String, repetir: () -> Unit) {
        datos = null
        reintento = repetir
        tvEstado.text = "$mensaje\n\nToque este mensaje para volver a intentar."
        tvEstado.setTextColor(ERROR)
        encenderEventos(false)
        lista.removeAllViews()
        actualizarHabilitado()
    }

    private fun apagarEventos(mensaje: String) {
        tvEstado.text = mensaje
        tvEstado.setTextColor(TEXTO_SUAVE)
        encenderEventos(false)
        pintarLista()
    }

    private fun encenderEventos(si: Boolean) {
        bloqueEventos.alpha = if (si) 1f else 0.45f
        etBuscar.isEnabled = si
        tvTodos.isEnabled = si
        tvNinguno.isEnabled = si
        actualizarHabilitado()
    }

    // ── La lista de eventos ───────────────────────────────────────────────────

    /** Igual que la web: primero los eventos con palmas en el mes, en el orden de la lista. */
    private fun ordenados(): List<Pair<EventoCenso, Int>> {
        val cat = estado.catalogo ?: return emptyList()
        val porEvento = HashMap<Int, Int>()
        datos?.puntos?.forEach { porEvento[it.eventoId] = (porEvento[it.eventoId] ?: 0) + 1 }
        val todos = cat.eventos.map { it to (porEvento[it.id] ?: 0) }
        return todos.filter { it.second > 0 } + todos.filter { it.second == 0 }
    }

    /** El buscador mira el código del evento, como en la web. */
    private fun filasVisibles(): List<Pair<EventoCenso, Int>> {
        val f = filtroTexto.lowercase()
        return ordenados().filter { (e, _) -> f.isEmpty() || e.codigo.lowercase().contains(f) }
    }

    private fun visibles(): List<EventoCenso> = filasVisibles().map { it.first }

    private fun pintarLista() {
        lista.removeAllViews()
        val activa = datos != null
        val filas = filasVisibles()
        val cat = estado.catalogo
        if (filas.isEmpty() && cat != null) {
            lista.addView(TextView(activity).apply {
                text = if (cat.eventos.isEmpty()) "Sin eventos en el catálogo." else "Nada coincide."
                textSize = 13f
                setTextColor(TEXTO_SUAVE)
                setPadding(dp(6), dp(12), dp(6), dp(12))
            })
        }
        for ((e, n) in filas) lista.addView(fila(e, n, activa))
        actualizarHabilitado()
    }

    private fun fila(e: EventoCenso, n: Int, activa: Boolean): View {
        val fila = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(2), dp(4), dp(4), dp(4))
            minimumHeight = dp(48)
            isEnabled = activa
        }
        val chk = CheckBox(activity).apply {
            isChecked = marcados.contains(e.id)
            isEnabled = activa
            buttonTintList = ColorStateList.valueOf(VERDE)
            // El toque lo maneja toda la fila
            isClickable = false
            isFocusable = false
        }
        fila.addView(chk)
        fila.addView(View(activity).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(e.color)
            }
        }, LinearLayout.LayoutParams(dp(11), dp(11)).apply { marginEnd = dp(10) })

        val textos = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
        textos.addView(TextView(activity).apply {
            text = e.codigo
            textSize = 15f
            setTextColor(TEXTO)
        })
        if (e.enfermedad.isNotBlank()) {
            textos.addView(TextView(activity).apply {
                text = e.enfermedad
                textSize = 12f
                setTextColor(TEXTO_SUAVE)
            })
        }
        fila.addView(textos, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        if (activa) {
            fila.addView(TextView(activity).apply {
                text = miles(n)
                textSize = 13f
                setTextColor(TEXTO_SUAVE)
                setPadding(dp(10), 0, 0, 0)
            })
        }
        fila.setOnClickListener {
            if (!activa) return@setOnClickListener
            if (marcados.contains(e.id)) marcados.remove(e.id) else marcados.add(e.id)
            chk.isChecked = marcados.contains(e.id)
            actualizarHabilitado()
        }
        return fila
    }

    // ── Aceptar ───────────────────────────────────────────────────────────────

    private fun actualizarHabilitado() {
        if (!::dialogo.isInitialized) return
        val b = dialogo.getButton(AlertDialog.BUTTON_POSITIVE) ?: return
        b.isEnabled = datos != null && !cargando
    }

    private fun aceptar() {
        val m = datos ?: return
        // Sin eventos y sin capa puesta no hay nada que hacer: la ventana
        // sigue abierta. Con la capa puesta, aceptar sin eventos la quita.
        if (marcados.isEmpty() && !capaActiva) {
            android.widget.Toast.makeText(activity, "Seleccione uno o varios eventos",
                android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        estado.anio = m.anio
        estado.mes = m.mes
        estado.datosMes = m
        estado.seleccion.clear()
        estado.seleccion.addAll(marcados)
        dialogo.dismiss()
        onAceptar()
    }

    // ── Piezas de la ventana ──────────────────────────────────────────────────

    private fun vivo(): Boolean = !activity.isFinishing && !activity.isDestroyed && dialogo.isShowing

    private fun etiqueta(t: String) = TextView(activity).apply {
        text = t
        textSize = 11f
        letterSpacing = 0.08f
        setTypeface(typeface, Typeface.BOLD)
        setTextColor(TEXTO_SUAVE)
    }

    /** Etiqueta arriba y el selector dentro de una caja con borde. El
     *  selector conserva su fondo propio, que es el que dibuja la flecha. */
    private fun campo(titulo: String, control: View): View =
        LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            addView(etiqueta(titulo))
            val marco = LinearLayout(activity).apply {
                background = caja(Color.WHITE, LINEA)
                setPadding(dp(2), dp(2), dp(2), dp(2))
                addView(control, LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            }
            addView(marco, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(2) })
        }

    private fun enlace(t: String) = TextView(activity).apply {
        text = t
        textSize = 13f
        setTypeface(typeface, Typeface.BOLD)
        setTextColor(VERDE)
        setPadding(0, dp(4), 0, dp(4))
    }

    private fun caja(fondo: Int, borde: Int) = GradientDrawable().apply {
        setColor(fondo)
        setStroke(dp(1), borde)
        cornerRadius = dp(8).toFloat()
    }

    private fun miles(n: Int): String = String.format(Locale.US, "%,d", n).replace(',', '.')

    private fun fecha(ms: Long): String =
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.forLanguageTag("es-CO")).format(Date(ms))
}
