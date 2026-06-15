package com.palmadata.app.maquinaria

data class MaquinariaRegistro(
    val idUnico: String,
    val maquina: Int,
    val plantacion: Int,
    val implemento: Int,
    val labor: Int,
    val trabajador: Int,
    val kiloinicial: Double,
    val kilofinal: Double,
    val combustible: Double,
    val horometroinicial: Double,
    val horometrofinal: Double,
    val lote: String,
    val observaciones: String,
    val unidadcantidad: Int,
    val cantidad: Double,
    val fechainicial: String,
    val horainicial: String,
    val fechafinal: String,
    val horafinal: String,
    val equipo: String
)