package com.palmadata.app.utils

import com.palmadata.app.R
import com.palmadata.app.censo_enfermedades.CensoEnfermedadesActivity
import com.palmadata.app.data.model.AppModule
import com.palmadata.app.polinizacion.PolinizacionActivity
import com.palmadata.app.tratamientos.TratamientosActivity
import com.palmadata.app.strategus.StrategusActivity
import com.palmadata.app.trampas.TrampasActivity
import com.palmadata.app.plagas.PlagasActivity
import com.palmadata.app.supercosecha.SuperCosechaActivity
import com.palmadata.app.maquinaria.MaquinariaActivity
import com.palmadata.app.mapa.MapaUmasActivity
import com.palmadata.app.super_cosecha_vagon.SuperCosechaVagonActivity
import com.palmadata.app.super_poli.SuperPoliActivity
import com.palmadata.app.estudio_tiempos.EstudioTiemposActivity
object ModuleRegistry {
    // formularioId según generarFormulariosMovil() del sistema web:
    // 1=CENSO ENFERMEDADES, 2=CENSO PLAGAS, 3=CENSO TRAMPAS, 5=POLINIZACION,
    // 6=CENSO STRATEGUS, 12=TRATAMIENTOS, 14=SUPERVISIÓN COSECHA,
    // 24=MAQUINARIA, 25=FERTILIZACION. Polen inicial/final es un diálogo → sin id.
    fun getModules(): List<AppModule> = listOf(
        AppModule(id = "censo_enfermedades", name = "Censo enfermedades", iconResId = R.drawable.censo_enfermedades, destinationClass = CensoEnfermedadesActivity::class.java, formularioId = 1),
        AppModule(id = "polinizacion",       name = "Polinización",       iconResId = R.drawable.polinizacion,       destinationClass = PolinizacionActivity::class.java,       formularioId = 5),
        AppModule(id = "tratamientos",       name = "Tratamientos",       iconResId = R.drawable.tratamientos,       destinationClass = TratamientosActivity::class.java,       formularioId = 12),
        AppModule(id = "polen_inicial_final",name = "Polen inicial final",iconResId = R.drawable.polen_inicial_final,destinationClass = null),
        AppModule(id = "sanstrategus",       name = "Sanstrategus",       iconResId = R.drawable.strategus,          destinationClass = StrategusActivity::class.java,          formularioId = 6),
        AppModule(id = "censo_trampas",      name = "Censo trampas",      iconResId = R.drawable.censo_trampas,      destinationClass = TrampasActivity::class.java,            formularioId = 3),
        AppModule(id = "muestreo_plagas",    name = "Muestreo plagas",    iconResId = R.drawable.muestreo_plagas,    destinationClass = PlagasActivity::class.java,             formularioId = 2),
        AppModule(id = "super_cosecha",      name = "Supervisión cosecha",iconResId = R.drawable.super_cosecha,      destinationClass = SuperCosechaActivity::class.java,       formularioId = 14),
        AppModule(id = "super_cosecha_vagon",name = "Supervisión cosecha vagón", iconResId = R.drawable.cosecha_vagon, destinationClass = SuperCosechaVagonActivity::class.java, formularioId = 15),
        AppModule(id = "maquinaria",         name = "Maquinaria",         iconResId = R.drawable.maquinaria,         destinationClass = MaquinariaActivity::class.java,         formularioId = 24),
        AppModule(id = "mapa_umas",          name = "Fertilización",      iconResId = R.drawable.fertilizacion,      destinationClass = MapaUmasActivity::class.java,           formularioId = 25),
        AppModule(id = "super_poli",         name = "Supervisión polinización", iconResId = R.drawable.super_poli, destinationClass = SuperPoliActivity::class.java, formularioId = 20),
        AppModule(id = "estudio_tiempos",    name = "Estudio de tiempos", iconResId = R.drawable.tiempos, destinationClass = EstudioTiemposActivity::class.java, formularioId = 30),
    )
}

