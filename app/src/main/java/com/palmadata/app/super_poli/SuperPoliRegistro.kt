package com.palmadata.app.super_poli

/**
 * Registro de supervisión de polinización.
 * Mapea la tabla plantacion.pro_ordenes_super_poli_detalle.
 *
 * Los dos flujos (LINEA-PALMA y POR LINEA) escriben en esta misma estructura:
 *   modo LP → palma con valor, cant_palmas = 0
 *   modo L  → palma = 0, cant_palmas con valor
 *
 * Campos que NO viajan desde la app:
 *   id, cat_palma_id, geom, actualizacion   → PostgreSQL (función + trigger)
 *   id_orden=0, cumple=1, polinizador_sin_orden=0,
 *   flor_dejada_aplicacion1/2/3=0            → valores fijos que pone el endpoint
 *   propolinizacionid, fecha, aplicacion1/2/3_repo → NULL
 */
data class SuperPoliRegistro(
    val idUnico: String,
    val fechaSuper: String,              // fecha del celular
    val supervisorSinOrden: Int,         // trabajador de la barra de MainActivity
    val polinizador: Int,                // pantalla 12 (obligatorio)
    val catLoteId: Long,                 // pantalla 2
    val linea: Int,                      // pantalla 4
    val palma: Int,                      // pantalla 5 en modo LP, 0 en modo L
    val cantPalmas: Int,                 // pantalla 5 en modo L, 0 en modo LP
    val florPoliniAplicacion1: Int,      // pantalla 6 — Primera
    val florPoliniAplicacion2: Int,      // pantalla 6 — Segunda
    val florPoliniAplicacion3: Int,      // pantalla 6 — Tercera
    val hojaSinMarcar: Int,              // pantalla 7
    val espataSinAbrir: Int,             // pantalla 8
    val espataAbierta: Int,              // pantalla 9
    val espataParcial: Int,              // pantalla 10
    val malaCoberturaAplicacion: Int,    // pantalla 11
    val observaciones: String,           // pantalla 13
    val latitud: Double,
    val longitud: Double
)