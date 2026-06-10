package com.palmadata.app.trampas

data class TrampasRegistro(
    val id: String,
    val fecha: String,
    val hora: String,
    val lectura: Long,
    val censador: Int,
    val machos: Int,
    val hembras: Int,
    val sanTrampaId: Int,
    val sanTipoTrampa: Int,
    val catPlantacionId: Int,
    val atrayente: Int,
    val feromona: Int,
    val observaciones: String,
    val equipo: String
)