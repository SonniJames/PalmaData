package com.palmadata.app.data.model

/**
 * Modelo de un track de recorrido.
 * Mapea la tabla tracksmovil en PostgreSQL.
 */
data class TrackMovil(
    // GPS
    val x: Double,              // latitud
    val y: Double,              // longitud
    val velocidad: Double,      // m/s desde location.speed
    val precision: Double,      // metros desde location.accuracy
    val sentido: Double,        // grados desde location.bearing
    val proveedor: String = "fused",

    // Tiempo
    val fecha: String,          // "2025-05-12"
    val hora: String,           // "14:23:05"

    // Sesión
    val trabajador: Int,        // código trabajador, 0 si no hay
    val plantacionId: Long,     // id plantación, 0 si no hay
    val formulario: Int = 0,    // id módulo activo, 0 si no hay

    // Identificadores
    val idunico: String,        // UUID v4 único por track
    val equipo: String,         // UUID único del celular
    val idEquipo: String = "",   // ← nuevo: id manual del equipo (asignado en setup)

    // Fijos
    val maquina: Int = 0,
    val laborMaquina: Int = 0,
    val loteId: Long = 0,
    val procesado: Long = 1,
    val sesionMaquinaria: String = "",
    // Fertilización
    val fertilizante: String = "[]",   // "[]" = sin fertilizante, "[1,2]" = ids seleccionados
    // Control local
    val sincronizado: Boolean = false  // false = pendiente de subir
)