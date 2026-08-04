package com.palmadata.app.super_tiempos

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.palmadata.app.MainActivity
import com.palmadata.app.databinding.ActivitySuperTiempos1Binding
import com.palmadata.app.service.TrackingService
import com.palmadata.app.utils.DatabaseHelper
import com.palmadata.app.utils.SessionManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Pantalla de captura del módulo de SUPERVISIÓN DE TIEMPOS (formulario 35).
 *
 * ── Modelo de interacción ────────────────────────────────────────────────────
 * Tres eventos con el mismo ciclo INICIO → FIN, MUTUAMENTE EXCLUYENTES: al
 * abrir uno se bloquean los inicios de los otros dos.
 *
 * La exclusión no es cosmética. Si la supervisora pudiera abrir un corte,
 * olvidar el FIN y abrir una parada, quedarían dos eventos solapados: al sumar
 * duraciones habría más minutos de eventos que minutos de jornada y ningún
 * promedio sería confiable, que es justo lo que el módulo existe para calcular.
 * Como un trabajador no puede cortar y descansar a la vez, la exclusión también
 * es fiel a la realidad.
 *
 * Efecto secundario valioso: los huecos entre el fin de un evento y el inicio
 * del siguiente quedan interpretables como desplazamiento entre palmas, una
 * categoría que se obtiene sin que ella registre nada.
 *
 * ── Corte ────────────────────────────────────────────────────────────────────
 * INICIO se marca cuando el trabajador LLEGA a la palma y empieza a analizarla,
 * no cuando da el primer machetazo: el tiempo de evaluación es parte del ciclo.
 * El contador suma un racimo por toque; el botón menos corrige toques de más.
 * Cero racimos es un dato válido (revisó y no había nada maduro), por eso al
 * cerrar en cero se pide confirmación en vez de bloquear.
 *
 * ── Sin visual ───────────────────────────────────────────────────────────────
 * Para cuando ella deja de ver al trabajador. Sin este estado, ese tiempo se
 * cerraría como desplazamiento y entrarían etiquetas inventadas al
 * entrenamiento. Se registra para poder excluirlo.
 */
class SuperTiempos1Activity : AppCompatActivity() {

    private lateinit var binding: ActivitySuperTiempos1Binding
    private lateinit var db: DatabaseHelper

    private var supervisorId = 0
    private var cortadorId   = 0
    private var cortadorNombre = ""

    private var sesionId  = ""
    private var secuencia = 0

    // ── Evento en curso ──────────────────────────────────────────────────────
    private var eventoActivo: String? = null      // null = ninguno abierto
    private var inicioMs      = 0L
    private var inicioHora    = ""
    private var inicioLat: Double? = null
    private var inicioLon: Double? = null
    private var desfaseGpsS: Double? = null
    private var racimos       = 0
    private var tipoParada: String? = null

    // Catálogo de tipos de parada, descargado del servidor con los demás
    // maestros. La app nunca lo tiene fijo: así se pueden agregar o retirar
    // tipos sin publicar una versión nueva del APK.
    private var tipos: List<SuperTiemposTipo> = emptyList()

    private val hCrono = Handler(Looper.getMainLooper())
    private lateinit var tickCrono: Runnable

