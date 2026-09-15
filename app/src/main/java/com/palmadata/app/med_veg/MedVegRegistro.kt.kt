package com.palmadata.app.med_veg

/**
 * Registro de MEDIDAS VEGETATIVAS. Mapea plantacion.medidas_vegetativas.
 * Los nullable se guardan NULL cuando el teclado quedó vacío; los Double de
 * las cajas de filas se guardan 0.0 cuando la caja quedó vacía (así lo pide
 * la tabla). cat_palma_id, geom, usuario y actualizacion los pone el trigger.
 */
data class MedVegRegistro(
    val id: String,               // UUID generado en el celular (ON CONFLICT)
    val fecha: String,            // "yyyy-MM-dd HH:mm:ss"
    val hora: String,             // "HH:mm:ss"
    val catPlantacionId: Int,
    val catLoteId: Long,
    val evaluador: Int,           // aux_trabajador_id del trabajador en sesión
    val observaciones: String,    // máx. 100 (varchar(100))
    val linea: Int,
    val palma: Int,
    val latitud: Double,
    val longitud: Double,
    val nutUmaId: Int,            // nut_uma_id de la uma seleccionada
    val numFoliolos: Double?,     // pantalla 6, vacío = NULL
    val longPeciolo: Double = 0.0, val anchPeciolo: Double = 0.0,
    val profPeciolo: Double = 0.0, val longRaquis: Double = 0.0,
    val ancho1: Double = 0.0, val largo1: Double = 0.0,
    val ancho2: Double = 0.0, val largo2: Double = 0.0,
    val ancho3: Double = 0.0, val largo3: Double = 0.0,
    val ancho4: Double = 0.0, val largo4: Double = 0.0,
    val ancho5: Double = 0.0, val largo5: Double = 0.0,
    val ancho6: Double = 0.0, val largo6: Double = 0.0,
    val ancho7: Double = 0.0, val largo7: Double = 0.0,
    val ancho8: Double = 0.0, val largo8: Double = 0.0,
    val hoja: Int?,               // pantalla 17, vacío = NULL
    val numHojasVerdes: Int?,     // pantalla 18, vacío = NULL
    val nivFoliar: Double?        // pantalla 16, vacío = NULL
)
