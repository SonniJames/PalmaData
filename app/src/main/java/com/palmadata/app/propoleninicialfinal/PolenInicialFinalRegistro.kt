package com.palmadata.app.polen

data class PolenInicialFinalRegistro(
    val fecha: String,
    val inicial: Double,
    val final: Double,
    val trabajador: Int,
    /**
     * Identificador único del registro, generado en el celular.
     *
     * Es lo que permite al servidor hacer ON CONFLICT y descartar un reenvío:
     * si el 200 se pierde por un microcorte de WiFi, el registro queda
     * pendiente y se vuelve a subir en la siguiente sincronización, pero con
     * el MISMO id_movil, así que no se duplica en Postgres.
     *
     * Va como valor por defecto para no tocar los puntos que ya construyen el
     * registro: cada instancia nueva trae su propio UUID sin pedirlo.
     *
     * No se llama `id` porque en Postgres esa columna ya existe y la genera la
     * secuencia de la tabla; en la tabla local sigue estando el `id`
     * autoincremental de SQLite, que es el que se usa para borrar el registro
     * una vez subido.
     */
    val idMovil: String = java.util.UUID.randomUUID().toString()
)