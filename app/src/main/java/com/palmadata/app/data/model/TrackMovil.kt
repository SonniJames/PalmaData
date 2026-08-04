package com.palmadata.app.data.model

/**
 * Modelo de un track de recorrido.
 * Mapea la tabla tracksmovil en PostgreSQL.
 */
data class TrackMovil(
    // GPS
    val x: Double,              // latitud
    val y: Double,              // longitud

    // Velocidad y sentido son NULOS cuando el chip no los reportó.
    // La distinción importa: null significa "no hubo dato", 0.0 significa
    // "medido y es cero" (operario detenido). Confundirlos hace imposible
    // separar una parada real de una ausencia de medición, que es justo lo
    // que el estudio de tiempos necesita distinguir.
    val velocidad: Double?,     // m/s desde location.speed, null si no hubo dato
    val precision: Double,      // metros desde location.accuracy
    val sentido: Double?,       // grados desde location.bearing, null si no hubo dato

    // Cuánto confiar en la velocidad medida (m/s). Sin este dato, un valor de
    // 0,3 m/s puede ser movimiento real o ruido del chip, y no hay forma de
    // saberlo al analizar. Requiere API 26+; null si el equipo no lo expone.
    val precisionVelocidad: Double? = null,

    // Calidad de señal en el momento del fix: termómetro objetivo del
    // desempeño bajo dosel de palma y criterio duro para descartar fixes
    // malos en el análisis.
    val satelites: Int = 0,     // satélites efectivamente usados en el cálculo
    val satVisibles: Int = 0,   // satélites detectados por el receptor

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
    val idEquipo: String = "",  // id manual del equipo (asignado en setup)

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