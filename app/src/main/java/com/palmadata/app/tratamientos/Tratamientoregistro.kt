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
    val cantidad: Double? = null,     // desde sep 2026 viaja NULL: la cantidad va por producto en `producto`
    val sanEnfLecturaId: Int = 0,
    val usuario: Int = 1,
    val equipo: String,
    val lote: String? = null,
    val loteAlias: String? = null,
    // Pantallas 7.1–8.2 (sep 2026). Nullable: vacío u omitido se guarda NULL.
    val equipoAplicacionId: Int? = null,
    val areaIntervenida: Double? = null,
    /** JSON: [{"producto_id","unidad_aplicacion_id","cantidad"}, ...] o null. Columna jsonb en Postgres. */
    val producto: String? = null,
    val remision: Int? = null
)