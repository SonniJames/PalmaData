package com.palmadata.app.data.model

data class UmaData(
    val nutUmaPolId: Int,
    val nutUmaId: Int,
    val codigo: String,
    val palmas: Int,
    val catPlantacionId: Int,
    val estado: Int,
    val simbolo: String,
    val geojson: String,
    val dosis: String
)