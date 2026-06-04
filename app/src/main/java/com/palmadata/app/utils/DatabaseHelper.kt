package com.palmadata.app.utils

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    companion object {
        const val DB_NAME    = "palma_data.db"
        const val DB_VERSION = 3  // incrementado por nueva tabla censo_enfermedades

        // Tablas maestro
        const val T_PLANTACIONES      = "plantaciones"
        const val T_TRABAJADORES      = "trabajadores"
        const val T_SECTORES          = "sectores"
        const val T_LOTES             = "lotes"
        const val T_ENFERMEDADES      = "enfermedades"
        const val T_EVENTOS           = "eventos"

        // Tablas de campo
        const val T_CENSO_ENF         = "censo_enfermedades"
    }

    override fun onCreate(db: SQLiteDatabase) {
        // ── Maestros ─────────────────────────────────────────────────────────
        db.execSQL("""
            CREATE TABLE $T_PLANTACIONES (
                id      INTEGER PRIMARY KEY,
                nombre  TEXT NOT NULL
            )
        """)
        db.execSQL("""
            CREATE TABLE $T_TRABAJADORES (
                id      INTEGER PRIMARY KEY,
                nombre  TEXT NOT NULL
            )
        """)
        db.execSQL("""
            CREATE TABLE $T_SECTORES (
                id            INTEGER PRIMARY KEY,
                nombre        TEXT NOT NULL,
                plantacion_id INTEGER NOT NULL
            )
        """)
        db.execSQL("""
            CREATE TABLE $T_LOTES (
                id        INTEGER PRIMARY KEY,
                nombre    TEXT NOT NULL,
                sector_id INTEGER NOT NULL
            )
        """)
        db.execSQL("""
            CREATE TABLE $T_ENFERMEDADES (
                id     INTEGER PRIMARY KEY,
                nombre TEXT NOT NULL
            )
        """)
        db.execSQL("""
            CREATE TABLE $T_EVENTOS (
                id            INTEGER PRIMARY KEY,
                codigo        TEXT NOT NULL,
                enfermedad_id INTEGER NOT NULL
            )
        """)

        // ── Campo ─────────────────────────────────────────────────────────────
        db.execSQL("""
            CREATE TABLE $T_CENSO_ENF (
                id                  TEXT PRIMARY KEY,
                censo               INTEGER NOT NULL,
                fecha               TEXT NOT NULL,
                hora                TEXT NOT NULL,
                evaluador           INTEGER NOT NULL,
                san_evento_enf_id   INTEGER NOT NULL,
                san_enfermedades_id INTEGER NOT NULL,
                observaciones       TEXT,
                linea               INTEGER NOT NULL,
                palma               INTEGER NOT NULL,
                cat_lote_id         INTEGER NOT NULL,
                cat_palma_id        INTEGER DEFAULT 0,
                cat_plantacion_id   INTEGER NOT NULL,
                latitud             REAL NOT NULL,
                longitud            REAL NOT NULL,
                equipo              TEXT NOT NULL,
                sincronizado        INTEGER DEFAULT 0
            )
        """)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        listOf(T_PLANTACIONES, T_TRABAJADORES, T_SECTORES,
            T_LOTES, T_ENFERMEDADES, T_EVENTOS, T_CENSO_ENF).forEach {
            db.execSQL("DROP TABLE IF EXISTS $it")
        }
        onCreate(db)
    }

    // ── Maestros: insertar ────────────────────────────────────────────────────

    fun reemplazarPlantaciones(lista: List<Pair<Int, String>>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete(T_PLANTACIONES, null, null)
            lista.forEach { (id, nombre) ->
                db.insert(T_PLANTACIONES, null, ContentValues().apply {
                    put("id", id); put("nombre", nombre)
                })
            }
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
    }

    fun reemplazarTrabajadores(lista: List<Pair<Int, String>>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete(T_TRABAJADORES, null, null)
            lista.forEach { (id, nombre) ->
                db.insert(T_TRABAJADORES, null, ContentValues().apply {
                    put("id", id); put("nombre", nombre)
                })
            }
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
    }

    fun reemplazarSectores(lista: List<Triple<Int, String, Int>>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete(T_SECTORES, null, null)
            lista.forEach { (id, nombre, plantacionId) ->
                db.insert(T_SECTORES, null, ContentValues().apply {
                    put("id", id); put("nombre", nombre); put("plantacion_id", plantacionId)
                })
            }
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
    }

    fun reemplazarLotes(lista: List<Triple<Int, String, Int>>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete(T_LOTES, null, null)
            lista.forEach { (id, nombre, sectorId) ->
                db.insert(T_LOTES, null, ContentValues().apply {
                    put("id", id); put("nombre", nombre); put("sector_id", sectorId)
                })
            }
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
    }

    fun reemplazarEnfermedades(lista: List<Pair<Int, String>>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete(T_ENFERMEDADES, null, null)
            lista.forEach { (id, nombre) ->
                db.insert(T_ENFERMEDADES, null, ContentValues().apply {
                    put("id", id); put("nombre", nombre)
                })
            }
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
    }

    fun reemplazarEventos(lista: List<Triple<Int, String, Int>>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete(T_EVENTOS, null, null)
            lista.forEach { (id, codigo, enfId) ->
                db.insert(T_EVENTOS, null, ContentValues().apply {
                    put("id", id); put("codigo", codigo); put("enfermedad_id", enfId)
                })
            }
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
    }

    // ── Campo: censo enfermedades ─────────────────────────────────────────────

    fun guardarCensoEnf(r: com.palmadata.app.censo_enfermedades.CensoEnfRegistro) {
        writableDatabase.insert(T_CENSO_ENF, null, ContentValues().apply {
            put("id",                  r.id)
            put("censo",               r.censo)
            put("fecha",               r.fecha)
            put("hora",                r.hora)
            put("evaluador",           r.evaluador)
            put("san_evento_enf_id",   r.sanEventoEnfId)
            put("san_enfermedades_id", r.sanEnfermedadesId)
            put("observaciones",       r.observaciones)
            put("linea",               r.linea)
            put("palma",               r.palma)
            put("cat_lote_id",         r.catLoteId)
            put("cat_palma_id",        r.catPalmaId)
            put("cat_plantacion_id",   r.catPlantacionId)
            put("latitud",             r.latitud)
            put("longitud",            r.longitud)
            put("equipo",              r.equipo)
            put("sincronizado",        0)
        })
    }

    fun getCensoEnfPendientes(): List<Map<String, Any>> {
        val result = mutableListOf<Map<String, Any>>()
        val cursor = readableDatabase.query(
            T_CENSO_ENF, null, "sincronizado = 0",
            null, null, null, "fecha ASC"
        )
        cursor.use {
            while (it.moveToNext()) {
                val map = mutableMapOf<String, Any>()
                for (i in 0 until it.columnCount) {
                    map[it.getColumnName(i)] = when (it.getType(i)) {
                        android.database.Cursor.FIELD_TYPE_INTEGER -> it.getLong(i)
                        android.database.Cursor.FIELD_TYPE_FLOAT   -> it.getDouble(i)
                        else -> it.getString(i) ?: ""
                    }
                }
                result.add(map)
            }
        }
        return result
    }

    fun eliminarCensoEnf(id: String) {
        writableDatabase.delete(T_CENSO_ENF, "id = ?", arrayOf(id))
    }

    fun contarCensoEnfPendientes(): Int {
        val cursor = readableDatabase.rawQuery(
            "SELECT COUNT(*) FROM $T_CENSO_ENF WHERE sincronizado = 0", null
        )
        cursor.use { it.moveToFirst(); return it.getInt(0) }
    }

    // ── Maestros: consultar ───────────────────────────────────────────────────

    fun getPlantaciones(): List<Pair<Int, String>> {
        val result = mutableListOf<Pair<Int, String>>()
        val cursor = readableDatabase.query(T_PLANTACIONES, null, null, null, null, null, "nombre")
        cursor.use { while (it.moveToNext()) result.add(Pair(it.getInt(0), it.getString(1))) }
        return result
    }

    fun getTrabajadores(): List<Pair<Int, String>> {
        val result = mutableListOf<Pair<Int, String>>()
        val cursor = readableDatabase.query(T_TRABAJADORES, null, null, null, null, null, "nombre")
        cursor.use { while (it.moveToNext()) result.add(Pair(it.getInt(0), it.getString(1))) }
        return result
    }

    fun getSectoresPorPlantacion(plantacionId: Int): List<Pair<Int, String>> {
        val result = mutableListOf<Pair<Int, String>>()
        val cursor = readableDatabase.query(
            T_SECTORES, null, "plantacion_id = ?", arrayOf(plantacionId.toString()),
            null, null, "nombre"
        )
        cursor.use { while (it.moveToNext()) result.add(Pair(it.getInt(0), it.getString(1))) }
        return result
    }

    fun getLotesPorSector(sectorId: Int): List<Pair<Int, String>> {
        val result = mutableListOf<Pair<Int, String>>()
        val cursor = readableDatabase.query(
            T_LOTES, null, "sector_id = ?", arrayOf(sectorId.toString()),
            null, null, "nombre"
        )
        cursor.use { while (it.moveToNext()) result.add(Pair(it.getInt(0), it.getString(1))) }
        return result
    }

    fun getEnfermedades(): List<Pair<Int, String>> {
        val result = mutableListOf<Pair<Int, String>>()
        val cursor = readableDatabase.query(T_ENFERMEDADES, null, null, null, null, null, "nombre")
        cursor.use { while (it.moveToNext()) result.add(Pair(it.getInt(0), it.getString(1))) }
        return result
    }

    fun getEventosPorEnfermedad(enfermedadId: Int): List<Pair<Int, String>> {
        val result = mutableListOf<Pair<Int, String>>()
        val cursor = readableDatabase.query(
            T_EVENTOS, null, "enfermedad_id = ?", arrayOf(enfermedadId.toString()),
            null, null, "codigo"
        )
        cursor.use { while (it.moveToNext()) result.add(Pair(it.getInt(0), it.getString(1))) }
        return result
    }

    fun tieneDatos(): Boolean {
        val cursor = readableDatabase.rawQuery("SELECT COUNT(*) FROM $T_TRABAJADORES", null)
        cursor.use { it.moveToFirst(); return it.getInt(0) > 0 }
    }
}