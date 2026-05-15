package com.palmadata.app

import android.Manifest
import android.app.AlertDialog
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
import com.palmadata.app.data.model.Plantacion
import com.palmadata.app.data.model.Worker
import com.palmadata.app.databinding.ActivityMainBinding
import com.palmadata.app.databinding.DialogSelectPlantacionBinding
import com.palmadata.app.databinding.DialogSelectWorkerBinding
import com.palmadata.app.ui.ModulesAdapter
import com.palmadata.app.ui.WorkerAdapter
import com.palmadata.app.utils.LocationHelper
import com.palmadata.app.utils.ModuleRegistry
import com.palmadata.app.utils.SessionManager
import java.util.Locale
import android.content.Intent

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var locationHelper: LocationHelper

    private val todasLasPlantaciones = listOf(
        Plantacion(id = "1", name = "CUCU",              code = "CUCU"),
        Plantacion(id = "2", name = "Villa Claudia",      code = "VC"),
        Plantacion(id = "3", name = "Palmeras de Yarima", code = "PDY")
    )

    private val todosLosTrabajadores = listOf(
        Worker(id = "101", name = "Carlos Martínez",      code = "1"),
        Worker(id = "102", name = "Ana Lucía Gómez",      code = "2"),
        Worker(id = "103", name = "Pedro Hernández",      code = "3"),
        Worker(id = "201", name = "María Salazar",        code = "4"),
        Worker(id = "202", name = "Juan Pablo Roa",       code = "5"),
        Worker(id = "301", name = "Lucía Fernández",      code = "6"),
        Worker(id = "302", name = "Andrés Felipe Torres", code = "7")
    )

    private val trabajadoresPorPlantacion = mapOf(
        "1" to listOf("101", "102", "103"),
        "2" to listOf("201", "202"),
        "3" to listOf("301", "302")
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
        setupPlantacionSelector()
        setupWorkerSelector()
        SessionManager.clearWorker(this)
        SessionManager.clearPlantacion(this)
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

    private fun setupLocationHelper() {
        locationHelper = LocationHelper(this) { lat, lon ->
            binding.tvGpsCoords.visibility = View.VISIBLE
            binding.tvGpsCoords.text = String.format(
                Locale.getDefault(), "📍 %.6f, %.6f", lat, lon
            )
            binding.tvGpsStatus.text = "✓"
        }
    }

    private fun setupModulesGrid() {
        val modules = ModuleRegistry.getModules()
        binding.rvModules.layoutManager = GridLayoutManager(this, 3)
        binding.rvModules.adapter = ModulesAdapter(modules) { module ->
            onModuleClicked(module)
        }
    }

    private fun onModuleClicked(module: AppModule) {
        if (!SessionManager.hasPlantacion(this)) {
            MaterialAlertDialogBuilder(this)
                .setTitle("⚠️ Seleccione una plantación")
                .setMessage("Debe seleccionar una plantación antes de acceder a este módulo.")
                .setPositiveButton("Seleccionar") { dialog, _ -> dialog.dismiss(); showPlantacionSelector() }
                .setNegativeButton("Cancelar") { dialog, _ -> dialog.dismiss() }
                .show()
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
        val intent = Intent(this, module.destinationClass)
        startActivity(intent)
    }

    // ── Plantación ───────────────────────────────────────────────────────────

    private fun setupPlantacionSelector() {
        binding.tvPlantacionSelector.setOnClickListener { showPlantacionSelector() }
    }

    private fun showPlantacionSelector() {
        val dialogBinding = DialogSelectPlantacionBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(this, R.style.WorkerDialogTheme)
            .setView(dialogBinding.root)
            .create()

        val adapter = WorkerAdapter { nombreSeleccionado ->
            val plantacion = todasLasPlantaciones.first { it.name == nombreSeleccionado }
            onPlantacionSelected(plantacion)
            dialog.dismiss()
        }

        dialogBinding.rvPlantaciones.layoutManager = LinearLayoutManager(this)
        dialogBinding.rvPlantaciones.adapter = adapter
        adapter.submitList(todasLasPlantaciones.map { it.name })

        if (todasLasPlantaciones.isEmpty()) {
            dialogBinding.tvNoPlantaciones.visibility = View.VISIBLE
            dialogBinding.rvPlantaciones.visibility = View.GONE
        }

        dialogBinding.btnCancelPlantacion.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun onPlantacionSelected(plantacion: Plantacion) {
        val plantacionAnterior = SessionManager.getCurrentPlantacion(this)
        if (plantacionAnterior?.id != plantacion.id) {
            SessionManager.clearWorker(this)
            binding.tvWorkerSelector.text = getString(R.string.worker_not_selected)
            binding.tvWorkerSelector.setTextColor(ContextCompat.getColor(this, R.color.worker_not_set))
            binding.tvGpsCoords.visibility = View.GONE
            locationHelper.stopLocationUpdates()
        }
        SessionManager.setCurrentPlantacion(this, plantacion)
        binding.tvPlantacionSelector.text = "Plantación: ${plantacion.name}"
        binding.tvPlantacionSelector.setTextColor(ContextCompat.getColor(this, R.color.worker_set))
        Toast.makeText(this, "Plantación: ${plantacion.name}", Toast.LENGTH_SHORT).show()
    }

    // ── Trabajador ───────────────────────────────────────────────────────────

    private fun setupWorkerSelector() {
        binding.tvWorkerSelector.setOnClickListener {
            if (!SessionManager.hasPlantacion(this)) {
                Toast.makeText(this, "Seleccione una plantación primero", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            showWorkerSelector()
        }
    }

    private fun showWorkerSelector() {
        val plantacionActual = SessionManager.getCurrentPlantacion(this) ?: return
        val idsPlantacion = trabajadoresPorPlantacion[plantacionActual.id] ?: emptyList()
        val trabajadoresFiltrados = todosLosTrabajadores.filter { it.id in idsPlantacion }

        val dialogBinding = DialogSelectWorkerBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(this, R.style.WorkerDialogTheme)
            .setView(dialogBinding.root)
            .create()

        val adapter = WorkerAdapter { nombreSeleccionado ->
            val worker = trabajadoresFiltrados.first { it.name == nombreSeleccionado }
            onWorkerSelected(worker)
            dialog.dismiss()
        }

        dialogBinding.rvWorkers.layoutManager = LinearLayoutManager(this)
        dialogBinding.rvWorkers.adapter = adapter
        adapter.submitList(trabajadoresFiltrados.map { it.name })

        if (trabajadoresFiltrados.isEmpty()) {
            dialogBinding.tvNoWorkers.visibility = View.VISIBLE
            dialogBinding.rvWorkers.visibility = View.GONE
        }

        // Buscador
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

    // ── Sesión ───────────────────────────────────────────────────────────────

    private fun restoreSession() {
        val plantacion = SessionManager.getCurrentPlantacion(this)
        if (plantacion != null) {
            binding.tvPlantacionSelector.text = "Plantación: ${plantacion.name}"
            binding.tvPlantacionSelector.setTextColor(ContextCompat.getColor(this, R.color.worker_set))
        }
        val worker = SessionManager.getCurrentWorker(this)
        if (worker != null) {
            binding.tvWorkerSelector.text = "${getString(R.string.worker_selected_prefix)}${worker.name}"
            binding.tvWorkerSelector.setTextColor(ContextCompat.getColor(this, R.color.worker_set))
            val lat = SessionManager.getLastLatitude(this)
            val lon = SessionManager.getLastLongitude(this)
            if (lat != 0.0 || lon != 0.0) {
                binding.tvGpsCoords.visibility = View.VISIBLE
                binding.tvGpsCoords.text = String.format(Locale.getDefault(), "📍 %.6f, %.6f", lat, lon)
            }
        }
    }

    // ── GPS ──────────────────────────────────────────────────────────────────

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