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
    val fertilizantes: String  // JSON array: [{"id":3,"nombre":"Grado palmero","rondas":5,"dosis":1.15}, ...]
)