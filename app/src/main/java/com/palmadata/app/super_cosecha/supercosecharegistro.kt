package com.palmadata.app.supercosecha

data class SuperCosechaRegistro(
    val idUnico: String,
    val fecha: String,
    val hora: String,
    val supervisor: Int,
    // Listas de ids de trabajadores separadas por coma:
    //   ""            = ninguno seleccionado
    //   "112"         = un trabajador
    //   "125,159,520" = varios
    val cortador: String,
    val recolector: String,
    val alistador: String,
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