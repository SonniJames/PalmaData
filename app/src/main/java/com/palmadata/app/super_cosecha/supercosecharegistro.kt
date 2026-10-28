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
    val linea: Int?,                // null = no se digitó (opcional en este módulo)
    val palma: Int?,                // null = no se digitó (opcional en este módulo)
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
    val hojasmalacomo: Int,         // "H. mal acomo." (pantalla 7)
    val hojacolgando: Int,
    val frutoplato: Int,
    val racimomalacomo: Int,        // "R. mal acomo." (pantalla 7)
    val pendunculolargo: Int,       // "Pendun. largo" (pantalla 7)
    val porcentajeBasura: Double?,  // pantalla 7.1 — null = en blanco
    val platosEnmalezados: Int,     // pantalla 7.2 — 1/0
    val malezasEstipe: Int,         // pantalla 7.2 — 1/0
    val loteInundado: Int,          // pantalla 7.2 — 1/0
    val podaAtrasada: Int,          // pantalla 7.2 — 1/0
    val observaciones: String,
    val latitud: Double,
    val longitud: Double,
    val equipo: String
)