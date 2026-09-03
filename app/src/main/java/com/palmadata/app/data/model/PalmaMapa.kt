package com.palmadata.app.data.model

/**
 * Una palma para dibujar en el mapa.
 *
 * Solo lo indispensable: posición para el punto, lote para filtrar, y línea y
 * palma para mostrar al tocarla. Con 300.000 registros, cada campo de más se
 * multiplica por 300.000 tanto en la descarga como en la base local.
 *
 * catPalmaId es Long y no Int a propósito: los códigos tienen 15 dígitos
 * (2040074433·048·21 = lote + línea + palma) y desbordarían un Int.
 */
data class PalmaMapa(
    val catPalmaId: Long,
    val catLoteId: Int,
    val linea: Int,
    val palma: Int,
    val lat: Double,
    val lon: Double
)