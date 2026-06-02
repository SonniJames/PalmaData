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
    val catLoteId: Long,         // cat_lote_id del lote seleccionado
    val catPalmaId: Long,        // construido: catLoteId + linea(3) + palma(2)
    val sanEnfermedadesId: Int,
    val sanEventoEnfId: Int,
    val evaluador: Int,          // aux_trabajador_id del trabajador

    // Tomados del celular automáticamente
    val fecha: String,           // "2025-05-12 00:00:00"
    val hora: String,            // "11:45:18"
    val actualizacion: String,   // fecha+hora del momento de descarga al server
    val latitud: Double,
    val longitud: Double,

    // Generado por la app
    val id: String,              // UUID v4 de 36 caracteres
    val equipo: String,          // UUID único del celular

    // Valores fijos
    val usuario: Int = 2,
    val sanErrorRegistroId: Int = 0,
    val epi: Int = 0,
    val eventoInicial: Int = 0,
    val ordenLabor: Int = 0,
    val ordenSupervision: Int = 0,
    val catPalmaIdDefault: Long = 0L,
    val sanEnfTratamientoId: Int = 0,

    // Siempre null
    val sintomaHoja: String? = null,
    val sintomaPalma: String? = null,
    val lote: String? = null,
    val loteAlias: String? = null,
    val fechaTratamiento: String? = null,
    val eliminacion: String? = null
)