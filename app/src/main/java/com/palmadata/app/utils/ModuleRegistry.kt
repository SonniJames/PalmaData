package com.palmadata.app.utils

import com.palmadata.app.R
import com.palmadata.app.censo_enfermedades.CensoEnfermedadesActivity
import com.palmadata.app.data.model.AppModule
import com.palmadata.app.polinizacion.PolinizacionActivity
import com.palmadata.app.tratamientos.TratamientosActivity
import com.palmadata.app.strategus.StrategusActivity

object ModuleRegistry {
    fun getModules(): List<AppModule> = listOf(
        AppModule(id = "censo_enfermedades", name = "Censo enfermedades", iconResId = R.drawable.censo_enfermedades, destinationClass = CensoEnfermedadesActivity::class.java),
        AppModule(id = "polinizacion",       name = "Polinización",       iconResId = R.drawable.polinizacion,       destinationClass = PolinizacionActivity::class.java),
        AppModule(id = "tratamientos",       name = "Tratamientos",       iconResId = R.drawable.tratamientos,       destinationClass = TratamientosActivity::class.java),
        AppModule(id = "informacion_local",  name = "Información local",  iconResId = R.drawable.informacion_local,  destinationClass = null),
        AppModule(id = "polen_inicial_final",name = "Polen inicial final",iconResId = R.drawable.polen_inicial_final,destinationClass = null),
        AppModule(id = "sanstrategus", name = "Sanstrategus", iconResId = R.drawable.strategus, destinationClass = StrategusActivity::class.java)
    )
}


