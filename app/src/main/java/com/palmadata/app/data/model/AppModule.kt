package com.palmadata.app.data.model

data class AppModule(
    val id: String,
    val name: String,
    val iconResId: Int?,
    val isEnabled: Boolean = true,
    val destinationClass: Class<*>? = null,
    // Id de formulario para la columna `formulario` de tracksmoviltemp
    // (según generarFormulariosMovil del sistema web). 0 = sin módulo activo.
    val formularioId: Int = 0
)