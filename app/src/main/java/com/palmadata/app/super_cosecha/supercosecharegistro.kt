package com.palmadata.app.supercosecha

data class SuperCosechaRegistro(
    val idUnico: String,
    val fecha: String,
    val hora: String,
    val supervisor: Int,
    val cortador: Int,
    val recolector: Int,
    val linea: Int,
    val palma: Int,
    val ciclo: Int,
    val catLoteId: Long,
    val catPlantacionId: Int,
    val racimosRecogidos: Int,
    val racimosVerdes: Int,
    val racimossobremaduros: Int,
    val racimosPodridos: Int,
    val racimossinrecoger: Int,
    val racimossincortar: Int,
    val racimorobado: Int,
    val hojasmalacomo: Int,
    val hojacolgando: Int,
    val frutoplato: Int,
    val observaciones: String,
    val latitud: Double,
    val longitud: Double,
    val equipo: String
)