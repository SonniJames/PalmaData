package com.palmadata.app

import android.Manifest
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
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
import com.palmadata.app.ui.ModulesAdapter
import com.palmadata.app.ui.WorkerAdapter
import com.palmadata.app.utils.DatabaseHelper
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
            if (SessionManager.hasWorker(this)) locationHelper.startLocationUpdates()
        } else {
            Toast.makeText(this, getString(R.string.gps_permission_denied), Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = DatabaseHelper(this)

        setupLocationHelper()
        setupModulesGrid()
        setupWorkerSelector()
        setupSincronizar()
        setupInformacionLocal()
        SessionManager.clearWorker(this)
        locationHelper.stopLocationUpdates()
        handleGpsPermissions()
    }

    override fun onResume() {
        super.onResume()
        if (SessionManager.hasWorker(this) && locationHelper.hasPermissions()) {
            locationHelper.startLocationUpdates()
        }
    }

    private fun setupInformacionLocal() {
        binding.btnInformacionLocal.setOnClickListener {
            showInformacionLocal()
        }
    }

    override fun onStop() {
        super.onStop()
        locationHelper.stopLocationUpdates()
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
        if (locationHelper.hasPermissions()) locationHelper.startLocationUpdates()
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

        startActivity(Intent(this, module.destinationClass))
    }

    // ── Selector de trabajador ────────────────────────────────────────────────

    private fun setupWorkerSelector() {
        binding.tvWorkerSelector.setOnClickListener { showWorkerSelector() }
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
        if (locationHelper.hasPermissions()) locationHelper.startLocationUpdates()
        else requestLocationPermissions()
        Toast.makeText(this, "Trabajador: ${worker.name}", Toast.LENGTH_SHORT).show()
    }


    // ── SINCRONIZAR ───────────────────────────────────────────────────────────

    private fun setupSincronizar() {
        binding.btnSincronizar.setOnClickListener { sincronizarDatos() }
    }

    private fun sincronizarDatos() {
        binding.btnSincronizar.isEnabled = false
        binding.btnSincronizar.text = "Sincronizando..."

        lifecycleScope.launch {
            val resultado = withContext(Dispatchers.IO) {
                SyncManager.sincronizar(this@MainActivity)
            }

            binding.btnSincronizar.isEnabled = true
            binding.btnSincronizar.text = "SINCRONIZAR"

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
                MaterialAlertDialogBuilder(this@MainActivity)
                    .setTitle("❌ Error de sincronización")
                    .setMessage(resultado.mensaje)
                    .setPositiveButton("Aceptar") { d, _ -> d.dismiss() }
                    .show()
            }
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
                inicial    = inicialStr.toDoubleOrNull() ?: 0.0,
                final      = finalStr.toDoubleOrNull() ?: 0.0,
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

        dialogBinding.btnCerrarInfo.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    // ── Permisos GPS ──────────────────────────────────────────────────────────

    private fun handleGpsPermissions() {
        when {
            locationHelper.hasPermissions() -> {
                if (SessionManager.hasWorker(this)) locationHelper.startLocationUpdates()
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
}