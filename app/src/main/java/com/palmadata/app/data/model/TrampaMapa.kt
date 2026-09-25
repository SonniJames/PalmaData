package com.palmadata.app.data.model

/** Trampa de plantacion.santrampa dentro del polígono de la plantación (vista trampas_mapa). */
data class TrampaMapa(
    val santrampaId: Int,
    val codigo: String,
    val lat: Double,
    val lon: Double
)
