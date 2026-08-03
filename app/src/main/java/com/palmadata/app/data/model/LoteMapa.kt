package com.palmadata.app.data.model

/**
 * Lote con su polígono y las características que se muestran en la caja
 * informativa del módulo de mapas. Todo llega en una sola descarga desde
 * /lotes_mapa: no hay que cruzar geometría con atributos en la app.
 */
data class LoteMapa(
    val catLoteId: Int,
    val nombre: String,
    val siembra: Int,        // año de siembra, 0 si no está registrado
    val palmas: Int,
    val material: String,
    val geojson: String
)