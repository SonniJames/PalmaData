package com.palmadata.app.data.model

/**
 * Capa de censo de enfermedades del módulo Mapas.
 *
 * Es lo mismo que Mapas · Censo de la página web: las palmas censadas en UN
 * mes, una por palma (su último registro del mes), con el color de su
 * evento. Los datos vienen del servidor (/censo_mapa_eventos y /censo_mapa),
 * que a su vez usa las funciones de la base que creó la web.
 */

/** Un evento de la lista, con su color ya resuelto (el mismo de la web). */
data class EventoCenso(
    val id: Int,
    val codigo: String,
    val enfermedad: String,
    /** Color como entero ARGB, listo para Paint. */
    val color: Int
)

/** Catálogo de eventos y el rango de meses que tienen censo. */
data class CatalogoCenso(
    val eventos: List<EventoCenso>,
    /** "AAAA-MM-DD" o null si no hay censo. */
    val desde: String?,
    val hasta: String?,
    /** Cuándo se bajó del servidor (milisegundos). */
    val guardadoMs: Long,
    /** true si se leyó del teléfono porque el servidor no respondió. */
    val sinConexion: Boolean
)

/** Una palma censada: su último registro del mes. */
data class PuntoCenso(
    val id: Long,
    /** "AAAA-MM-DD" */
    val fecha: String,
    /** Hora del registro, si la trae ("" si no). */
    val hora: String,
    val lote: String,
    val linea: Int,
    val palma: Int,
    val enfermedad: String,
    val eventoId: Int,
    val trabajador: String,
    /** null si la palma no está en cat_palma: cuenta, pero no se dibuja. */
    val lat: Double?,
    val lon: Double?,
    /** Cuántos registros tuvo la palma en el mes (se muestra el último). */
    val registros: Int
)

/** El censo de un mes. */
data class MesCenso(
    val anio: Int,
    val mes: Int,
    val puntos: List<PuntoCenso>,
    val guardadoMs: Long,
    val sinConexion: Boolean
)
