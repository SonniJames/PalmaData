package com.palmadata.app.plagas

data class PlagasRegistro(
    val id: String,
    val fecha: String,
    val hora: String,
    val lectura: Int,
    val linea: Int,
    val palma: Int,
    val catLoteId: Long,
    val catPalmaId: Long = 0L,
    val catPlantacionId: Int,
    val evaluador: Int,
    val insectoId: Int,
    val estadoInsectoId: Int,
    val cantidad: Int,
    val nivFoliar: Int,
    val defol5: Double,
    val defol13: Double,
    val defol21: Double,
    val defol29: Double,
    val defol37: Double,
    val observaciones: String,
    val latitud: Double,
    val longitud: Double,
    val equipo: String
)