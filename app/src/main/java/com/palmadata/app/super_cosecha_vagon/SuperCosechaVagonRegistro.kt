package com.palmadata.app.super_cosecha_vagon

/**
 * Registro de supervisión de cosecha vagón.
 * Mapea la tabla plantacion.supercosechavagon.
 *
 * Campos que NO viajan desde la app (los pone PostgreSQL):
 *   id, usuario, actualizacion, geom  → función + trigger
 *   cortador, recolector, qr          → siempre NULL
 */
data class SuperCosechaVagonRegistro(
    val idUnico: String,
    val fecha: String,
    val hora: String,
    val supervisor: Int,            // trabajador de la barra de MainActivity
    val trabajador: Int?,           // pantalla 3 — NULL si se dejó en blanco
    val catLoteId: Long,            // pantalla 2
    val catPlantacionId: Long,      // pantalla 0
    val racimosMuestra: Int,        // R. muestra (teclado numérico)
    val racimosVerde: Int,          // R. verdes
    val racimosSobremaduro: Int,    // R. sobremaduro
    val racimosPodridos: Int,       // R. podridos
    val pedunculoLargo: Int,        // Pendun. largo
    val racimosMalformados: Int,    // R. mal formados
    val racimosEnfermos: Int,       // R. enfermos
    val racimosEupalamides: Int,    // R. eupalamides
    val observaciones: String,      // pantalla 5
    val latitud: Double,
    val longitud: Double
)