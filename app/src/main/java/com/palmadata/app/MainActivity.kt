package com.palmadata.app

import android.Manifest
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.view.WindowManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.palmadata.app.data.model.AppModule
import com.palmadata.app.data.model.Worker
import com.palmadata.app.databinding.ActivityMainBinding
import com.palmadata.app.databinding.DialogSelectWorkerBinding
import com.palmadata.app.databinding.DialogInformacionLocalBinding
import com.palmadata.app.databinding.DialogPolenInicialFinalBinding
import com.palmadata.app.polen.PolenInicialFinalRegistro
import com.palmadata.app.maquinaria.MaquinariaActivity
import com.palmadata.app.supercosecha.SuperCosechaActivity
import com.palmadata.app.service.TrackingService
import com.palmadata.app.utils.aDecimalCampo
import com.palmadata.app.ui.ModulesAdapter
import com.palmadata.app.ui.WorkerAdapter
import com.palmadata.app.utils.DatabaseHelper
import com.palmadata.app.utils.ExportManager
import com.palmadata.app.utils.LocationHelper
import com.palmadata.app.utils.ModuleRegistry
import com.palmadata.app.utils.SessionManager
import com.palmadata.app.utils.SyncManager
import com.palmadata.app.utils.TrackStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import androidx.core.view.WindowCompat

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var locationHelper: LocationHelper
    private lateinit var db: DatabaseHelper

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        SessionManager.markGpsPermissionRequested(this)
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                requestBackgroundLocationPermission()
            } else {
                iniciarTrackingService()
            }
        } else {
            Toast.makeText(this, getString(R.string.gps_permission_denied), Toast.LENGTH_LONG).show()
        }
    }

    private fun solicitarExcluirOptimizacionBateria() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = getSystemService(android.os.PowerManager::class.java)
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                val intent = Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = android.net.Uri.parse("package:$packageName")
                }
                startActivity(intent)
            }
        }
    }


    private fun setupLimpiarWorker() {
        actualizarEstadoLimpiarWorker()
        binding.tvLimpiarWorker.setOnClickListener {
            if (SessionManager.hasWorker(this)) {
                SessionManager.clearWorker(this)
                binding.tvWorkerSelector.text = getString(R.string.worker_not_selected)
                binding.tvWorkerSelector.setTextColor(ContextCompat.getColor(this, R.color.worker_not_set))
                actualizarEstadoLimpiarWorker()
                Toast.makeText(this, "Trabajador limpiado", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun actualizarEstadoLimpiarWorker() {
        val hayTrabajador = SessionManager.hasWorker(this)
        binding.tvLimpiarWorker.alpha = if (hayTrabajador) 1.0f else 0.4f
        binding.tvLimpiarWorker.isEnabled = hayTrabajador
    }
    private val backgroundLocationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Sin importar si lo concede o no, arrancamos el servicio.
        // Sin el permiso de background, Android puede limitar las actualizaciones
        // cuando la app esté minimizada, pero el servicio sigue activo en foreground.
        iniciarTrackingService()
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* concedido o no, la app continúa; sin él solo se pierde la notificación */ }

    /**
     * Permiso de almacenamiento para DESCARGAR, necesario SOLO en Android 8 y 9:
     * desde Android 10 se escribe en Descargas/PalmaData vía MediaStore sin
     * permiso alguno. Si lo concede, se continúa con la descarga.
     */
    private val storagePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedido ->
        if (concedido) confirmarDescarga()
        else Toast.makeText(this, "Sin permiso de almacenamiento no se pueden guardar los archivos.", Toast.LENGTH_LONG).show()
    }

    /**
     * Android 13+ exige pedir el permiso de notificaciones en runtime.
     * Sin él, NO se muestra la notificación del servicio ni la de fertilización
     * (la que avisa la UMA actual y su dosis en la pantalla de bloqueo).
     */
    private fun solicitarPermisoNotificaciones() {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Respetar barras del sistema
        WindowCompat.setDecorFitsSystemWindows(window, true)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = DatabaseHelper.getInstance(this)

        // Red de seguridad: si el proceso murió dentro del módulo de fertilización,
        // el onDestroy del mapa nunca corrió y los fertilizantes quedarían "pegados"
        // etiquetando tracks de otros días. Al arrancar la app siempre se limpian.
        SessionManager.clearFertilizantesActivos(this)

        setupLocationHelper()
        setupModulesGrid()
        setupWorkerSelector()
        setupLimpiarWorker()
        setupSincronizar()
        setupInformacionLocal()
        //SessionManager.clearWorker(this)
        val workerActual = SessionManager.getCurrentWorker(this)
        if (workerActual != null) {
            binding.tvWorkerSelector.text = "${getString(R.string.worker_selected_prefix)}${workerActual.name}"
            binding.tvWorkerSelector.setTextColor(ContextCompat.getColor(this, R.color.worker_set))
        }
        handleGpsPermissions()
        solicitarPermisoNotificaciones()
        solicitarExcluirOptimizacionBateria()
    }

    override fun onStart() {
        super.onStart()
        // No revivir el tracking si la jornada de hoy ya se cerró (el trabajador
        // sincronizó en la tarde). Antes, cualquier retorno a esta pantalla
        // reiniciaba el servicio y volvía a registrar tracks — ese era el bug.
        if (locationHelper.hasPermissions() && !SessionManager.isJornadaCerradaHoy(this)) {
            iniciarTrackingService()
        }
    }

    override fun onResume() {
        super.onResume()
        // El usuario está en la pantalla de módulos → sin módulo activo.
        // Al entrar a un módulo se marca su id y, como el trabajador queda
        // "encerrado" navegando dentro del módulo sin pasar por aquí, el id
        // se mantiene hasta que regrese a esta pantalla.
        SessionManager.clearFormularioActivo(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) {
            detenerTrackingService()
        }
    }

    private fun setupInformacionLocal() {
        binding.btnInformacionLocal.setOnClickListener {
            showInformacionLocal()
        }
    }

    // ── Tracking Service (background) ───────────────────────────────────────

    private fun iniciarTrackingService() {
        // Guarda central: nunca arrancar si la jornada de hoy ya se cerró.
        // Así los tres puntos de arranque (onCreate, onStart, permisos)
        // respetan el corte de la tarde sin repetir la comprobación.
        if (SessionManager.isJornadaCerradaHoy(this)) return
        val intent = Intent(this, TrackingService::class.java)
        intent.action = TrackingService.ACTION_START
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun detenerTrackingService() {
        stopService(Intent(this, TrackingService::class.java))
    }

    // ── GPS ──────────────────────────────────────────────────────────────────

    private fun setupLocationHelper() {
        locationHelper = LocationHelper(
            context = this,
            onLocationUpdate = { lat, lon ->
                binding.tvGpsCoords.visibility = View.VISIBLE
                binding.tvGpsCoords.text = String.format(
                    Locale.getDefault(), "📍 %.6f, %.6f", lat, lon
                )
                binding.tvGpsStatus.text = "✓"
            },
            onTrackGuardado = null
        )
    }

    // ── Grid de módulos ──────────────────────────────────────────────────────

    private fun setupModulesGrid() {
        val modules = ModuleRegistry.getModules()
        binding.rvModules.layoutManager = GridLayoutManager(this, 3)
        binding.rvModules.adapter = ModulesAdapter(modules) { module ->
            onModuleClicked(module)
        }
    }

    private fun onModuleClicked(module: AppModule) {

        if (module.id == "informacion_local") {
            showInformacionLocal()
            return
        }

        if (module.id == "polen_inicial_final") {
            if (!SessionManager.hasWorker(this)) {
                MaterialAlertDialogBuilder(this)
                    .setTitle("⚠️ ${getString(R.string.no_worker_warning)}")
                    .setMessage(getString(R.string.no_worker_message))
                    .setPositiveButton(getString(R.string.select_worker)) { dialog, _ -> dialog.dismiss(); showWorkerSelector() }
                    .setNegativeButton(getString(R.string.cancel)) { dialog, _ -> dialog.dismiss() }
                    .show()
                return
            }
            showPolenInicialFinal()
            return
        }

        if (module.id == "maquinaria") {
            SessionManager.setFormularioActivo(this, module.formularioId)  // 24
            startActivity(Intent(this, MaquinariaActivity::class.java))
            return
        }

        if (!SessionManager.hasWorker(this)) {
            MaterialAlertDialogBuilder(this)
                .setTitle("⚠️ ${getString(R.string.no_worker_warning)}")
                .setMessage(getString(R.string.no_worker_message))
                .setPositiveButton(getString(R.string.select_worker)) { dialog, _ -> dialog.dismiss(); showWorkerSelector() }
                .setNegativeButton(getString(R.string.cancel)) { dialog, _ -> dialog.dismiss() }
                .show()
            return
        }

        if (module.destinationClass == null) {
            Toast.makeText(this, "Módulo '${module.name}' próximamente disponible", Toast.LENGTH_SHORT).show()
            return
        }

        // Marcar el módulo activo: los tracks llevarán este id en `formulario`
        // hasta que el usuario regrese a esta pantalla (onResume lo pone en 0).
        if (module.formularioId > 0) {
            SessionManager.setFormularioActivo(this, module.formularioId)
        }
        startActivity(Intent(this, module.destinationClass))
    }

    // ── Selector de trabajador ────────────────────────────────────────────────

    private fun setupWorkerSelector() {
        binding.tvWorkerSelector.setOnClickListener { showWorkerSelector() }
        actualizarEstadoLimpiarWorker()
    }

    private fun showWorkerSelector() {
        val trabajadores = db.getTrabajadoresConSupervisor()

        val dialogBinding = DialogSelectWorkerBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(this, R.style.WorkerDialogTheme)
            .setView(dialogBinding.root)
            .create()

        if (trabajadores.isEmpty()) {
            dialogBinding.tvNoWorkers.text = "Sin datos. Toque SINCRONIZAR primero."
            dialogBinding.tvNoWorkers.visibility = View.VISIBLE
            dialogBinding.rvWorkers.visibility = View.GONE
        } else {
            val adapter = WorkerAdapter { nombreSeleccionado ->
                val worker = trabajadores.first { it.second == nombreSeleccionado }
                onWorkerSelected(Worker(
                    id         = worker.first.toString(),
                    name       = worker.second,
                    code       = worker.first.toString(),
                    supervisor = worker.third
                ))
                dialog.dismiss()
            }
            dialogBinding.rvWorkers.layoutManager = LinearLayoutManager(this)
            dialogBinding.rvWorkers.adapter = adapter
            adapter.submitList(trabajadores.map { it.second })

            dialogBinding.etSearchWorker.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    adapter.filter(s.toString())
                    dialogBinding.tvNoWorkers.visibility =
                        if (adapter.isEmpty()) View.VISIBLE else View.GONE
                }
            })
        }

        dialogBinding.btnCancelWorker.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun onWorkerSelected(worker: Worker) {
        SessionManager.setCurrentWorker(this, worker)
        binding.tvWorkerSelector.text = "${getString(R.string.worker_selected_prefix)}${worker.name}"
        binding.tvWorkerSelector.setTextColor(ContextCompat.getColor(this, R.color.worker_set))
        actualizarEstadoLimpiarWorker()
        if (!locationHelper.hasPermissions()) requestLocationPermissions()
        Toast.makeText(this, "Trabajador: ${worker.name}", Toast.LENGTH_SHORT).show()
    }


    // ── SINCRONIZAR ───────────────────────────────────────────────────────────

    private fun setupSincronizar() {
        binding.btnSincronizar.setOnClickListener { sincronizarDatos() }
        binding.btnDescargar.setOnClickListener { descargarDatos() }
    }

    private fun sincronizarDatos() {
        // Diálogo de carga — bloquea interacción mientras sincroniza
        val progressLayout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER
            setPadding(80, 60, 80, 60)
        }
        val progressBar = android.widget.ProgressBar(this)
        val tvSincronizando = android.widget.TextView(this).apply {
            text = "Sincronizando..."
            gravity = android.view.Gravity.CENTER
            setPadding(0, 24, 0, 0)
            textSize = 15f
        }
        progressLayout.addView(progressBar)
        progressLayout.addView(tvSincronizando)

        val dialogCargando = MaterialAlertDialogBuilder(this)
            .setView(progressLayout)
            .setCancelable(false)
            .create()
        dialogCargando.show()

        // Mantener la pantalla encendida mientras sincroniza: evita que el
        // sistema apague la pantalla y limite el proceso a mitad de una subida
        // grande de tracks. Se libera sí o sí al terminar (éxito o error).
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        binding.btnSincronizar.isEnabled = false
        binding.btnSincronizar.text = "Sincronizando..."

        val hora = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        if (hora >= 12) {
            // Marcar la jornada como cerrada (estado que dura el resto del día):
            // ni onStart ni ningún otro punto reviven el servicio hasta mañana.
            SessionManager.cerrarJornadaHoy(this)
            detenerTrackingService()
            Toast.makeText(this, "Fin de jornada: el registro de recorrido se detiene y reinicia mañana al abrir la app.", Toast.LENGTH_LONG).show()
        }

        lifecycleScope.launch {
            val resultado = try {
                withContext(Dispatchers.IO) {
                    SyncManager.sincronizar(this@MainActivity)
                }
            } finally {
                // Pase lo que pase (éxito, error o excepción), soltar la pantalla
                // encendida y reactivar el botón — nunca dejar la UI bloqueada.
                dialogCargando.dismiss()
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                binding.btnSincronizar.isEnabled = true
                binding.btnSincronizar.text = "SINCRONIZAR"
            }

            if (resultado.exitoso) {
                val detalle = resultado.detalles.entries.joinToString("\n") {
                    "• ${it.key}: ${it.value}"
                }
                MaterialAlertDialogBuilder(this@MainActivity)
                    .setTitle("✅ Sincronización exitosa")
                    .setMessage(detalle)
                    .setPositiveButton("Aceptar") { d, _ -> d.dismiss() }
                    .show()
            } else {
                val detalle = if (resultado.detalles.isNotEmpty()) {
                    "\n\n" + resultado.detalles.entries.joinToString("\n") { "• ${it.key}: ${it.value}" }
                } else ""
                MaterialAlertDialogBuilder(this@MainActivity)
                    .setTitle("⚠️ Sincronización parcial")
                    .setMessage(resultado.mensaje + detalle)
                    .setPositiveButton("Aceptar") { d, _ -> d.dismiss() }
                    .show()
            }
        }
    }

    // ── DESCARGAR (exportar a Excel, para plantaciones sin red) ──────────────

    /**
     * Punto de entrada del botón. En Android 8/9 pide primero el permiso de
     * almacenamiento; en Android 10+ va directo a la confirmación.
     */
    private fun descargarDatos() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            return
        }
        confirmarDescarga()
    }

    private fun confirmarDescarga() {
        val hora = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        // Misma regla que SINCRONIZAR: después de mediodía la jornada se cierra.
        // Solo se avisa cuando aplica, para no asustar en una descarga de la
        // mañana que no detiene nada.
        val avisoJornada = if (hora >= 12)
            "\n\nAl descargar se finaliza la jornada de hoy: el registro de recorrido " +
                    "se detiene y se reanuda mañana a las 6:00 a.m."
        else ""

        MaterialAlertDialogBuilder(this)
            .setTitle("Generar archivos del día")
            .setMessage(
                "¿Seguro desea generar los archivos del día?\n\n" +
                        "Se creará un archivo Excel por cada módulo con registros, más el de tracks, " +
                        "en Descargas/${ExportManager.CARPETA}. Los registros exportados se eliminan del " +
                        "teléfono, así que ya no se podrán sincronizar." + avisoJornada
            )
            .setPositiveButton("Sí, generar") { d, _ -> d.dismiss(); ejecutarDescarga() }
            .setNegativeButton("Cancelar") { d, _ -> d.dismiss() }
            .show()
    }

    private fun ejecutarDescarga() {
        // Diálogo de carga — bloquea la interacción, igual que en sincronizar
        val progressLayout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER
            setPadding(80, 60, 80, 60)
        }
        val progressBar = android.widget.ProgressBar(this)
        val tvDescargando = android.widget.TextView(this).apply {
            text = "Descargando..."
            gravity = android.view.Gravity.CENTER
            setPadding(0, 24, 0, 0)
            textSize = 15f
        }
        progressLayout.addView(progressBar)
        progressLayout.addView(tvDescargando)

        val dialogCargando = MaterialAlertDialogBuilder(this)
            .setView(progressLayout)
            .setCancelable(false)
            .create()
        dialogCargando.show()

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        binding.btnDescargar.isEnabled = false
        binding.btnSincronizar.isEnabled = false
        binding.btnDescargar.text = "Descargando..."

        val hora = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        if (hora >= 12) {
            // Idéntico a sincronizar: jornada cerrada el resto del día, el
            // servicio no se revive hasta mañana desde ningún punto de arranque.
            SessionManager.cerrarJornadaHoy(this)
            detenerTrackingService()
            Toast.makeText(this, "Fin de jornada: el registro de recorrido se detiene y reinicia mañana al abrir la app.", Toast.LENGTH_LONG).show()
        }

        lifecycleScope.launch {
            val resultado = try {
                withContext(Dispatchers.IO) {
                    ExportManager.exportar(this@MainActivity)
                }
            } catch (e: Exception) {
                ExportManager.ResultadoExport(exitoso = false, mensaje = "Error inesperado: ${e.message}")
            } finally {
                dialogCargando.dismiss()
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                binding.btnDescargar.isEnabled = true
                binding.btnSincronizar.isEnabled = true
                binding.btnDescargar.text = "DESCARGAR"
            }

            val conteos = resultado.detalles.entries.joinToString("\n") { "• ${it.key}: ${it.value}" }
            val lista = if (resultado.archivos.isNotEmpty())
                "\n\nArchivos (descarga #${resultado.secuencia} de hoy):\n" +
                        resultado.archivos.joinToString("\n") { "• $it" }
            else ""

            MaterialAlertDialogBuilder(this@MainActivity)
                .setTitle(if (resultado.exitoso) "✅ Descarga completa" else "⚠️ Descarga parcial")
                .setMessage(resultado.mensaje + (if (conteos.isNotEmpty()) "\n\n$conteos" else "") + lista)
                .setPositiveButton("Aceptar") { d, _ -> d.dismiss() }
                .show()
        }
    }

    // ── Polen inicial/final ───────────────────────────────────────────────────

    private fun showPolenInicialFinal() {
        val dialogBinding = DialogPolenInicialFinalBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(this, R.style.WorkerDialogTheme)
            .setView(dialogBinding.root)
            .create()

        // Fecha por defecto: hoy
        val fmtFecha = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val fmtMostrar = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        var fechaSeleccionada = fmtFecha.format(Date())
        dialogBinding.btnFecha.text = fmtMostrar.format(Date())

        // Selector de fecha
        dialogBinding.btnFecha.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(this,
                { _, year, month, day ->
                    cal.set(year, month, day)
                    fechaSeleccionada = fmtFecha.format(cal.time)
                    dialogBinding.btnFecha.text = fmtMostrar.format(cal.time)
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        dialogBinding.btnCancelar.setOnClickListener { dialog.dismiss() }

        dialogBinding.btnGuardar.setOnClickListener {
            val worker = SessionManager.getCurrentWorker(this)
            if (worker == null) {
                Toast.makeText(this, "Error: no hay trabajador en sesión", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val inicialStr = dialogBinding.etPolenInicial.text.toString().trim()
            val finalStr   = dialogBinding.etPolenFinal.text.toString().trim()

            if (inicialStr.isEmpty() || finalStr.isEmpty()) {
                dialog.dismiss()
                return@setOnClickListener
            }

            val registro = PolenInicialFinalRegistro(
                fecha      = fechaSeleccionada,
                inicial    = inicialStr.aDecimalCampo() ?: 0.0,
                final      = finalStr.aDecimalCampo() ?: 0.0,
                trabajador = worker.code.toIntOrNull() ?: 0
            )

            try {
                db.guardarPolen(registro)
                Toast.makeText(this, "✅ Registro guardado", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            } catch (e: Exception) {
                Toast.makeText(this, "❌ Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }

        dialog.show()
    }

    // ── Información local ─────────────────────────────────────────────────────

    private fun showInformacionLocal() {
        val dialogBinding = DialogInformacionLocalBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(this, R.style.WorkerDialogTheme)
            .setView(dialogBinding.root)
            .create()

        dialogBinding.tvUltimaSincronizacion.text = SyncManager.getUltimaSincronizacion(this)
        dialogBinding.tvTracks.text               = TrackStorage.contarTracks(this).toString()
        dialogBinding.tvCensoEnf.text             = db.contarCensoEnfPendientes().toString()
        dialogBinding.tvPolinizacion.text         = db.contarPolinizacionPendientes().toString()
        dialogBinding.tvTratamientos.text         = db.contarTratamientosPendientes().toString()
        dialogBinding.tvPolen.text                = db.contarPolenPendientes().toString()
        dialogBinding.tvStrategus.text            = db.contarStrateguspendientes().toString()
        dialogBinding.tvTrampas.text              = db.contarTrampasPendientes().toString()
        dialogBinding.tvPlagas.text               = db.contarPlagasPendientes().toString()
        dialogBinding.tvSuperCosecha.text         = db.contarSuperCosechaPendientes().toString()
        dialogBinding.tvMaquinaria.text           = db.contarMaquinariaPendientes().toString()
        dialogBinding.tvSuperCosechaVagon.text    = db.contarSuperCosechaVagonPendientes().toString()
        dialogBinding.tvSuperPoli.text            = db.contarSuperPoliPendientes().toString()
        dialogBinding.tvSuperTiempos.text         = db.contarSuperTiemposPendientes().toString()

        dialogBinding.btnCerrarInfo.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    // ── Permisos GPS ──────────────────────────────────────────────────────────

    private fun handleGpsPermissions() {
        when {
            locationHelper.hasPermissions() -> {
                iniciarTrackingService()
            }
            !SessionManager.wasGpsPermissionRequested(this) -> {
                MaterialAlertDialogBuilder(this)
                    .setTitle(getString(R.string.gps_permission_title))
                    .setMessage(getString(R.string.gps_permission_message))
                    .setPositiveButton("Continuar") { _, _ -> requestLocationPermissions() }
                    .setNegativeButton("Ahora no") { dialog, _ ->
                        SessionManager.markGpsPermissionRequested(this)
                        dialog.dismiss()
                    }
                    .setCancelable(false)
                    .show()
            }
        }
    }

    private fun requestLocationPermissions() {
        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    private fun requestBackgroundLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            backgroundLocationPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }
    }
}