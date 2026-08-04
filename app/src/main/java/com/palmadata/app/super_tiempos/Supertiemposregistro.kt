package com.palmadata.app.super_tiempos

/**
 * Un evento observado por la supervisora mientras acompaña a un cortador.
 *
 * Cada fila es un ciclo INICIO → FIN completo. Los eventos son mutuamente
 * excluyentes: nunca hay dos abiertos a la vez, porque un trabajador no puede
 * estar cortando y descansando al mismo tiempo. Eso hace que los huecos entre
 * un fin y el siguiente inicio sean interpretables (desplazamiento entre
 * palmas), sin que la supervisora tenga que registrarlos.
 *
 * Los campos que no aplican a un tipo de evento van en null, no en cero:
 * racimos = 0 significa "revisó la palma y no cortó nada", que es un dato real
 * y distinto de "este evento no es de corte".
 */
data class SuperTiemposRegistro(
    val idUnico: String,

    // ── Sesión ───────────────────────────────────────────────────────────────
    // Agrupa todos los eventos de una supervisora siguiendo a UN cortador en un
    // día. Sin esto no se pueden calcular denominadores por jornada observada.
    val sesionId: String,
    val secuencia: Int,          // orden dentro de la sesión, 1..n

    // ── Quién ────────────────────────────────────────────────────────────────
    val supervisor: Int,         // el de la pantalla de módulos (ella)
    val trabajador: Int,         // el cortador observado

    // ── Qué ──────────────────────────────────────────────────────────────────
    val evento: String,          // 'corte' | 'parada' | 'sin_visual'
    val tipoParada: String?,     // código del catálogo, solo si evento = 'parada'
    val racimos: Int?,           // solo si evento = 'corte'

    // ── Cuándo ───────────────────────────────────────────────────────────────
    val fecha: String,           // yyyy-MM-dd
    val horaInicio: String,      // HH:mm:ss  (reloj del equipo)
    val horaFin: String,
    val duracionSegundos: Int,   // redundante a propósito: evita recalcular en
    // cada consulta y sobrevive a cambios de huso

    /**
     * Diferencia en segundos entre el reloj del equipo y la hora del último fix
     * GPS, medida al iniciar el evento.
     *
     * Las marcas de la supervisora son toques en pantalla, así que la única
     * hora disponible en ese instante es System.currentTimeMillis(). Los tracks
     * del cortador, en cambio, llevan location.time, que en un fix de GPS viene
     * de los satélites. Si el reloj de su equipo está corrido, todo el cruce
     * temporal se desplaza y el modelo aprendería el desfase en vez del
     * comportamiento. Guardarlo permite corregir sin tocar el dato original.
     *
     * null = no había un fix reciente al marcar.
     */
    val desfaseGpsSegundos: Double?,

    // ── Dónde ────────────────────────────────────────────────────────────────
    // Dos coordenadas: la distancia entre el fin de un corte y el inicio del
    // siguiente es el desplazamiento real entre palmas.
    val latitudInicio: Double?,
    val longitudInicio: Double?,
    val latitudFin: Double?,
    val longitudFin: Double?,
    // El lote NO viaja desde la app: se deriva en PostgreSQL con ST_Contains
    // sobre la coordenada de inicio. La geometría del celular viene simplificada
    // y un valor congelado en el dispositivo no se podría recalcular si se
    // corrige un polígono.

    // ── Trazabilidad ─────────────────────────────────────────────────────────
    val observaciones: String,
    val equipo: String,
    val idEquipo: String
)


/**
 * Tipo de parada, tal como viene del catálogo del servidor
 * (plantacion.super_tiempos_tipo).
 *
 * La CLASE (productivo / auxiliar / improductivo) se descarga pero la app no la
 * usa al capturar: vive en el servidor para poder reclasificar sin reprocesar
 * eventos ya guardados. Si mañana operaciones decide que "Espera" pasa de
 * auxiliar a improductivo, se cambia una fila y todas las vistas se recalculan.
 */
data class SuperTiemposTipo(
    val codigo: String,       // lo que se guarda en el registro
    val descripcion: String,  // detalle, usado por el módulo web
    val clase: String,        // productivo | auxiliar | improductivo | excluir
    val orden: Int            // orden de aparición: los más frecuentes primero
) {
    override fun toString(): String = codigo   // lo que muestra el Spinner
}


object TiposEvento {
    const val CORTE  = "corte"
    const val PARADA = "parada"

    /**
     * La supervisora dejó de ver al trabajador. No es una parada de él: es un
     * hueco en la observación.
     *
     * Sin este estado, ese tiempo se cerraría como desplazamiento entre palmas
     * —la categoría que se deduce de los huecos— y entrarían al entrenamiento
     * etiquetas inventadas. Se registra para poder EXCLUIRLO.
     */
    const val SIN_VISUAL = "sin_visual"
}


/**
 * Semilla del catálogo local.
 *
 * El catálogo real vive en el servidor y se descarga con los demás maestros.
 * Pero un equipo recién instalado que todavía no ha sincronizado tendría el
 * desplegable vacío y el módulo sería inusable en campo, justo el día que más
 * se necesita. Por eso la tabla local se crea ya sembrada con estos valores y
 * la sincronización simplemente los reemplaza.
 *
 * Deben coincidir con el INSERT inicial de super_tiempos_postgres.sql.
 */
object TiposParadaDefecto {
    val LISTA = listOf(
        SuperTiemposTipo("Hidratación",               "Toma agua",                      "improductivo", 1),
        SuperTiemposTipo("Socialización",             "Conversación no laboral",        "improductivo", 2),
        SuperTiemposTipo("Descanso",                  "Sentado, sombra",                "improductivo", 3),
        SuperTiemposTipo("Mantenimiento herramienta", "Afila o ajusta el malayo",       "auxiliar",     4),
        SuperTiemposTipo("Espera",                    "Vehículo, insumos, instrucción", "auxiliar",     5),
        SuperTiemposTipo("Necesidad fisiológica",     "No atribuible al trabajador",    "auxiliar",     6),
        SuperTiemposTipo("Alimentación",              "Refrigerio o almuerzo",          "improductivo", 7)
    )
}