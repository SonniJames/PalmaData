package com.palmadata.app.utils

import com.palmadata.app.R
import com.palmadata.app.censo_enfermedades.CensoEnfermedadesActivity
import com.palmadata.app.data.model.AppModule
import com.palmadata.app.polinizacion.PolinizacionActivity
import com.palmadata.app.tratamientos.TratamientosActivity
import com.palmadata.app.strategus.StrategusActivity
import com.palmadata.app.trampas.TrampasActivity
import com.palmadata.app.plagas.PlagasActivity

object ModuleRegistry {
    fun getModules(): List<AppModule> = listOf(
        AppModule(id = "censo_enfermedades", name = "Censo enfermedades", iconResId = R.drawable.censo_enfermedades, destinationClass = CensoEnfermedadesActivity::class.java),
        AppModule(id = "polinizacion",       name = "Polinización",       iconResId = R.drawable.polinizacion,       destinationClass = PolinizacionActivity::class.java),
        AppModule(id = "tratamientos",       name = "Tratamientos",       iconResId = R.drawable.tratamientos,       destinationClass = TratamientosActivity::class.java),
        AppModule(id = "polen_inicial_final",name = "Polen inicial final",iconResId = R.drawable.polen_inicial_final,destinationClass = null),
        AppModule(id = "sanstrategus",       name = "Sanstrategus",       iconResId = R.drawable.strategus,          destinationClass = StrategusActivity::class.java),
        AppModule(id = "censo_trampas",      name = "Censo trampas",      iconResId = R.drawable.censo_trampas,      destinationClass = TrampasActivity::class.java),
        AppModule(id = "muestreo_plagas",    name = "Muestreo plagas",    iconResId = R.drawable.muestreo_plagas,    destinationClass = PlagasActivity::class.java),
        AppModule(id = "super_cosecha",      name = "Supervisión cosecha",iconResId = R.drawable.super_cosecha,      destinationClass = null),
    )
}



