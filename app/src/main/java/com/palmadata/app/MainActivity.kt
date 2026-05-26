package com.palmadata.app

import android.Manifest
import android.app.AlertDialog
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
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.palmadata.app.data.model.AppModule
import com.palmadata.app.data.model.Worker
import com.palmadata.app.databinding.ActivityMainBinding
import com.palmadata.app.databinding.DialogSelectWorkerBinding
import com.palmadata.app.databinding.DialogInformacionLocalBinding
import com.palmadata.app.ui.ModulesAdapter
import com.palmadata.app.ui.WorkerAdapter
import com.palmadata.app.utils.LocationHelper
import com.palmadata.app.utils.ModuleRegistry
import com.palmadata.app.utils.SessionManager
import com.palmadata.app.utils.TrackStorage
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var locationHelper: LocationHelper

    // ── Trabajadores demo ────────────────────────────────────────────────────
    private val todosLosTrabajadores = listOf(
        Worker(id = "101", name = "Carlos Martínez",       code = "1"),
        Worker(id = "102", name = "Ana Lucía Gómez",       code = "2"),
        Worker(id = "103", name = "Pedro Hernández",       code = "3"),
        Worker(id = "201", name = "María Salazar",         code = "4"),
        Worker(id = "202", name = "Juan Pablo Roa",        code = "5"),
        Worker(id = "301", name = "Lucía Fernández",       code = "6"),
        Worker(id = "302", name = "Andrés Felipe Torres",  code = "7")
    )

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

        setupLocationHelper()
        setupModulesGrid()
        setupWorkerSelector()
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

        if (locationHelper.hasPermissions()) {
            locationHelper.startLocationUpdates()
        }
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
        // Módulo especial: no requiere trabajador
        if (module.id == "informacion_local") {
            showInformacionLocal()
            return
        }

        if (!SessionManager.hasWorker(this)) {
            MaterialAlertDialogBuilder(this)
                .setTitle("⚠️ ${getString(R.string.no_worker_warning)}")
                .setMessage(getString(R.string.no_worker_message))
                .setPositiveButton(getString(R.string.select_worker)) { dialog, _ ->
                    dialog.dismiss()
                    showWorkerSelector()
                }
                .setNegativeButton(getString(R.string.cancel)) { dialog, _ -> dialog.dismiss() }
                .show()
            return
        }

        if (module.destinationClass == null) {
            Toast.makeText(this, "Módulo '${module.name}' próximamente disponible", Toast.LENGTH_SHORT).show()
            return
        }

        val intent = Intent(this, module.destinationClass)
        startActivity(intent)
    }

    // ── Selector de trabajador ────────────────────────────────────────────────

    private fun setupWorkerSelector() {
        binding.tvWorkerSelector.setOnClickListener { showWorkerSelector() }
    }

    private fun showWorkerSelector() {
        val dialogBinding = DialogSelectWorkerBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(this, R.style.WorkerDialogTheme)
            .setView(dialogBinding.root)
            .create()

        val adapter = WorkerAdapter { nombreSeleccionado ->
            val worker = todosLosTrabajadores.first { it.name == nombreSeleccionado }
            onWorkerSelected(worker)
            dialog.dismiss()
        }

        dialogBinding.rvWorkers.layoutManager = LinearLayoutManager(this)
        dialogBinding.rvWorkers.adapter = adapter
        adapter.submitList(todosLosTrabajadores.map { it.name })

        dialogBinding.etSearchWorker.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                adapter.filter(s.toString())
                dialogBinding.tvNoWorkers.visibility =
                    if (adapter.isEmpty()) View.VISIBLE else View.GONE
            }
        })

        dialogBinding.btnCancelWorker.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun onWorkerSelected(worker: Worker) {
        SessionManager.setCurrentWorker(this, worker)
        binding.tvWorkerSelector.text = "${getString(R.string.worker_selected_prefix)}${worker.name}"
        binding.tvWorkerSelector.setTextColor(ContextCompat.getColor(this, R.color.worker_set))

        if (locationHelper.hasPermissions()) {
            locationHelper.startLocationUpdates()
        } else {
            requestLocationPermissions()
        }
        Toast.makeText(this, "Trabajador: ${worker.name}", Toast.LENGTH_SHORT).show()
    }

    // ── Información local ─────────────────────────────────────────────────────

    private fun showInformacionLocal() {
        val dialogBinding = DialogInformacionLocalBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(this, R.style.WorkerDialogTheme)
            .setView(dialogBinding.root)
            .create()

        dialogBinding.tvUltimaSincronizacion.text = "Sin sincronización aún"
        dialogBinding.tvTracks.text = TrackStorage.contarTracks(this).toString()
        dialogBinding.tvCensoEnf.text = "0"
        dialogBinding.tvPolinizacion.text = "0"
        dialogBinding.tvTratamientos.text = "0"

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