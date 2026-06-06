package com.palmadata.app.strategus

data class StrategusRegistro(
    val id: String,
    val fecha: String,
    val hora: String,
    val catLoteId: Long,
    val linea: Int,
    val palma: Int,
    val catPalmaId: Long = 0L,
    val galerias: Int,
    val censo: Long,
    val evaluador: Int,
    val catPlantacionId: Int,
    val observaciones: String,
    val latitud: Double,
    val longitud: Double,
    val equipo: String,
    val lote: String? = null,
    val loteAlias: String? = null
)