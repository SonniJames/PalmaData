package com.palmadata.app.tratamientos

data class TratamientoRegistro(
    val id: String,
    val sanEventoTratId: Int,
    val auxTrabajadorId: Int,
    val fecha: String,
    val hora: String,
    val catLoteId: Long,
    val catPalmaId: Double = 0.0,
    val catPlantacionId: Int = 0,
    val linea: Int,
    val palma: Int,
    val sanEnfermedadesId: Int,
    val sanEventoEnfId: Int,
    val observaciones: String,
    val latitud: Double,
    val longitud: Double,
    val cantidad: Double,
    val sanEnfLecturaId: Int = 0,
    val usuario: Int = 1,
    val equipo: String,
    val lote: String? = null,
    val loteAlias: String? = null
)