package com.palmadata.app.censo_enfermedades

/**
 * Modelo del registro de censo de enfermedades.
 * Mapea exactamente la tabla san_enf_lectura en PostgreSQL.
 */
data class CensoEnfRegistro(
    // Datos ingresados por el usuario
    val censo: Long,
    val linea: Int,
    val palma: Int,
    val observaciones: String,

    // Selecciones del usuario
    val catPlantacionId: Int,
    val sanEnfermedadesId: Int,
    val sanEventoEnfId: Int,
    val evaluador: Int,          // código del trabajador

    // Tomados del celular automáticamente
    val fecha: String,           // "2025-05-12 00:00:00"
    val hora: String,            // "11:45:18"
    val actualizacion: String,   // igual que fecha
    val latitud: Double,
    val longitud: Double,

    // Generado por la app
    val id: String,              // ej: "PDY_20250512_114518_101"

    // Valores fijos
    val usuario: Int = 2,
    val sanErrorRegistroId: Int = 0,
    val epi: Int = 0,
    val eventoInicial: Int = 0,
    val ordenLabor: Int = 0,
    val ordenSupervision: Int = 0,

    val equipo: String,

    // Siempre null
    val sintomaHoja: String? = null,
    val sintomaPalma: String? = null,
    val lote: String? = null,
    val loteAlias: String? = null,
    val fechaTratamiento: String? = null,
    val sanEnfTratamientoId: Int = 0,
    val eliminacion: String? = null,

)