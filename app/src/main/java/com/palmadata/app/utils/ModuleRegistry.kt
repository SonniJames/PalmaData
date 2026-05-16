package com.palmadata.app.utils

import com.palmadata.app.R
import com.palmadata.app.censo_enfermedades.CensoEnfermedadesActivity
import com.palmadata.app.data.model.AppModule

object ModuleRegistry {

    fun getModules(): List<AppModule> = listOf(

        AppModule(
            id               = "censo_enfermedades",
            name             = "Censo enfermedades",
            iconResId        = R.drawable.censo_enfermedades,
            destinationClass = CensoEnfermedadesActivity::class.java
        ),

        AppModule(
            id               = "polinizacion",
            name             = "Polinización",
            iconResId        = R.drawable.polinizacion,
            destinationClass = null
        ),

        AppModule(
            id               = "tratamientos",
            name             = "Tratamientos",
            iconResId        = R.drawable.tratamientos,
            destinationClass = null
        ),

        AppModule(
            id               = "informacion_local",
            name             = "Información local",
            iconResId        = R.drawable.informacion_local,
            destinationClass = null   // Este módulo abre un diálogo, no una Activity
        )
    )
}