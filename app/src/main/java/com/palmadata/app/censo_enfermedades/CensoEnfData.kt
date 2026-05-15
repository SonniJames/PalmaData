package com.palmadata.app.censo_enfermedades

// ══════════════════════════════════════════════════════════════
//  DATOS HARDCODEADOS — se reemplazarán con DataLake en Etapa 3
// ══════════════════════════════════════════════════════════════

object CensoEnfData {

    // ── IDs de plantación (cat_plantacion_id en PostgreSQL) ───
    const val ID_PALMERAS_YARIMA = 123
    const val ID_VILLA_CLAUDIA   = 456
    const val ID_CUCU            = 789

    // ── Sectores por plantación ───────────────────────────────
    val sectoresPorPlantacion: Map<Int, List<String>> = mapOf(
        ID_PALMERAS_YARIMA to listOf("Sector 1", "Sector 2", "Sector 3"),
        ID_VILLA_CLAUDIA   to listOf("Sector 4", "Sector 5", "Sector 6"),
        ID_CUCU            to listOf("Sector 7", "Sector 8", "Sector 9")
    )

    // ── Lotes por sector ──────────────────────────────────────
    val lotesPorSector: Map<String, List<String>> = mapOf(
        "Sector 1" to listOf("L101-A", "L101-B", "L101-C"),
        "Sector 2" to listOf("L102-A", "L102-B", "L102-C"),
        "Sector 3" to listOf("L103-A", "L103-B", "L103-C"),
        "Sector 4" to listOf("L104-A", "L104-B", "L104-C"),
        "Sector 5" to listOf("L105-A", "L105-B", "L105-C"),
        "Sector 6" to listOf("L106-A", "L106-B", "L106-C"),
        "Sector 7" to listOf("L107-A", "L107-B", "L107-C"),
        "Sector 8" to listOf("L108-A", "L108-B", "L108-C"),
        "Sector 9" to listOf("L109-A", "L109-B", "L109-C")
    )

    // ── Enfermedades por plantación ───────────────────────────
    data class Enfermedad(val id: Int, val nombre: String)

    val enfermedadesPorPlantacion: Map<Int, List<Enfermedad>> = mapOf(
        ID_PALMERAS_YARIMA to listOf(
            Enfermedad(1, "Enfermedad_1"),
            Enfermedad(2, "Enfermedad_2"),
            Enfermedad(3, "Enfermedad_3")
        ),
        ID_VILLA_CLAUDIA to listOf(
            Enfermedad(4, "Enfermedad_4"),
            Enfermedad(5, "Enfermedad_5"),
            Enfermedad(6, "Enfermedad_6")
        ),
        ID_CUCU to listOf(
            Enfermedad(7, "Enfermedad_7"),
            Enfermedad(8, "Enfermedad_8"),
            Enfermedad(9, "Enfermedad_9")
        )
    )

    // ── Eventos por enfermedad ────────────────────────────────
    data class Evento(val id: Int, val nombre: String)

    val eventosPorEnfermedad: Map<Int, List<Evento>> = mapOf(
        1 to listOf(Evento(101, "Evento_1")),
        2 to listOf(Evento(102, "Evento_2")),
        3 to listOf(Evento(103, "Evento_3")),
        4 to listOf(Evento(104, "Evento_4")),
        5 to listOf(Evento(105, "Evento_5")),
        6 to listOf(Evento(106, "Evento_6")),
        7 to listOf(Evento(107, "Evento_7")),
        8 to listOf(Evento(108, "Evento_8")),
        9 to listOf(Evento(109, "Evento_9"))
    )

    // ── Helper: obtener cat_plantacion_id desde nombre ────────
    fun getPlantacionId(nombrePlantacion: String): Int {
        return when {
            nombrePlantacion.contains("Yarima", ignoreCase = true) -> ID_PALMERAS_YARIMA
            nombrePlantacion.contains("Claudia", ignoreCase = true) -> ID_VILLA_CLAUDIA
            nombrePlantacion.contains("CUCU", ignoreCase = true)   -> ID_CUCU
            else -> 0
        }
    }
}