    private val fmtFecha = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val fmtHora  = SimpleDateFormat("HH:mm:ss",   Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySuperTiempos1Binding.inflate(layoutInflater)
        setContentView(binding.root)

        db = DatabaseHelper.getInstance(this)

        supervisorId   = SessionManager.getCurrentWorker(this)?.code?.toIntOrNull() ?: 0
        cortadorId     = intent.getIntExtra(SuperTiemposActivity.EXTRA_CORTADOR_ID, 0)
        cortadorNombre = intent.getStringExtra(SuperTiemposActivity.EXTRA_CORTADOR_NOMBRE).orEmpty()

        binding.tvCortador.text = cortadorNombre

        // Una sesión = una supervisora siguiendo a un cortador. Si ella cambia
        // de cortador se abre otra, para no mezclar denominadores.
        sesionId  = restaurarOCrearSesion()
        secuencia = prefs().getInt(K_SECUENCIA, 0)

        configurarTipos()
        configurarBotones()
        restaurarEventoEnCurso()   // el sistema pudo matar la activity
        actualizarUi()
        arrancarCronometro()
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Configuración de UI
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Carga el catálogo desde SQLite. La tabla local se crea ya sembrada con
     * los tipos por defecto, así que un equipo que todavía no ha sincronizado
     * puede trabajar igual; la sincronización los reemplaza por los del
     * servidor.
     *
     * El Spinner guarda objetos SuperTiemposTipo y muestra su codigo (toString),
     * de modo que al cerrar la parada se guarda el CÓDIGO del catálogo y no el
     * texto que ve la supervisora. Si mañana se corrige una descripción, los
     * registros históricos siguen apuntando al mismo tipo.
     */
    private fun configurarTipos() {
        tipos = db.getSuperTiemposTipos()

        if (tipos.isEmpty()) {
            // No debería ocurrir (la tabla se siembra al crearse), pero si
            // alguien la vacía es mejor trabajar con los de fábrica que dejar
            // a la supervisora sin poder registrar paradas.
            tipos = TiposParadaDefecto.LISTA
            Toast.makeText(this,
                "Catálogo de tipos vacío: se usan los valores por defecto. Sincronice.",
                Toast.LENGTH_LONG).show()
        }

        // El primer elemento es el marcador de "sin elegir": obliga a un toque
        // consciente y evita que se guarde el primer tipo por descuido.
        binding.spTipoParada.adapter = ArrayAdapter<Any>(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            listOf<Any>(SIN_TIPO) + tipos
        )
    }

    private fun configurarBotones() {
        binding.btnCorteInicio.setOnClickListener { abrirEvento(TiposEvento.CORTE) }
        binding.btnCorteFin.setOnClickListener    { intentarCerrarCorte() }

        // El número es el botón de sumar: es el objetivo más grande de la
        // pantalla porque es el que más se pulsa, caminando y sin mirar.
        binding.btnRacimosMas.setOnClickListener {
            racimos++
            binding.btnRacimosMas.text = racimos.toString()
        }
        binding.btnRacimosMenos.setOnClickListener {
            if (racimos > 0) racimos--
            binding.btnRacimosMas.text = racimos.toString()
        }

        binding.btnParadaInicio.setOnClickListener { abrirEvento(TiposEvento.PARADA) }
        binding.btnParadaFin.setOnClickListener    { intentarCerrarParada() }

        binding.btnSinVisualInicio.setOnClickListener { abrirEvento(TiposEvento.SIN_VISUAL) }
        binding.btnSinVisualFin.setOnClickListener    { cerrarEvento(null, null) }

        // Descartar el evento abierto sin guardarlo: para cuando se pulsó
        // INICIO por error. Es preferible a que lo cierre y quede un evento
        // basura en el conjunto de entrenamiento.
        binding.btnCancelar.setOnClickListener { confirmarCancelar() }

        binding.btnFinalizar.setOnClickListener { confirmarFinJornada() }
        binding.btnCambiarCortador.setOnClickListener { confirmarCambioCortador() }
    }

    /**
     * Habilita solo lo que tiene sentido en el estado actual. Un botón
     * deshabilitado comunica mejor que un mensaje de error después del toque.
     */
    private fun actualizarUi() {
        val libre  = eventoActivo == null
        val corte  = eventoActivo == TiposEvento.CORTE
        val parada = eventoActivo == TiposEvento.PARADA
        val sinVis = eventoActivo == TiposEvento.SIN_VISUAL

        binding.btnCorteInicio.isEnabled = libre
        binding.btnCorteFin.isEnabled    = corte
        binding.btnRacimosMas.isEnabled  = corte
        binding.btnRacimosMenos.isEnabled = corte

        binding.btnParadaInicio.isEnabled = libre
        binding.btnParadaFin.isEnabled    = parada
        binding.spTipoParada.isEnabled    = parada

        binding.btnSinVisualInicio.isEnabled = libre
        binding.btnSinVisualFin.isEnabled    = sinVis

        binding.btnCancelar.isEnabled        = !libre
        binding.btnCambiarCortador.isEnabled = libre

        binding.btnRacimosMas.text = racimos.toString()

        binding.tvEstado.text = when (eventoActivo) {
            TiposEvento.CORTE      -> "CORTE en curso"
            TiposEvento.PARADA     -> "PARADA en curso"
            TiposEvento.SIN_VISUAL -> "SIN VISUAL en curso"
            else                   -> "Sin evento activo"
        }
        binding.tvResumen.text = "Eventos registrados: $secuencia"
    }

    /**
     * Cronómetro visible del evento en curso.
     *
     * Es la principal defensa contra el olvido del FIN: un corte que lleva
     * 14 minutos salta a la vista, mientras que un botón sin retroalimentación
     * puede quedarse abierto media jornada y arruinar todos los promedios.
     */
    private fun arrancarCronometro() {
        tickCrono = object : Runnable {
            override fun run() {
                if (eventoActivo != null) {
                    val s = (System.currentTimeMillis() - inicioMs) / 1000
                    binding.tvCronometro.text =
                        String.format(Locale.getDefault(), "%02d:%02d", s / 60, s % 60)
                    // Aviso pasivo: pasados 10 minutos casi siempre es un FIN
                    // olvidado, no un evento real de esa duración.
                    binding.tvCronometro.alpha = if (s > 600) 0.45f else 1f
                } else {
                    binding.tvCronometro.text = "--:--"
                    binding.tvCronometro.alpha = 1f
                }
                hCrono.postDelayed(this, 1_000L)
            }
        }
        hCrono.post(tickCrono)
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Ciclo de vida del evento
    // ─────────────────────────────────────────────────────────────────────────

    private fun abrirEvento(tipo: String) {
        if (eventoActivo != null) return       // exclusión mutua

        eventoActivo = tipo
        inicioMs     = System.currentTimeMillis()
        inicioHora   = fmtHora.format(Date(inicioMs))
        inicioLat    = SessionManager.getLastLatitude(this).takeIf { it != 0.0 }
        inicioLon    = SessionManager.getLastLongitude(this).takeIf { it != 0.0 }
        desfaseGpsS  = calcularDesfaseGps()
        racimos      = 0
        tipoParada   = null
        binding.spTipoParada.setSelection(0)

        persistirEventoEnCurso()
        actualizarUi()
    }

    private fun intentarCerrarCorte() {
        if (racimos > 0) { cerrarEvento(racimos, null); return }

        // Cero racimos es válido: revisó la palma y no había nada maduro. Pero
        // también es el síntoma de haber olvidado el contador, así que se
        // confirma en vez de bloquear o de asumir.
        AlertDialog.Builder(this)
            .setTitle("Racimos en cero")
            .setMessage("¿Es este el número real de racimos cortados?")
            .setPositiveButton("Aceptar") { _, _ -> cerrarEvento(0, null) }
            .setNegativeButton("Rechazar", null)   // vuelve para corregir
            .show()
    }

    private fun intentarCerrarParada() {
        val sel = binding.spTipoParada.selectedItem
        if (sel !is SuperTiemposTipo) {
            Toast.makeText(this, "Seleccione tipo de parada", Toast.LENGTH_SHORT).show()
            return
        }
        cerrarEvento(null, sel.codigo)
    }

    private fun cerrarEvento(racimosFinal: Int?, tipo: String?) {
        val tipoEvento = eventoActivo ?: return
        val finMs   = System.currentTimeMillis()
        val finLat  = SessionManager.getLastLatitude(this).takeIf { it != 0.0 }
        val finLon  = SessionManager.getLastLongitude(this).takeIf { it != 0.0 }

        // El lote NO se calcula aquí: se deriva en PostgreSQL a partir de la
        // coordenada de inicio, con ST_Contains contra cat_lote.
        //
        // Podría hacerse en la app con LotePoligono, pero sería peor: la
        // geometría que descarga el equipo viene simplificada a 6 decimales por
        // /lotes_mapa, y en los linderos esa diferencia decide mal. Además, un
        // valor congelado desde el celular no se puede recalcular si mañana se
        // corrige un polígono, y un equipo que aún no ha sincronizado
        // lotes_mapa asignaría cero a todo sin avisar.
        //
        // Lo único que se necesita para derivarlo son las coordenadas, que sí
        // viajan en el registro.

        secuencia++

        db.guardarSuperTiempos(
            SuperTiemposRegistro(
                idUnico   = UUID.randomUUID().toString(),
                sesionId  = sesionId,
                secuencia = secuencia,
                supervisor = supervisorId,
                trabajador = cortadorId,
                evento     = tipoEvento,
                tipoParada = tipo,
                racimos    = racimosFinal,
                fecha      = fmtFecha.format(Date(inicioMs)),
                horaInicio = inicioHora,
                horaFin    = fmtHora.format(Date(finMs)),
                duracionSegundos   = ((finMs - inicioMs) / 1000).toInt(),
                desfaseGpsSegundos = desfaseGpsS,
                latitudInicio  = inicioLat,
                longitudInicio = inicioLon,
                latitudFin     = finLat,
                longitudFin    = finLon,
                observaciones  = binding.etObservaciones.text?.toString().orEmpty().take(250),
                equipo   = SessionManager.getEquipoId(this),
                idEquipo = SessionManager.getIdEquipo(this)
            )
        )

        binding.etObservaciones.setText("")
        limpiarEventoEnCurso()
        eventoActivo = null
        actualizarUi()
    }

    private fun confirmarCancelar() {
        AlertDialog.Builder(this)
            .setTitle("Descartar evento")
            .setMessage("El evento en curso no se guardará. ¿Continuar?")
            .setPositiveButton("Descartar") { _, _ ->
                limpiarEventoEnCurso()
                eventoActivo = null
                actualizarUi()
            }
            .setNegativeButton("Volver", null)
            .show()
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Persistencia del evento abierto
    //
    //  Android puede matar la activity con el evento corriendo (memoria,
    //  pantalla apagada, otra app encima). Sin esto se perdería el INICIO y la
    //  supervisora tendría que rehacerlo sin saber a qué hora empezó.
    // ─────────────────────────────────────────────────────────────────────────

    private fun prefs() = getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun persistirEventoEnCurso() {
        prefs().edit()
            .putString(K_EVENTO, eventoActivo)
            .putLong(K_INICIO_MS, inicioMs)
            .putString(K_INICIO_HORA, inicioHora)
            .putFloat(K_LAT, (inicioLat ?: 0.0).toFloat())
            .putFloat(K_LON, (inicioLon ?: 0.0).toFloat())
            .putInt(K_SECUENCIA, secuencia)
            .apply()
    }

    private fun restaurarEventoEnCurso() {
        val p = prefs()
        val ev = p.getString(K_EVENTO, null) ?: return
        val ini = p.getLong(K_INICIO_MS, 0L)

        // Un evento de más de 6 horas es basura de un día anterior, no algo
        // que la supervisora quiera continuar.
        if (System.currentTimeMillis() - ini > 6 * 60 * 60 * 1000L) {
            limpiarEventoEnCurso(); return
        }

        eventoActivo = ev
        inicioMs     = ini
        inicioHora   = p.getString(K_INICIO_HORA, "").orEmpty()
        inicioLat    = p.getFloat(K_LAT, 0f).toDouble().takeIf { it != 0.0 }
        inicioLon    = p.getFloat(K_LON, 0f).toDouble().takeIf { it != 0.0 }

        Toast.makeText(this, "Se recuperó el evento en curso", Toast.LENGTH_SHORT).show()
    }

    private fun limpiarEventoEnCurso() {
        prefs().edit()
            .remove(K_EVENTO).remove(K_INICIO_MS).remove(K_INICIO_HORA)
            .remove(K_LAT).remove(K_LON)
            .putInt(K_SECUENCIA, secuencia)
            .apply()
    }

    private fun restaurarOCrearSesion(): String {
        val p = prefs()
        val hoy = fmtFecha.format(Date())
        val mismaSesion = p.getString(K_SESION_FECHA, "") == hoy &&
                p.getInt(K_SESION_CORTADOR, -1) == cortadorId
        if (mismaSesion) return p.getString(K_SESION_ID, "").orEmpty()

        val nueva = UUID.randomUUID().toString()
        p.edit()
            .putString(K_SESION_ID, nueva)
            .putString(K_SESION_FECHA, hoy)
            .putInt(K_SESION_CORTADOR, cortadorId)
            .putInt(K_SECUENCIA, 0)
            .apply()
        return nueva
    }

    /**
     * Segundos de diferencia entre el reloj del equipo y la hora del último fix
     * GPS. Ver la nota de SuperTiemposRegistro.desfaseGpsSegundos.
     */
    private fun calcularDesfaseGps(): Double? {
        val fixMs = SessionManager.getLastFixTime(this)
        if (fixMs <= 0L) return null
        // Un fix de hace más de dos minutos no sirve como referencia de reloj.
        if (System.currentTimeMillis() - fixMs > 120_000L) return null
        return (System.currentTimeMillis() - fixMs) / 1000.0
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Salidas
    // ─────────────────────────────────────────────────────────────────────────

    private fun confirmarCambioCortador() {
        AlertDialog.Builder(this)
            .setTitle("Cambiar cortador")
            .setMessage("Se abrirá una nueva sesión de observación. Los eventos ya registrados se conservan.")
            .setPositiveButton("Cambiar") { _, _ ->
                startActivity(Intent(this, SuperTiemposActivity::class.java))
                finish()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun confirmarFinJornada() {
        if (eventoActivo != null) {
            Toast.makeText(this,
                "Cierre o descarte el evento en curso antes de finalizar",
                Toast.LENGTH_LONG).show()
            return
        }
        AlertDialog.Builder(this)
            .setTitle("Finalizar jornada")
            .setMessage("El registro de recorrido se detendrá hasta mañana. ¿Desea continuar?")
            .setPositiveButton("Finalizar") { _, _ -> finalizarJornada() }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    /** Mismo mecanismo que el módulo de estudio de tiempos. */
    private fun finalizarJornada() {
        limpiarEventoEnCurso()
        SessionManager.cerrarJornadaHoy(this)
        stopService(Intent(this, TrackingService::class.java))

        Toast.makeText(this,
            "Fin de jornada: el registro se detiene y reinicia mañana al abrir la app.",
            Toast.LENGTH_LONG).show()

        startActivity(Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        })
        finish()
    }

    override fun onDestroy() {
        hCrono.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    /** Salir por atrás perdería el evento en curso sin aviso. */
    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        Toast.makeText(this, "Use FINALIZAR JORNADA para salir", Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val SIN_TIPO = "— Tipo de parada —"
        private const val PREFS = "super_tiempos_estado"
        private const val K_EVENTO          = "evento"
        private const val K_INICIO_MS       = "inicio_ms"
        private const val K_INICIO_HORA     = "inicio_hora"
        private const val K_LAT             = "lat"
        private const val K_LON             = "lon"
        private const val K_SECUENCIA       = "secuencia"
        private const val K_SESION_ID       = "sesion_id"
        private const val K_SESION_FECHA    = "sesion_fecha"
        private const val K_SESION_CORTADOR = "sesion_cortador"
    }
}